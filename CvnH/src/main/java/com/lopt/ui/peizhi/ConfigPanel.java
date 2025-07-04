package com.lopt.ui.peizhi;
import javax.swing.*;
import java.awt.*;
public class ConfigPanel extends JPanel {

    private GlobalConfigPanel globalConfigPanel;
    private RuleEnginePanel ruleEnginePanel;
    private PayloadManagementPanel payloadManagementPanel;

    public ConfigPanel() {
        this.setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        globalConfigPanel = new GlobalConfigPanel();
        ruleEnginePanel = new RuleEnginePanel();
        payloadManagementPanel = new PayloadManagementPanel();

        JSplitPane bottomRowSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ruleEnginePanel, payloadManagementPanel);
        bottomRowSplitPane.setDividerLocation(650);

        JSplitPane mainVerticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, globalConfigPanel, bottomRowSplitPane);
        mainVerticalSplitPane.setDividerLocation(220);

        this.add(mainVerticalSplitPane, BorderLayout.CENTER);
    }


    public RuleEnginePanel getRuleEnginePanel() {
        return this.ruleEnginePanel;
    }
}