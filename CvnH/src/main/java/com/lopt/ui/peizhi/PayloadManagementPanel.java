package com.lopt.ui.peizhi;

import com.lopt.bean.FuzzRule;
import com.lopt.utils.YamlUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static com.lopt.bean.Data.CATEGORIZED_PAYLOADS;
import static com.lopt.bean.Data.FUZZING_RULES;
public class PayloadManagementPanel extends JPanel {
    private JTable payloadCategoryTable;
    private JButton addCategoryButton;
    private JButton editCategoryButton;
    private JButton removeCategoryButton;
    private JTable payloadTable;
    private JButton addPayloadButton;
    private JButton editPayloadButton;
    private JButton removePayloadButton;

    public PayloadManagementPanel() {
        initComponents();
        loadInitialState();
        addListeners();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder("Payload管理"));

        payloadCategoryTable = new JTable();
        addCategoryButton = new JButton("添加");
        editCategoryButton = new JButton("编辑");
        removeCategoryButton = new JButton("删除");
        payloadTable = new JTable();
        addPayloadButton = new JButton("添加");
        editPayloadButton = new JButton("编辑");
        removePayloadButton = new JButton("删除");

        JPanel categoryPanel = new JPanel(new BorderLayout(5, 5));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("漏洞类别"));
        DefaultTableModel categoryTableModel = new DefaultTableModel(new String[]{"类别名称"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payloadCategoryTable.setModel(categoryTableModel);
        JScrollPane categoryTableScrollPane = new JScrollPane(payloadCategoryTable);
        categoryPanel.add(categoryTableScrollPane, BorderLayout.CENTER);
        JPanel categoryButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        categoryButtonsPanel.add(addCategoryButton);
        categoryButtonsPanel.add(editCategoryButton);
        categoryButtonsPanel.add(removeCategoryButton);
        categoryPanel.add(categoryButtonsPanel, BorderLayout.SOUTH);

        JPanel payloadMainPanel = new JPanel(new BorderLayout(5, 5));
        payloadMainPanel.setBorder(BorderFactory.createTitledBorder("Payloads"));
        DefaultTableModel payloadTableModel = new DefaultTableModel(new Object[]{"Payload"}, 0){
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payloadTable.setModel(payloadTableModel);
        JScrollPane payloadTableScrollPane = new JScrollPane(payloadTable);
        payloadMainPanel.add(payloadTableScrollPane, BorderLayout.CENTER);
        JPanel payloadOperatePanel = new JPanel();
        payloadOperatePanel.setLayout(new BoxLayout(payloadOperatePanel, BoxLayout.Y_AXIS));
        payloadOperatePanel.add(addPayloadButton);
        payloadOperatePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        payloadOperatePanel.add(editPayloadButton);
        payloadOperatePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        payloadOperatePanel.add(removePayloadButton);
        JPanel payloadEastPanel = new JPanel();
        payloadEastPanel.add(payloadOperatePanel);
        payloadMainPanel.add(payloadEastPanel, BorderLayout.EAST);

        JSplitPane payloadSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, categoryPanel, payloadMainPanel);
        payloadSplitPane.setDividerLocation(200);
        this.add(payloadSplitPane, BorderLayout.CENTER);
    }

    public void loadInitialState(){
        DefaultTableModel categoryModel = (DefaultTableModel) payloadCategoryTable.getModel();
        categoryModel.setRowCount(0);
        if (CATEGORIZED_PAYLOADS != null) {
            for (String categoryName : CATEGORIZED_PAYLOADS.keySet()) {
                categoryModel.addRow(new Object[]{categoryName});
            }
        }
    }

    private void addListeners() {
        payloadCategoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                DefaultTableModel payloadModel = (DefaultTableModel) payloadTable.getModel();
                payloadModel.setRowCount(0);
                int selectedRow = payloadCategoryTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String categoryName = (String) payloadCategoryTable.getValueAt(selectedRow, 0);
                    ArrayList<String> payloads = CATEGORIZED_PAYLOADS.get(categoryName);
                    if (payloads != null) {
                        for (String payload : payloads) {
                            payloadModel.addRow(new Object[]{payload});
                        }
                    }
                }
            }
        });

        addCategoryButton.addActionListener(e -> {
            String newCategory = JOptionPane.showInputDialog(this, "输入新的类别名称:", "添加类别", JOptionPane.PLAIN_MESSAGE);
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                newCategory = newCategory.trim();
                if (CATEGORIZED_PAYLOADS.containsKey(newCategory)) {
                    JOptionPane.showMessageDialog(this, "该类别已存在！", "错误", JOptionPane.ERROR_MESSAGE);
                } else {
                    CATEGORIZED_PAYLOADS.put(newCategory, new ArrayList<>());
                    ((DefaultTableModel)payloadCategoryTable.getModel()).addRow(new Object[]{newCategory});
                    YamlUtil.exportToYaml();
                }
            }
        });

        editCategoryButton.addActionListener(e -> {
            int selectedRow = payloadCategoryTable.getSelectedRow();
            if (selectedRow < 0) { JOptionPane.showMessageDialog(this, "请先选择要编辑的类别。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            String oldCategory = (String) payloadCategoryTable.getValueAt(selectedRow, 0);
            String newCategory = JOptionPane.showInputDialog(this, "输入新的类别名称:", oldCategory);
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                newCategory = newCategory.trim();
                if (!newCategory.equals(oldCategory)) {
                    if (CATEGORIZED_PAYLOADS.containsKey(newCategory)) {
                        JOptionPane.showMessageDialog(this, "该类别已存在！", "错误", JOptionPane.ERROR_MESSAGE);
                    } else {
                        ArrayList<String> payloads = CATEGORIZED_PAYLOADS.remove(oldCategory);
                        CATEGORIZED_PAYLOADS.put(newCategory, payloads);
                        payloadCategoryTable.setValueAt(newCategory, selectedRow, 0);
                        for (int i = 0; i < FUZZING_RULES.size(); i++) {
                            FuzzRule currentRule = FUZZING_RULES.get(i);
                            if (currentRule.getCategoryName() != null && currentRule.getCategoryName().equals(oldCategory)) {
                                currentRule.setCategoryName(newCategory);
                            }
                        }
                        YamlUtil.exportToYaml();
                    }
                }
            }
        });

        removeCategoryButton.addActionListener(e -> {
            int selectedRow = payloadCategoryTable.getSelectedRow();
            if (selectedRow < 0) { JOptionPane.showMessageDialog(this, "请先选择要删除的类别。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "确定要删除选中的类别及其所有Payload吗？\n同时会删除所有引用此分类的规则！", "严重警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                String category = (String) payloadCategoryTable.getValueAt(selectedRow, 0);
                CATEGORIZED_PAYLOADS.remove(category);
                ((DefaultTableModel)payloadCategoryTable.getModel()).removeRow(selectedRow);
                FUZZING_RULES.removeIf(rule -> category.equals(rule.getCategoryName()));
                Component parent = this.getParent();
                while(parent != null && !(parent instanceof ConfigPanel)){
                    parent = parent.getParent();
                }
                if(parent instanceof ConfigPanel){
                    ((ConfigPanel)parent).getRuleEnginePanel().refreshRulesTable();
                }
                YamlUtil.exportToYaml();
            }
        });

        addPayloadButton.addActionListener(e -> {
            int selectedCategoryRow = payloadCategoryTable.getSelectedRow();
            if (selectedCategoryRow < 0) { JOptionPane.showMessageDialog(this, "请先在左侧选择一个类别。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            String category = (String) payloadCategoryTable.getValueAt(selectedCategoryRow, 0);

            JTextArea textArea = new JTextArea(10, 40);
            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "添加Payloads (每行一个)", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String[] lines = textArea.getText().split("\n");
                ArrayList<String> payloadList = CATEGORIZED_PAYLOADS.get(category);
                DefaultTableModel payloadModel = (DefaultTableModel) payloadTable.getModel();
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty() && !payloadList.contains(trimmedLine)) {
                        payloadList.add(trimmedLine);
                        payloadModel.addRow(new Object[]{trimmedLine});
                    }
                }
                YamlUtil.exportToYaml();
            }
        });

        removePayloadButton.addActionListener(e -> {
            int selectedCategoryRow = payloadCategoryTable.getSelectedRow();
            int[] selectedPayloadRows = payloadTable.getSelectedRows();
            if (selectedCategoryRow < 0 || selectedPayloadRows.length == 0) { JOptionPane.showMessageDialog(this, "请先选择一个类别，并在右侧选择要删除的Payload。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }

            String category = (String) payloadCategoryTable.getValueAt(selectedCategoryRow, 0);
            ArrayList<String> payloadList = CATEGORIZED_PAYLOADS.get(category);

            Integer[] modelRows = new Integer[selectedPayloadRows.length];
            for(int i=0; i<selectedPayloadRows.length; i++){
                modelRows[i] = payloadTable.convertRowIndexToModel(selectedPayloadRows[i]);
            }
            Arrays.sort(modelRows, Collections.reverseOrder());

            for(int modelRow : modelRows){
                payloadList.remove(modelRow);
            }

            DefaultTableModel currentPayloadModel = (DefaultTableModel) payloadTable.getModel();
            currentPayloadModel.setRowCount(0);
            ArrayList<String> currentPayloads = CATEGORIZED_PAYLOADS.get(category);
            if(currentPayloads != null){
                for(String p : currentPayloads){
                    currentPayloadModel.addRow(new Object[]{p});
                }
            }
            YamlUtil.exportToYaml();
        });

        editPayloadButton.addActionListener(e -> {
            int selectedCategoryRow = payloadCategoryTable.getSelectedRow();
            int selectedPayloadRow = payloadTable.getSelectedRow();
            if (selectedCategoryRow < 0 || selectedPayloadRow < 0) { JOptionPane.showMessageDialog(this, "请先选择一个类别，并在右侧选择一个要编辑的Payload。", "提示", JOptionPane.INFORMATION_MESSAGE); return; }
            String category = (String) payloadCategoryTable.getValueAt(selectedCategoryRow, 0);
            ArrayList<String> payloadList = CATEGORIZED_PAYLOADS.get(category);
            String oldPayload = payloadList.get(selectedPayloadRow);
            String newPayload = JOptionPane.showInputDialog(this, "编辑Payload:", oldPayload);
            if(newPayload != null && !newPayload.trim().isEmpty()){
                payloadList.set(selectedPayloadRow, newPayload.trim());
                ((DefaultTableModel)payloadTable.getModel()).setValueAt(newPayload.trim(), selectedPayloadRow, 0);
                YamlUtil.exportToYaml();
            }
        });
    }
}