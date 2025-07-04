package com.lopt.bean;
import burp.api.montoya.http.message.requests.HttpRequest;
import java.util.*;
public class Data {
    // 运行时数据
    public static LinkedHashMap<Integer, OriginRequestItem> ORIGIN_REQUEST_TABLE_DATA = new LinkedHashMap<>();
    public static LinkedHashMap<Integer, ArrayList<HttpRequest>> NEW_REQUEST_TO_BE_SENT_DATA = new LinkedHashMap<>();
    public static Set<String> DEDUPLICATION_LOG = new HashSet<>();

    // 持久化配置数据
    public static ArrayList<String> DOMAIN_LIST = new ArrayList<>();
    public static LinkedHashMap<String, ArrayList<String>> CATEGORIZED_PAYLOADS = new LinkedHashMap<>();
    public static ArrayList<FuzzRule> FUZZING_RULES = new ArrayList<>();

    public static Map<String, Object> saveToMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("domainList", DOMAIN_LIST);
        dataMap.put("categorizedPayloads", CATEGORIZED_PAYLOADS);
        dataMap.put("fuzzingRules", FUZZING_RULES);
        return dataMap;
    }

    @SuppressWarnings("unchecked")
    public static void loadFromMap(Map<String, Object> config) {
        if (config == null) return;
        if (config.containsKey("domainList")) {
            Object dl = config.get("domainList");
            if (dl instanceof ArrayList) {
                DOMAIN_LIST = (ArrayList<String>) dl;
            }
        }
        if (config.containsKey("categorizedPayloads")) {
            Object cp = config.get("categorizedPayloads");
            if (cp instanceof LinkedHashMap) {
                CATEGORIZED_PAYLOADS = (LinkedHashMap<String, ArrayList<String>>) cp;
            }
        }
        if (config.containsKey("fuzzingRules")) {
            Object fr = config.get("fuzzingRules");
            if (fr instanceof ArrayList) {
                FUZZING_RULES = (ArrayList<FuzzRule>) fr;
            }
        }
    }
}