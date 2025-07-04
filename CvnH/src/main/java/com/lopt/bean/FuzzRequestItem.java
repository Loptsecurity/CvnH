package com.lopt.bean;
import burp.api.montoya.http.message.HttpRequestResponse;
import lombok.Data;

@Data
public class FuzzRequestItem {

    // 用于区分Fuzz类型的枚举
    public enum FuzzType {
        PARAMETER,          // 参数规则Fuzz
        HEADER,             // Header规则Fuzz
        PARAMETER_DELETION  // 删除参数Fuzz
    }

    private String param;
    private String payload;
    private String responseLength;
    private String responseLengthChange;
    private String responseCode;
    private String responseTime;
    private HttpRequestResponse fuzzRequestResponse;
    private OriginRequestItem originRequestItem;
    private FuzzType fuzzType; // 记录Fuzz类型的字段

    public FuzzRequestItem(String param, String payload, String responseLength, String responseLengthChange, String responseCode, String responseTime, OriginRequestItem originRequestItem, FuzzType fuzzType) {
        this.param = param;
        this.payload = payload;
        this.responseLength = responseLength;
        this.responseLengthChange = responseLengthChange;
        this.responseCode = responseCode;
        this.responseTime = responseTime;
        this.originRequestItem = originRequestItem;
        this.fuzzType = fuzzType; // 【新增】在构造时传入类型
    }
}