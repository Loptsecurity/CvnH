package com.lopt.service;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.TimingData;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lopt.Main;
import com.lopt.bean.FuzzRequestItem.FuzzType;
import com.lopt.config.Config;
import com.lopt.bean.Data;
import com.lopt.bean.FuzzRequestItem;
import com.lopt.bean.FuzzRule;
import com.lopt.bean.OriginRequestItem;
import com.lopt.utils.Util;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.lopt.bean.Data.DEDUPLICATION_LOG;

public class FuzzService {

    public synchronized void preFuzz(HttpRequest request, int menuId) throws UnsupportedEncodingException, MalformedURLException {
        if (Data.FUZZING_RULES.isEmpty()) {
            return;
        }
        for (String suffix : Config.STATIC_RESOURCE) {
            if (request.pathWithoutQuery().endsWith(suffix)) {
                return;
            }
        }

        boolean hasGeneratedFuzzRequests = false;
        OriginRequestItem originRequestItem = new OriginRequestItem(Data.ORIGIN_REQUEST_TABLE_DATA.size() + 1, request.method(), new URL(request.url()).getHost(), request.pathWithoutQuery(), null, null);
        ArrayList<HttpRequest> newRequestToBeSentList = new ArrayList<>();
        originRequestItem.setOriginRequest(request);

        for (FuzzRule rule : Data.FUZZING_RULES) {
            if (!rule.isActive()) continue;

            try {
                boolean didGenerateForThisRule = false;

                if (rule.getType() == FuzzRule.RuleType.PARAMETER) {
                    String deduplicationKey;
                    if (rule.getDeduplicationStrategy() == FuzzRule.DeduplicationStrategy.HOST) {
                        deduplicationKey = "PARAM:" + request.httpService().host() + ":" + rule.getRegex();
                    } else { // 默认为 ENDPOINT
                        deduplicationKey = "PARAM:" + request.method() + ":" + request.httpService().host() + request.path() + ":" + rule.getRegex();
                    }
                    if (DEDUPLICATION_LOG.contains(deduplicationKey)) {
                        continue;
                    }

                    ArrayList<String> payloads = Data.CATEGORIZED_PAYLOADS.get(rule.getCategoryName());
                    if (payloads == null || payloads.isEmpty()) continue;
                    Pattern pattern = Pattern.compile(rule.getRegex());

                    for (ParsedHttpParameter parameter : request.parameters()) {
                        if (parameter.type() == HttpParameterType.COOKIE || parameter.type() == HttpParameterType.JSON) continue;
                        if (pattern.matcher(parameter.name()).find()) {
                            didGenerateForThisRule = true;
                            for (String payload : payloads) {
                                String finalPayload = rule.isUrlEncode() ? Util.urlEncode(payload) : payload;
                                String finalValue = rule.isAppendMode() ? parameter.value() + finalPayload : finalPayload;
                                HttpRequest newRequest = request.withParameter(HttpParameter.parameter(parameter.name(), finalValue, parameter.type()));
                                newRequestToBeSentList.add(newRequest);
                                originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(parameter.name(), payload, null, null, null, null, originRequestItem, FuzzType.PARAMETER));
                            }
                        }
                    }
                    if (request.hasParameters(HttpParameterType.JSON)) {
                        String jsonString = request.bodyToString();
                        Object jsonObject = JSON.parse(jsonString);
                        if (jsonObject != null && !(jsonObject instanceof String)) {
                            LinkedHashMap<HashMap<Integer, Object>, HashMap<String, Object>> result = new LinkedHashMap<>();
                            parseJsonParam(null, jsonObject, result);
                            for (Map.Entry<HashMap<Integer, Object>, HashMap<String, Object>> resultEntry : result.entrySet()) {
                                for (Map.Entry<Integer, Object> keyEntry : resultEntry.getKey().entrySet()) {
                                    Integer positionId = keyEntry.getKey();
                                    String jsonKey = (String) keyEntry.getValue();
                                    if (pattern.matcher(jsonKey).find()) {
                                        didGenerateForThisRule = true;
                                        for (String payload : payloads) {
                                            Object originalJson = JSON.parse(jsonString);
                                            String newJsonBody = updateJsonValue(positionId, payload, originalJson, result, rule.isAppendMode()).get("json").toString();
                                            HttpRequest newRequest = request.withBody(ByteArray.byteArray(newJsonBody.getBytes(StandardCharsets.UTF_8)));
                                            newRequestToBeSentList.add(newRequest);
                                            originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(jsonKey, payload, null, null, null, null, originRequestItem, FuzzType.PARAMETER));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(didGenerateForThisRule) {
                        hasGeneratedFuzzRequests = true;
                        DEDUPLICATION_LOG.add(deduplicationKey);
                    }
                } else if (rule.getType() == FuzzRule.RuleType.HEADER) {
                    String deduplicationKey = "HEADER:" + request.httpService().host() + ":" + rule.getRegex();
                    if (DEDUPLICATION_LOG.contains(deduplicationKey)) {
                        continue;
                    }

                    ArrayList<String> payloads = Data.CATEGORIZED_PAYLOADS.get(rule.getCategoryName());
                    if (payloads == null || payloads.isEmpty()) continue;
                    Pattern pattern = Pattern.compile(rule.getRegex());

                    boolean headerExists = false;
                    for (HttpHeader header : request.headers()) {
                        if (pattern.matcher(header.name()).find()) {
                            headerExists = true;
                            didGenerateForThisRule = true;
                            for (String payload : payloads) {
                                HttpRequest newRequest = request.withHeader(header.name(), payload);
                                newRequestToBeSentList.add(newRequest);
                                originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(header.name(), payload, null, null, null, null, originRequestItem, FuzzType.HEADER));
                            }
                        }
                    }
                    if (!headerExists && rule.isAddHeaderIfNotExists()) {
                        if (isLiteralString(rule.getRegex())) {
                            didGenerateForThisRule = true;
                            for (String payload : payloads) {
                                HttpRequest newRequest = request.withAddedHeader(rule.getRegex(), payload);
                                newRequestToBeSentList.add(newRequest);
                                originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(rule.getRegex(), payload, null, null, null, null, originRequestItem, FuzzType.HEADER));
                            }
                        }
                    }
                    if(didGenerateForThisRule){
                        hasGeneratedFuzzRequests = true;
                        DEDUPLICATION_LOG.add(deduplicationKey);
                    }
                }
            } catch (Exception e) {
                Main.LOG.logToError("[ERROR] Fuzz规则处理异常: " + e.getMessage());
            }
        }

        if (hasGeneratedFuzzRequests) {
            if (request instanceof HttpRequestToBeSent) {
                HttpRequestToBeSent r = (HttpRequestToBeSent) request;
                Data.ORIGIN_REQUEST_TABLE_DATA.put(r.messageId(), originRequestItem);
                Data.NEW_REQUEST_TO_BE_SENT_DATA.put(r.messageId(), newRequestToBeSentList);
            } else {
                Data.ORIGIN_REQUEST_TABLE_DATA.put(menuId, originRequestItem);
                Data.NEW_REQUEST_TO_BE_SENT_DATA.put(menuId, newRequestToBeSentList);
            }
        }
    }

    public synchronized void performParameterDeletionFuzz(HttpRequest request, int menuId) throws MalformedURLException {
        List<ParsedHttpParameter> parameters = request.parameters();
        if (parameters.isEmpty()) return;
        OriginRequestItem originRequestItem = new OriginRequestItem(Data.ORIGIN_REQUEST_TABLE_DATA.size() + 1, request.method(), new URL(request.url()).getHost(), request.pathWithoutQuery(), null, null);
        ArrayList<HttpRequest> newRequestToBeSentList = new ArrayList<>();
        originRequestItem.setOriginRequest(request);
        for (ParsedHttpParameter param : parameters) {
            HttpRequest newRequest = request.withRemovedParameters(param);
            newRequestToBeSentList.add(newRequest);
            FuzzRequestItem fuzzItem = new FuzzRequestItem(param.name(), "*DELETED*", null, null, null, null, originRequestItem, FuzzType.PARAMETER_DELETION);
            originRequestItem.getFuzzRequestArrayList().add(fuzzItem);
        }
        newRequestToBeSentList.add(0, request);
        Data.ORIGIN_REQUEST_TABLE_DATA.put(menuId, originRequestItem);
        Data.NEW_REQUEST_TO_BE_SENT_DATA.put(menuId, newRequestToBeSentList);
    }

    public synchronized void performHeaderFuzz(HttpRequest request, int menuId) throws MalformedURLException {
        OriginRequestItem originRequestItem = new OriginRequestItem(Data.ORIGIN_REQUEST_TABLE_DATA.size() + 1, request.method(), new URL(request.url()).getHost(), request.pathWithoutQuery(), null, null);
        ArrayList<HttpRequest> newRequestToBeSentList = new ArrayList<>();
        originRequestItem.setOriginRequest(request);
        boolean hasGeneratedFuzzRequests = false;
        for (FuzzRule rule : Data.FUZZING_RULES) {
            if (!rule.isActive() || rule.getType() != FuzzRule.RuleType.HEADER) continue;
            try {
                Pattern pattern = Pattern.compile(rule.getRegex());
                ArrayList<String> payloads = Data.CATEGORIZED_PAYLOADS.get(rule.getCategoryName());
                if (payloads == null || payloads.isEmpty()) continue;
                boolean headerExists = false;
                for (HttpHeader header : request.headers()) {
                    if (pattern.matcher(header.name()).find()) {
                        headerExists = true;
                        hasGeneratedFuzzRequests = true;
                        for (String payload : payloads) {
                            HttpRequest newRequest = request.withHeader(header.name(), payload);
                            newRequestToBeSentList.add(newRequest);
                            originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(header.name(), payload, null, null, null, null, originRequestItem, FuzzType.HEADER));
                        }
                    }
                }
                if (!headerExists && rule.isAddHeaderIfNotExists()) {
                    if (isLiteralString(rule.getRegex())) {
                        hasGeneratedFuzzRequests = true;
                        for (String payload : payloads) {
                            HttpRequest newRequest = request.withAddedHeader(rule.getRegex(), payload);
                            newRequestToBeSentList.add(newRequest);
                            originRequestItem.getFuzzRequestArrayList().add(new FuzzRequestItem(rule.getRegex(), payload, null, null, null, null, originRequestItem, FuzzType.HEADER));
                        }
                    }
                }
            } catch (Exception e) {
                Main.LOG.logToError("[ERROR] 主动Header Fuzz规则处理异常: " + e.getMessage());
            }
        }
        if (hasGeneratedFuzzRequests) {
            newRequestToBeSentList.add(0, request);
            Data.ORIGIN_REQUEST_TABLE_DATA.put(menuId, originRequestItem);
            Data.NEW_REQUEST_TO_BE_SENT_DATA.put(menuId, newRequestToBeSentList);
        }
    }

    public void startFuzz(int msgId) {
        if (!Data.NEW_REQUEST_TO_BE_SENT_DATA.containsKey(msgId)) {
            return;
        }
        ArrayList<HttpRequest> requestToBeSentList = Data.NEW_REQUEST_TO_BE_SENT_DATA.get(msgId);
        if (requestToBeSentList == null) return;
        OriginRequestItem originRequestItem = Data.ORIGIN_REQUEST_TABLE_DATA.get(msgId);
        if (originRequestItem == null) return;
        ArrayList<FuzzRequestItem> fuzzRequestItemArrayList = originRequestItem.getFuzzRequestArrayList();
        int i = 0;
        if (msgId < 0) {
            if (!requestToBeSentList.isEmpty()) {
                HttpRequest originRequest = requestToBeSentList.remove(0);
                HttpRequestResponse httpRequestResponse = Main.API.http().sendRequest(originRequest);
                originRequestItem.setOriginResponse(httpRequestResponse.response());
                originRequestItem.setResponseLength(String.valueOf(httpRequestResponse.response().toString().length()));
                originRequestItem.setResponseCode(String.valueOf(httpRequestResponse.response().statusCode()));
            }
        }
        for (HttpRequest request : requestToBeSentList) {
            if (i >= fuzzRequestItemArrayList.size()) break;
            HttpRequestResponse httpRequestResponse = Main.API.http().sendRequest(request);
            FuzzRequestItem fuzzRequestItem = fuzzRequestItemArrayList.get(i);
            fuzzRequestItem.setFuzzRequestResponse(httpRequestResponse);
            String responseLength = String.valueOf(httpRequestResponse.response().toString().length());
            if (fuzzRequestItem.getOriginRequestItem().getResponseLength() != null) {
                int lengthChange = httpRequestResponse.response().toString().length() - Integer.parseInt(fuzzRequestItem.getOriginRequestItem().getResponseLength());
                fuzzRequestItem.setResponseLengthChange((lengthChange > 0 ? "+" + lengthChange : String.valueOf(lengthChange)));
            }
            fuzzRequestItem.setResponseLength(responseLength);
            fuzzRequestItem.setResponseCode(String.valueOf(httpRequestResponse.response().statusCode()));
            Optional<TimingData> timingOpt = httpRequestResponse.timingData();
            if (timingOpt.isPresent()) {
                long timeInMillis = timingOpt.get().timeBetweenRequestSentAndEndOfResponse().toMillis();
                fuzzRequestItem.setResponseTime(timeInMillis + "ms");
            } else {
                Main.LOG.logToError("[ERROR] 未获取到 timingData");
            }
            i++;
        }
        Data.NEW_REQUEST_TO_BE_SENT_DATA.remove(msgId);
    }

    private boolean isLiteralString(String s) {
        return s != null && !s.matches(".*[\\*\\+\\?\\^\\$\\.\\|\\(\\)\\[\\]\\{\\}].*");
    }

    public static void parseJsonParam(Object jsonKey, Object jsonObj, LinkedHashMap<HashMap<Integer, Object>, HashMap<String, Object>> result) {
        if (jsonKey == null) jsonKey = "root";
        if (jsonObj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) jsonObj;
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                parseJsonParam(entry.getKey(), entry.getValue(), result);
            }
        } else if (jsonObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonObj;
            for (int i = 0; i < jsonArray.size(); i++) {
                String arrayKey = jsonKey + "[" + i + "]";
                parseJsonParam(arrayKey, jsonArray.get(i), result);
            }
        } else {
            HashMap<String, Object> valueMap = new HashMap<>();
            HashMap<Integer, Object> keyMap = new HashMap<>();
            String typeName = jsonObj != null ? jsonObj.getClass().getSimpleName() : "null";
            valueMap.put(typeName, jsonObj);
            keyMap.put(result.size() + 1, jsonKey);
            result.put(keyMap, valueMap);
        }
    }

    public static HashMap<String, Object> updateJsonValue(int i, String payload, Object jsonObj, LinkedHashMap<HashMap<Integer, Object>, HashMap<String, Object>> result, boolean isAppendMode) {
        HashMap<String, Object> newJsonStringMap = new HashMap<>();
        newJsonStringMap.put("isModified", false);
        newJsonStringMap.put("json", jsonObj);
        if (jsonObj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) jsonObj;
            for (Map.Entry<String, Object> entry : ((JSONObject) jsonObj).entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof JSONObject) && !(value instanceof JSONArray)) {
                    Object resultKey = null;
                    Object resultValue = null;
                    for (Map.Entry<HashMap<Integer, Object>, HashMap<String, Object>> resultEntry : result.entrySet()) {
                        if (resultEntry.getKey().containsKey(i)) {
                            resultKey = resultEntry.getKey().get(i);
                            resultValue = resultEntry.getValue().values().iterator().next();
                            break;
                        }
                    }
                    if (Objects.equals(resultKey, entry.getKey()) && Objects.equals(resultValue, value)) {
                        Object newValue = Util.isNumber(payload);
                        if(newValue instanceof String) newValue = Util.isBoolean((String)newValue);
                        if(isAppendMode && !payload.isEmpty() && value != null){
                            jsonObject.put(entry.getKey(), value.toString() + newValue.toString());
                        } else {
                            jsonObject.put(entry.getKey(), newValue);
                        }
                        newJsonStringMap.put("isModified", true);
                        newJsonStringMap.put("json", jsonObject);
                        return newJsonStringMap;
                    }
                } else {
                    HashMap<String, Object> tmpResult = updateJsonValue(i, payload, value, result, isAppendMode);
                    if ((boolean) tmpResult.get("isModified")) {
                        jsonObject.put(entry.getKey(), tmpResult.get("json"));
                        newJsonStringMap.put("json", jsonObject);
                        newJsonStringMap.put("isModified", true);
                        return newJsonStringMap;
                    }
                }
            }
        } else if (jsonObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonObj;
            for (int index = 0; index < jsonArray.size(); index++) {
                HashMap<String, Object> tmpResult = updateJsonValue(i, payload, jsonArray.get(index), result, isAppendMode);
                if ((boolean) tmpResult.get("isModified")) {
                    jsonArray.set(index, tmpResult.get("json"));
                    newJsonStringMap.put("json", jsonArray);
                    newJsonStringMap.put("isModified", true);
                    return newJsonStringMap;
                }
            }
        }
        return newJsonStringMap;
    }
}