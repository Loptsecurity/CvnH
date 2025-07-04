package com.lopt.ui.peizhi;
import com.lopt.config.UserConfig;
import com.lopt.handler.FuzzHandler;
import com.lopt.Main;
import com.lopt.utils.Util;
import com.lopt.utils.YamlUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
public class GlobalConfigPanel extends JPanel {

    private JRadioButton blackListRadioButton;
    private JRadioButton whiteListRadioButton;
    private ButtonGroup blackOrWhiteGroup;
    private JTable domainTable;
    private JButton addDomainButton;
    private JButton removeDomainButton;
    private JButton cleanRequestItemButton;
    private JButton editDomainButton;
    private JCheckBox turnOnCheckBox;
    private JCheckBox listenProxyCheckBox;
    private JCheckBox listenRepeterCheckBox;
    private JCheckBox includeSubDomainCheckBox;
    private JComboBox<String> languageSupportComboBox;
    private JSpinner corePoolSizeSpinner;
    private JSpinner maxPoolSizeSpinner;
    private JSpinner keepAliveTimeSpinner;
    private JTextField configPathField;
    private JButton changePathButton;
    private JButton applyThreadConfigButton;

    public GlobalConfigPanel() {
        // 使用一个box layout来垂直排列两个部分
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initComponents();
        loadInitialState();
        addListeners();
    }

    private void initComponents() {
        // 实例化所有组件
        blackListRadioButton = new JRadioButton("黑名单");
        whiteListRadioButton = new JRadioButton("白名单");
        blackOrWhiteGroup = new ButtonGroup();
        addDomainButton = new JButton("添加");
        editDomainButton = new JButton("编辑");
        removeDomainButton = new JButton("删除");
        cleanRequestItemButton = new JButton("清空攻击列表");
        turnOnCheckBox = new JCheckBox("启用插件");
        listenProxyCheckBox = new JCheckBox("监听Proxy");
        listenRepeterCheckBox = new JCheckBox("监听Repeater");
        includeSubDomainCheckBox = new JCheckBox("包含子域名");
        languageSupportComboBox = new JComboBox<>(new String[]{"简体中文", "English"});
        corePoolSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        maxPoolSizeSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        keepAliveTimeSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 3600, 1));
        configPathField = new JTextField(35);
        configPathField.setEditable(false);
        changePathButton = new JButton("更改...");
        applyThreadConfigButton = new JButton("应用线程配置");

        // 基础设置面板
        JPanel basicPanel = new JPanel();
        basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.Y_AXIS));
        basicPanel.setBorder(BorderFactory.createTitledBorder("基础设置"));
        JPanel turnOnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        turnOnPanel.add(turnOnCheckBox);
        JPanel listenProxyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listenProxyPanel.add(listenProxyCheckBox);
        listenProxyPanel.add(listenRepeterCheckBox);
        JPanel languageSupportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        languageSupportPanel.add(new JLabel("语言:"));
        languageSupportPanel.add(languageSupportComboBox);
        JPanel cleanRequestListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cleanRequestListPanel.add(cleanRequestItemButton);
        JPanel threadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        threadPanel.add(new JLabel("核心线程:"));
        threadPanel.add(corePoolSizeSpinner);
        threadPanel.add(new JLabel("最大线程:"));
        threadPanel.add(maxPoolSizeSpinner);
        threadPanel.add(new JLabel("存活(秒):"));
        threadPanel.add(keepAliveTimeSpinner);
        threadPanel.add(applyThreadConfigButton);
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathPanel.add(new JLabel("配置文件路径:"));
        pathPanel.add(configPathField);
        pathPanel.add(changePathButton);
        basicPanel.add(turnOnPanel);
        basicPanel.add(listenProxyPanel);
        basicPanel.add(languageSupportPanel);
        basicPanel.add(cleanRequestListPanel);
        basicPanel.add(threadPanel);
        basicPanel.add(pathPanel);

        // 域名作用域面板
        JPanel domainMainPanel = new JPanel(new BorderLayout(5, 5));
        domainMainPanel.setBorder(BorderFactory.createTitledBorder("域名作用域"));
        DefaultTableModel domainModel = new DefaultTableModel(new String[]{"Domain"}, 0) { @Override public boolean isCellEditable(int r, int c){return false;} };
        domainTable = new JTable(domainModel);
        domainTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane domainTableScrollPane = new JScrollPane(domainTable);
        JPanel domainButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        domainButtons.add(addDomainButton);
        domainButtons.add(editDomainButton);
        domainButtons.add(removeDomainButton);
        JPanel domainOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blackOrWhiteGroup.add(blackListRadioButton);
        blackOrWhiteGroup.add(whiteListRadioButton);
        domainOptions.add(blackListRadioButton);
        domainOptions.add(whiteListRadioButton);
        domainOptions.add(includeSubDomainCheckBox);
        domainMainPanel.add(domainTableScrollPane, BorderLayout.CENTER);
        domainMainPanel.add(domainButtons, BorderLayout.SOUTH);
        domainMainPanel.add(domainOptions, BorderLayout.NORTH);

        this.add(basicPanel);
        this.add(domainMainPanel);
    }

    public void loadInitialState(){
        Util.flushConfigTable("domain", domainTable);
        turnOnCheckBox.setSelected(UserConfig.TURN_ON);
        listenProxyCheckBox.setSelected(UserConfig.LISTEN_PROXY);
        listenRepeterCheckBox.setSelected(UserConfig.LISTEN_REPETER);
        blackListRadioButton.setSelected(UserConfig.BLACK_OR_WHITE_CHOOSE);
        whiteListRadioButton.setSelected(!UserConfig.BLACK_OR_WHITE_CHOOSE);
        includeSubDomainCheckBox.setSelected(UserConfig.INCLUDE_SUBDOMAIN);
        corePoolSizeSpinner.setValue(UserConfig.CORE_POOL_SIZE);
        maxPoolSizeSpinner.setValue(UserConfig.MAX_POOL_SIZE);
        keepAliveTimeSpinner.setValue(UserConfig.KEEP_ALIVE_TIME);
        configPathField.setText(YamlUtil.getCurrentConfigPath());
    }

    private void addListeners(){
        ActionListener blackWhiteListener = e -> {
            UserConfig.BLACK_OR_WHITE_CHOOSE = blackListRadioButton.isSelected();
            YamlUtil.exportToYaml();
        };
        blackListRadioButton.addActionListener(blackWhiteListener);
        whiteListRadioButton.addActionListener(blackWhiteListener);
        turnOnCheckBox.addItemListener(e -> {
            UserConfig.TURN_ON = e.getStateChange() == ItemEvent.SELECTED;
            YamlUtil.exportToYaml();
        });
        listenProxyCheckBox.addItemListener(e -> {
            UserConfig.LISTEN_PROXY = e.getStateChange() == ItemEvent.SELECTED;
            YamlUtil.exportToYaml();
        });
        listenRepeterCheckBox.addItemListener(e -> {
            UserConfig.LISTEN_REPETER = e.getStateChange() == ItemEvent.SELECTED;
            YamlUtil.exportToYaml();
        });
        includeSubDomainCheckBox.addItemListener(e -> {
            UserConfig.INCLUDE_SUBDOMAIN = e.getStateChange() == ItemEvent.SELECTED;
            YamlUtil.exportToYaml();
        });
        addDomainButton.addActionListener(e -> {
            JTextArea textArea = new JTextArea(10, 30);
            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "添加域名 (每行一个)", JOptionPane.OK_CANCEL_OPTION);
            if(result == JOptionPane.OK_OPTION){
                Util.addConfigData("domain", textArea);
                Util.flushConfigTable("domain", domainTable);
                YamlUtil.exportToYaml();
            }
        });
        removeDomainButton.addActionListener(e -> {
            int[] rows = domainTable.getSelectedRows();
            if(rows.length > 0){
                Util.removeConfigData("domain", rows);
                Util.flushConfigTable("domain", domainTable);
                YamlUtil.exportToYaml();
            }
        });
        applyThreadConfigButton.addActionListener(e -> {
            UserConfig.CORE_POOL_SIZE = (Integer) corePoolSizeSpinner.getValue();
            UserConfig.MAX_POOL_SIZE = (Integer) maxPoolSizeSpinner.getValue();
            UserConfig.KEEP_ALIVE_TIME = ((Number) keepAliveTimeSpinner.getValue()).longValue();
            YamlUtil.exportToYaml();
            FuzzHandler.rebuildExecutor();
            JOptionPane.showMessageDialog(this, "线程池配置已应用！");
        });
        changePathButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("选择新的配置文件(config.yml)");
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String newPath = selectedFile.getAbsolutePath();
                Main.API.persistence().preferences().setString("CvnH.configPath", newPath);
                configPathField.setText(newPath);
                JOptionPane.showMessageDialog(this, "配置文件路径已保存！请重启Burp或重新加载插件以使新路径生效。", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}