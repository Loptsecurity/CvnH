package com.lopt.utils;
import com.lopt.Main;
import com.lopt.bean.Data;
import com.lopt.bean.FuzzRule;
import com.lopt.config.UserConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class YamlUtil {

    private static final String CONFIG_DIR_NAME = "CvnH-Config";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String CONFIG_PATH_KEY = "CvnH.configPath";
    private static Path configFilePath;

    private static class CustomRepresenter extends Representer {
        public CustomRepresenter(DumperOptions options) {
            super(options);
            this.multiRepresenters.put(FuzzRule.class, data -> {
                FuzzRule rule = (FuzzRule) data;
                Tag tag = rule.getType() == FuzzRule.RuleType.PARAMETER ? new Tag("!param_rule") : new Tag("!header_rule");
                Node node = representJavaBean(getProperties(FuzzRule.class), rule);
                node.setTag(tag);
                return node;
            });
        }
    }

    private static class CustomConstructor extends Constructor {
        public CustomConstructor(Class<?> theRoot, LoaderOptions loadingConfig) {
            super(theRoot, loadingConfig);
            this.addTypeDescription(new TypeDescription(FuzzRule.class, new Tag("!param_rule")));
            this.addTypeDescription(new TypeDescription(FuzzRule.class, new Tag("!header_rule")));
        }
    }

    private static Yaml getCustomYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        LoaderOptions loaderOptions = new LoaderOptions();
        Representer representer = new CustomRepresenter(dumperOptions);
        Constructor constructor = new CustomConstructor(LinkedHashMap.class, loaderOptions);
        return new Yaml(constructor, representer, dumperOptions, loaderOptions);
    }

    public static boolean checkConfigDir() {
        String customPathStr = Main.API.persistence().preferences().getString(CONFIG_PATH_KEY);
        if (customPathStr != null && !customPathStr.isEmpty()) {
            try {
                Path customPath = Paths.get(customPathStr);
                if(Files.isDirectory(customPath)){
                    configFilePath = customPath.resolve(CONFIG_FILE_NAME);
                } else {
                    configFilePath = customPath;
                }
                Main.LOG.logToOutput("使用自定义配置文件路径: " + configFilePath.toString());
                return true;
            } catch (InvalidPathException e) {
                Main.LOG.logToError("自定义配置文件路径无效: " + customPathStr);
            }
        }

        try {
            Main.LOG.logToOutput("未使用自定义路径，将使用默认路径。");
            URI burpJarUri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path burpJarPath = Paths.get(burpJarUri);
            Path burpDir = burpJarPath.getParent();
            Path configDir = burpDir.resolve(CONFIG_DIR_NAME);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            configFilePath = configDir.resolve(CONFIG_FILE_NAME);
            return true;
        } catch (Exception e) {
            Main.LOG.logToError("创建插件默认配置目录失败: " + e.getMessage());
            return false;
        }
    }

    public static String getCurrentConfigPath() {
        return configFilePath != null ? configFilePath.toString() : "路径未初始化";
    }

    @SuppressWarnings("unchecked")
    public static void loadYamlConfig() {
        if (configFilePath == null || !Files.exists(configFilePath)) {
            Main.LOG.logToOutput("配置文件不存在，跳过加载。");
            return;
        }
        Yaml yaml = getCustomYaml();
        try (InputStream is = Files.newInputStream(configFilePath)) {
            Map<String, Object> config = yaml.load(is);
            if (config == null) {
                Main.LOG.logToOutput("配置文件为空。");
                return;
            }
            UserConfig.loadFromMap(config);
            Data.loadFromMap(config);
            Main.LOG.logToOutput("CvnH 配置文件加载成功。");
        } catch (Exception e) {
            Main.LOG.logToError("加载配置文件失败: " + e.getMessage());
        }
    }

    public static void exportToYaml() {
        if (configFilePath == null) {
            return;
        }
        Map<String, Object> config = new HashMap<>();
        config.putAll(UserConfig.saveToMap());
        config.putAll(Data.saveToMap());
        Yaml yaml = getCustomYaml();
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configFilePath), StandardCharsets.UTF_8)) {
            yaml.dump(config, writer);
        } catch (Exception e) {
            Main.LOG.logToError("导出配置文件失败: " + e.getMessage());
        }
    }
}