package com.lopt.ui.peizhi;
import com.lopt.bean.FuzzRule;
import com.lopt.utils.YamlUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;

import static com.lopt.bean.Data.CATEGORIZED_PAYLOADS;
import static com.lopt.bean.Data.FUZZING_RULES;

public class RuleEnginePanel extends JPanel {

    private JTable fuzzingRulesTable;
    private JButton addRuleButton;
    private JButton editRuleButton;
    private JButton removeRuleButton;
    private JComboBox<String> ruleFilterComboBox;

    public RuleEnginePanel() {
        initComponents();
        addListeners();
        refreshRulesTable();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createTitledBorder("规则引擎"));

        ruleFilterComboBox = new JComboBox<>(new String[]{"显示全部", "仅显示参数规则", "仅显示Header规则"});
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("筛选规则类型:"));
        filterPanel.add(ruleFilterComboBox);
        this.add(filterPanel, BorderLayout.NORTH);

        DefaultTableModel ruleTableModel = new DefaultTableModel(new String[]{"激活", "类型", "匹配名称(正则)", "Payload类别", "模式"}, 0){
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        fuzzingRulesTable = new JTable(ruleTableModel);
        fuzzingRulesTable.getColumnModel().getColumn(0).setMaxWidth(40);
        fuzzingRulesTable.getColumnModel().getColumn(1).setMaxWidth(80);
        fuzzingRulesTable.getColumnModel().getColumn(4).setMaxWidth(120);
        JScrollPane rulesTableScrollPane = new JScrollPane(fuzzingRulesTable);
        this.add(rulesTableScrollPane, BorderLayout.CENTER);

        addRuleButton = new JButton("添加规则");
        editRuleButton = new JButton("编辑规则");
        removeRuleButton = new JButton("删除规则");
        JPanel ruleButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        ruleButtonsPanel.add(addRuleButton);
        ruleButtonsPanel.add(editRuleButton);
        ruleButtonsPanel.add(removeRuleButton);
        this.add(ruleButtonsPanel, BorderLayout.SOUTH);
    }

    public void refreshRulesTable() {
        DefaultTableModel model = (DefaultTableModel) fuzzingRulesTable.getModel();
        model.setRowCount(0);
        String filter = (String) ruleFilterComboBox.getSelectedItem();
        if (FUZZING_RULES != null) {
            for (FuzzRule rule : FUZZING_RULES) {
                boolean show = false;
                if ("显示全部".equals(filter)) show = true;
                else if ("仅显示参数规则".equals(filter) && rule.getType() == FuzzRule.RuleType.PARAMETER) show = true;
                else if ("仅显示Header规则".equals(filter) && rule.getType() == FuzzRule.RuleType.HEADER) show = true;

                if (show) {
                    if (rule.getType() == FuzzRule.RuleType.PARAMETER) {
                        String mode = rule.isAppendMode() ? "追加" : "替换";
                        if (rule.isUrlEncode()) mode += "+URL";
                        String dedupeStrategy = rule.getDeduplicationStrategy() == FuzzRule.DeduplicationStrategy.HOST ? "(主机去重)" : "";
                        model.addRow(new Object[]{rule.isActive(), "参数", rule.getRegex(), rule.getCategoryName(), mode + dedupeStrategy});
                    } else {
                        String mode = rule.isAddHeaderIfNotExists() ? "不存在则添加" : "仅替换";
                        model.addRow(new Object[]{rule.isActive(), "Header", rule.getRegex(), rule.getCategoryName(), mode});
                    }
                }
            }
        }
    }

    private void addListeners() {
        ruleFilterComboBox.addActionListener(e -> refreshRulesTable());

        fuzzingRulesTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 0 && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int modelRow = fuzzingRulesTable.convertRowIndexToModel(row);
                boolean isActive = (boolean) fuzzingRulesTable.getValueAt(row, 0);
                FUZZING_RULES.get(modelRow).setActive(isActive);
                YamlUtil.exportToYaml();
            }
        });

        addRuleButton.addActionListener(e -> {
            JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            selectionPanel.add(new JLabel("请选择要添加的规则类型:"));
            String[] ruleTypes = {"参数规则", "Header规则"};
            JComboBox<String> typeComboBox = new JComboBox<>(ruleTypes);
            selectionPanel.add(typeComboBox);
            int selectionResult = JOptionPane.showConfirmDialog(this, selectionPanel, "选择规则类型", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (selectionResult != JOptionPane.OK_OPTION) return;
            String selectedType = (String) typeComboBox.getSelectedItem();

            if (CATEGORIZED_PAYLOADS.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先在“Payload管理”中至少创建一个Payload类别。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            boolean isParamRule = "参数规则".equals(selectedType);
            String title = isParamRule ? "添加参数规则" : "添加Header规则";
            String labelText = isParamRule ? "参数名(正则):" : "Header名(正则):";

            JPanel dialogPanel = new JPanel(new BorderLayout(5, 5));
            int gridRows = isParamRule ? 4 : 3;
            JPanel formPanel = new JPanel(new GridLayout(gridRows, 2, 5, 5));
            JComboBox<String> categoryComboBox = new JComboBox<>(CATEGORIZED_PAYLOADS.keySet().toArray(new String[0]));
            JTextField regexField = new JTextField();

            formPanel.add(new JLabel("漏洞类别:"));
            formPanel.add(categoryComboBox);
            formPanel.add(new JLabel(labelText));
            formPanel.add(regexField);

            JCheckBox appendModeBox = new JCheckBox("追加模式 (默认: 替换)");
            JCheckBox urlEncodeBox = new JCheckBox("Payload进行URL编码");
            JCheckBox addIfNotExistBox = new JCheckBox("若Header不存在则添加");
            JComboBox<String> dedupeComboBox = new JComboBox<>(new String[]{"按完整端点去重", "仅按主机去重"});

            if(isParamRule) {
                formPanel.add(new JLabel("去重策略:"));
                formPanel.add(dedupeComboBox);
                formPanel.add(appendModeBox);
                formPanel.add(urlEncodeBox);
            } else {
                formPanel.add(addIfNotExistBox);
            }

            dialogPanel.add(formPanel, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(this, dialogPanel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String category = (String) categoryComboBox.getSelectedItem();
                String regex = regexField.getText();
                if (category != null && !regex.trim().isEmpty()) {
                    FuzzRule.RuleType type = isParamRule ? FuzzRule.RuleType.PARAMETER : FuzzRule.RuleType.HEADER;
                    FuzzRule newRule = FuzzRule.createRule(true, type, category, regex.trim());
                    if(isParamRule) {
                        newRule.setAppendMode(appendModeBox.isSelected());
                        newRule.setUrlEncode(urlEncodeBox.isSelected());
                        if ("仅按主机去重".equals(dedupeComboBox.getSelectedItem())) {
                            newRule.setDeduplicationStrategy(FuzzRule.DeduplicationStrategy.HOST);
                        } else {
                            newRule.setDeduplicationStrategy(FuzzRule.DeduplicationStrategy.ENDPOINT);
                        }
                    } else {
                        newRule.setAddHeaderIfNotExists(addIfNotExistBox.isSelected());
                    }
                    FUZZING_RULES.add(newRule);
                    refreshRulesTable();
                    YamlUtil.exportToYaml();
                }
            }
        });

        editRuleButton.addActionListener(e -> {
            int selectedViewRow = fuzzingRulesTable.getSelectedRow();
            if (selectedViewRow < 0) { JOptionPane.showMessageDialog(this, "请先选择一条规则进行编辑。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            int modelRow = fuzzingRulesTable.convertRowIndexToModel(selectedViewRow);
            FuzzRule oldRule = FUZZING_RULES.get(modelRow);

            boolean isParamRule = oldRule.getType() == FuzzRule.RuleType.PARAMETER;
            String title = isParamRule ? "编辑参数规则" : "编辑Header规则";
            String labelText = isParamRule ? "参数名(正则):" : "Header名(正则):";

            JPanel dialogPanel = new JPanel(new BorderLayout(5, 5));
            int gridRows = isParamRule ? 4 : 3;
            JPanel formPanel = new JPanel(new GridLayout(gridRows, 2, 5, 5));
            JComboBox<String> categoryComboBox = new JComboBox<>(CATEGORIZED_PAYLOADS.keySet().toArray(new String[0]));
            JTextField regexField = new JTextField();

            categoryComboBox.setSelectedItem(oldRule.getCategoryName());
            regexField.setText(oldRule.getRegex());

            formPanel.add(new JLabel("漏洞类别:")); formPanel.add(categoryComboBox);
            formPanel.add(new JLabel(labelText)); formPanel.add(regexField);

            JCheckBox appendModeBox = new JCheckBox("追加模式 (默认: 替换)");
            JCheckBox urlEncodeBox = new JCheckBox("Payload进行URL编码");
            JCheckBox addIfNotExistBox = new JCheckBox("若Header不存在则添加");
            JComboBox<String> dedupeComboBox = new JComboBox<>(new String[]{"按完整端点去重", "仅按主机去重"});

            if(isParamRule){
                appendModeBox.setSelected(oldRule.isAppendMode());
                urlEncodeBox.setSelected(oldRule.isUrlEncode());
                dedupeComboBox.setSelectedItem(oldRule.getDeduplicationStrategy() == FuzzRule.DeduplicationStrategy.HOST ? "仅按主机去重" : "按完整端点去重");
                formPanel.add(new JLabel("去重策略:")); formPanel.add(dedupeComboBox);
                formPanel.add(appendModeBox);
                formPanel.add(urlEncodeBox);
            } else {
                addIfNotExistBox.setSelected(oldRule.isAddHeaderIfNotExists());
                formPanel.add(addIfNotExistBox);
            }

            dialogPanel.add(formPanel, BorderLayout.CENTER);
            int result = JOptionPane.showConfirmDialog(this, dialogPanel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String category = (String) categoryComboBox.getSelectedItem();
                String regex = regexField.getText();
                if (category != null && !regex.trim().isEmpty()) {
                    oldRule.setCategoryName(category);
                    oldRule.setRegex(regex.trim());
                    if(isParamRule){
                        oldRule.setAppendMode(appendModeBox.isSelected());
                        oldRule.setUrlEncode(urlEncodeBox.isSelected());
                        if ("仅按主机去重".equals(dedupeComboBox.getSelectedItem())) {
                            oldRule.setDeduplicationStrategy(FuzzRule.DeduplicationStrategy.HOST);
                        } else {
                            oldRule.setDeduplicationStrategy(FuzzRule.DeduplicationStrategy.ENDPOINT);
                        }
                    } else {
                        oldRule.setAddHeaderIfNotExists(addIfNotExistBox.isSelected());
                    }
                    refreshRulesTable();
                    YamlUtil.exportToYaml();
                }
            }
        });

        removeRuleButton.addActionListener(e -> {
            int[] selectedViewRows = fuzzingRulesTable.getSelectedRows();
            if (selectedViewRows.length == 0) { JOptionPane.showMessageDialog(this, "请先选择要删除的规则。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "确定要删除选中的 " + selectedViewRows.length + " 条规则吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION){
                Integer[] modelRows = new Integer[selectedViewRows.length];
                for(int i=0; i < selectedViewRows.length; i++){
                    modelRows[i] = fuzzingRulesTable.convertRowIndexToModel(selectedViewRows[i]);
                }
                Arrays.sort(modelRows, Collections.reverseOrder());

                for(int modelRow : modelRows){
                    FUZZING_RULES.remove(modelRow);
                }

                refreshRulesTable();
                YamlUtil.exportToYaml();
            }
        });
    }
}