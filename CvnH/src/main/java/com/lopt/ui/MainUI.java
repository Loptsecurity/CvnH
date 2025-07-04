package com.lopt.ui;

import com.lopt.ui.peizhi.ConfigPanel;
import lombok.Data;
import javax.swing.*;
import java.awt.*;

@Data
public class MainUI {

    private JPanel mainPanel;
    private JTabbedPane mainTabbedPane;

    private ConfigPanel configPanel;   // 配置页
    private AttackPanel attackPanel;   // 攻击页

    public MainUI() {
        // 创建主面板和标签页
        mainPanel = new JPanel(new BorderLayout());
        mainTabbedPane = new JTabbedPane();

        // 实例化各个功能面板
        configPanel = new ConfigPanel();
        attackPanel = new AttackPanel(); //

        // 将功能面板添加到标签页中
        mainTabbedPane.addTab("配置", configPanel);
        mainTabbedPane.addTab("攻击", attackPanel);

        // 将标签页设置为主面板的内容
        mainPanel.add(mainTabbedPane, BorderLayout.CENTER);
    }
}