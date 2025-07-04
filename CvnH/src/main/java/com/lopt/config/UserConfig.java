package com.lopt.config;

import com.lopt.bean.Language;
import com.lopt.bean.SearchScope;

import java.util.HashMap;
import java.util.Map;

public class UserConfig {
    // 基础配置
    public static Boolean TURN_ON = Boolean.TRUE;
    public static Boolean LISTEN_PROXY = Boolean.TRUE;
    public static Boolean LISTEN_REPETER = Boolean.TRUE;
    public static Language LANGUAGE = Language.SIMPLIFIED_CHINESE;

    // 作用域配置
    public static Boolean BLACK_OR_WHITE_CHOOSE = Boolean.TRUE; // true为黑名单
    public static Boolean INCLUDE_SUBDOMAIN = Boolean.FALSE;

    // 线程池配置
    public static Integer CORE_POOL_SIZE = 10;
    public static Integer MAX_POOL_SIZE = 100;
    public static Long KEEP_ALIVE_TIME = 60L; // 单位：秒

    public static SearchScope SEARCH_SCOPE;

    public static Map<String, Object> saveToMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("turnOn", TURN_ON);
        config.put("listenProxy", LISTEN_PROXY);
        config.put("listenRepeter", LISTEN_REPETER);
        config.put("language", LANGUAGE.name());
        config.put("blackOrWhiteChoose", BLACK_OR_WHITE_CHOOSE);
        config.put("includeSubdomain", INCLUDE_SUBDOMAIN);
        config.put("corePoolSize", CORE_POOL_SIZE);
        config.put("maxPoolSize", MAX_POOL_SIZE);
        config.put("keepAliveTime", KEEP_ALIVE_TIME);

        if (SEARCH_SCOPE != null) {
            config.put("searchScope", SEARCH_SCOPE.name());
        }
        return config;
    }

    public static void loadFromMap(Map<String, Object> config) {
        if (config == null) return;

        TURN_ON = (Boolean) config.getOrDefault("turnOn", Boolean.TRUE);
        LISTEN_PROXY = (Boolean) config.getOrDefault("listenProxy", Boolean.TRUE);
        LISTEN_REPETER = (Boolean) config.getOrDefault("listenRepeter", Boolean.TRUE);
        BLACK_OR_WHITE_CHOOSE = (Boolean) config.getOrDefault("blackOrWhiteChoose", Boolean.TRUE);
        INCLUDE_SUBDOMAIN = (Boolean) config.getOrDefault("includeSubdomain", Boolean.FALSE);

        // 注意从Map加载时，数字可能被解析为更宽的类型，需要安全转换
        CORE_POOL_SIZE = ((Number) config.getOrDefault("corePoolSize", 10)).intValue();
        MAX_POOL_SIZE = ((Number) config.getOrDefault("maxPoolSize", 100)).intValue();
        KEEP_ALIVE_TIME = ((Number) config.getOrDefault("keepAliveTime", 60)).longValue();

        if (config.containsKey("language")) {
            try {
                LANGUAGE = Language.valueOf((String) config.get("language"));
            } catch (Exception e) {
                LANGUAGE = Language.SIMPLIFIED_CHINESE;
            }
        }
        if (config.containsKey("searchScope")) {
            try {
                SEARCH_SCOPE = SearchScope.valueOf((String) config.get("searchScope"));
            } catch (Exception e) {
                SEARCH_SCOPE = null;
            }
        }
    }
}