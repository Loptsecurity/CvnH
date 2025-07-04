package com.lopt.bean;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class FuzzRule {

    public enum RuleType {
        PARAMETER,
        HEADER
    }

    // 定义两种去重策略
    public enum DeduplicationStrategy {
        ENDPOINT, // 按完整端点去重 (方法+主机+路径+规则)
        HOST      // 只按主机去重 (主机+规则)
    }

    private boolean isActive;
    private RuleType type;
    private String categoryName;
    private String regex;

    // --- 参数规则配置 ---
    private boolean appendMode;
    private boolean urlEncode;
    private DeduplicationStrategy deduplicationStrategy; // 【新增】去重策略字段

    // --- Header规则配置 ---
    private boolean addHeaderIfNotExists;

    private FuzzRule(boolean isActive, RuleType type, String categoryName, String regex) {
        this.isActive = isActive;
        this.type = type;
        this.categoryName = categoryName;
        this.regex = regex;
        // 默认值
        this.appendMode = false;
        this.urlEncode = false;
        this.addHeaderIfNotExists = false;
        this.deduplicationStrategy = DeduplicationStrategy.ENDPOINT; // 参数规则默认为最严格的端点去重
    }

    public static FuzzRule createRule(boolean isActive, RuleType type, String categoryName, String regex) {
        return new FuzzRule(isActive, type, categoryName, regex);
    }
}