package com.lopt;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.lopt.handler.FuzzHandler;
import com.lopt.menu.FuzzMenu;
import com.lopt.ui.MainUI;
import com.lopt.utils.YamlUtil;

public class Main implements BurpExtension {
    public static MontoyaApi API;
    public static Logging LOG;
    public static MainUI MainUI;
    @Override
    public void initialize(MontoyaApi api) {
        // 初始化api与log
        API = api;
        LOG = api.logging();

        // banner info
        API.extension().setName("CvnH");
        LOG.logToOutput("CvnH v1.O");
        LOG.logToOutput("Author: Lopt");

        // 加载配置文件
        if (YamlUtil.checkConfigDir()) {
            YamlUtil.loadYamlConfig();
        }
        FuzzHandler.rebuildExecutor(); // 根据加载的配置初始化/重建线程池

        // 初始化ui
        MainUI = new MainUI();

        API.userInterface().registerSuiteTab("CvnH", MainUI.getMainPanel());
        API.http().registerHttpHandler(new FuzzHandler());
        API.userInterface().registerContextMenuItemsProvider(new FuzzMenu());
    }
}
