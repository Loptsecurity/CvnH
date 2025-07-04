package com.lopt.ui;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import com.lopt.Main;
import com.lopt.bean.*;
import com.lopt.bean.FuzzRequestItem.FuzzType;
import com.lopt.handler.FuzzHandler;
import com.lopt.service.FuzzEventService;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lopt.bean.Data.NEW_REQUEST_TO_BE_SENT_DATA;
import static com.lopt.bean.Data.ORIGIN_REQUEST_TABLE_DATA;

public class AttackPanel extends JPanel implements AutoFuzzListener {

    private JTable fuzzRequestItemTable;
    private JTable originRequestItemTable;
    private JButton searchButton;
    private JButton cleanSearchResultButton;
    private JTextField searchTextField;
    private JComboBox<String> searchScopeComboBox;
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private HashMap<Integer, ArrayList<Integer>> highlightMap;
    private JPopupMenu tablePopupMenu;
    private JMenuItem clearHistoryMenuItem;
    private JMenu filterMenu;
    private JRadioButtonMenuItem showAllMenuItem;
    private JRadioButtonMenuItem showDiffMenuItem;
    private ButtonGroup filterGroup;
    private JComboBox<String> fuzzTypeFilterComboBox;

    private enum FuzzResultFilter { ALL, DIFF_LENGTH_ONLY }
    private FuzzResultFilter currentFuzzResultFilter = FuzzResultFilter.ALL;


    public AttackPanel() {
        this.setLayout(new BorderLayout());
        initComponents();
        layoutComponents();
        addListeners();
        FuzzEventService.registerListener(this);
    }

    private void initComponents() {
        fuzzRequestItemTable = new JTable();
        originRequestItemTable = new JTable();
        searchButton = new JButton("搜索");
        cleanSearchResultButton = new JButton("清空高亮");
        searchTextField = new JTextField(20);
        searchScopeComboBox = new JComboBox<>(new String[]{"request", "response"});
        requestEditor = Main.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        responseEditor = Main.API.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        highlightMap = new HashMap<>();
        tablePopupMenu = new JPopupMenu();
        clearHistoryMenuItem = new JMenuItem("清除所有历史记录");
        filterMenu = new JMenu("筛选长度变化");
        showAllMenuItem = new JRadioButtonMenuItem("显示全部", true);
        showDiffMenuItem = new JRadioButtonMenuItem("仅显示长度不同的");
        filterGroup = new ButtonGroup();
        filterGroup.add(showAllMenuItem);
        filterGroup.add(showDiffMenuItem);
        fuzzTypeFilterComboBox = new JComboBox<>(new String[]{
                "显示全部",
                "参数规则",
                "Header规则",
                "删除参数"
        });
    }

    private void layoutComponents() {
        this.setLayout(new BorderLayout());

        filterMenu.add(showAllMenuItem);
        filterMenu.add(showDiffMenuItem);
        tablePopupMenu.add(filterMenu);
        tablePopupMenu.addSeparator();
        tablePopupMenu.add(clearHistoryMenuItem);

        JPanel topTablePanel = new JPanel(new BorderLayout());
        JPanel tableContainer = new JPanel();
        tableContainer.setLayout(new BoxLayout(tableContainer, BoxLayout.X_AXIS));
        originRequestItemTable.setModel(new DefaultTableModel(new String[]{"#", "Method", "Host", "Path", "Length", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        fuzzRequestItemTable.setModel(new DefaultTableModel(new String[]{"Param/Header", "Payload", "Length", "Change", "Status", "Time(ms)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        tableContainer.add(new JScrollPane(originRequestItemTable));
        tableContainer.add(new JScrollPane(fuzzRequestItemTable));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("筛选类型:"));
        searchPanel.add(fuzzTypeFilterComboBox);
        searchPanel.add(new JSeparator(SwingConstants.VERTICAL));
        searchPanel.add(new JLabel("关键字搜索:"));
        searchPanel.add(searchScopeComboBox);
        searchPanel.add(searchTextField);
        searchPanel.add(searchButton);
        searchPanel.add(cleanSearchResultButton);

        topTablePanel.add(tableContainer, BorderLayout.CENTER);
        topTablePanel.add(searchPanel, BorderLayout.SOUTH);

        JPanel bottomEditorPanel = new JPanel();
        bottomEditorPanel.setLayout(new BoxLayout(bottomEditorPanel, BoxLayout.X_AXIS));
        bottomEditorPanel.add(requestEditor.uiComponent());
        bottomEditorPanel.add(responseEditor.uiComponent());
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topTablePanel, bottomEditorPanel);
        mainSplitPane.setDividerLocation(400);
        this.add(mainSplitPane, BorderLayout.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        originRequestItemTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        originRequestItemTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        originRequestItemTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        originRequestItemTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        fuzzRequestItemTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        fuzzRequestItemTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        fuzzRequestItemTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        fuzzRequestItemTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
    }

    private void refreshFuzzResultTable() {
        DefaultTableModel fuzzModel = (DefaultTableModel) fuzzRequestItemTable.getModel();
        fuzzModel.setRowCount(0);

        int selectedOriginRow = originRequestItemTable.getSelectedRow();
        if (selectedOriginRow < 0) return;

        int modelRow = originRequestItemTable.convertRowIndexToModel(selectedOriginRow);
        Integer displayId = (Integer) originRequestItemTable.getModel().getValueAt(modelRow, 0);

        OriginRequestItem originItem = null;
        for(OriginRequestItem item : ORIGIN_REQUEST_TABLE_DATA.values()){
            if(item.getId().equals(displayId)){
                originItem = item;
                break;
            }
        }

        if (originItem == null) return;

        String selectedTypeFilter = (String) fuzzTypeFilterComboBox.getSelectedItem();

        for (FuzzRequestItem fuzzItem : originItem.getFuzzRequestArrayList()) {
            boolean lengthFilterPassed = false;
            if (currentFuzzResultFilter == FuzzResultFilter.ALL) {
                lengthFilterPassed = true;
            } else if (currentFuzzResultFilter == FuzzResultFilter.DIFF_LENGTH_ONLY) {
                String change = fuzzItem.getResponseLengthChange();
                if (change != null && !change.equals("0")) {
                    lengthFilterPassed = true;
                }
            }

            boolean typeFilterPassed = false;
            switch (selectedTypeFilter) {
                case "显示全部":
                    typeFilterPassed = true;
                    break;
                case "参数规则":
                    typeFilterPassed = fuzzItem.getFuzzType() == FuzzType.PARAMETER;
                    break;
                case "Header规则":
                    typeFilterPassed = fuzzItem.getFuzzType() == FuzzType.HEADER;
                    break;
                case "删除参数":
                    typeFilterPassed = fuzzItem.getFuzzType() == FuzzType.PARAMETER_DELETION;
                    break;
            }

            if (lengthFilterPassed && typeFilterPassed) {
                fuzzModel.addRow(new Object[]{
                        fuzzItem.getParam(), fuzzItem.getPayload(),
                        fuzzItem.getResponseLength(), fuzzItem.getResponseLengthChange(),
                        fuzzItem.getResponseCode(), fuzzItem.getResponseTime()
                });
            }
        }
    }


    private void addListeners(){
        fuzzTypeFilterComboBox.addActionListener(e -> refreshFuzzResultTable());

        showAllMenuItem.addActionListener(e -> {
            currentFuzzResultFilter = FuzzResultFilter.ALL;
            refreshFuzzResultTable();
        });
        showDiffMenuItem.addActionListener(e -> {
            currentFuzzResultFilter = FuzzResultFilter.DIFF_LENGTH_ONLY;
            refreshFuzzResultTable();
        });
        clearHistoryMenuItem.addActionListener(e -> clearAllHistory());

        MouseListener popupListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { showPopup(e); }
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    if (e.getComponent() == originRequestItemTable) {
                        int row = originRequestItemTable.rowAtPoint(e.getPoint());
                        if (row >= 0 && !originRequestItemTable.isRowSelected(row)) {
                            originRequestItemTable.setRowSelectionInterval(row, row);
                        }
                        tablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        };
        originRequestItemTable.addMouseListener(popupListener);

        fuzzRequestItemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fuzzTableRow = fuzzRequestItemTable.getSelectedRow();
                int originTableRow = originRequestItemTable.getSelectedRow();
                if (fuzzTableRow < 0 || originTableRow < 0) return;
                int originModelRow = originRequestItemTable.convertRowIndexToModel(originTableRow);
                Integer displayId = (Integer) originRequestItemTable.getModel().getValueAt(originModelRow, 0);

                OriginRequestItem originItem = null;
                for(OriginRequestItem item : ORIGIN_REQUEST_TABLE_DATA.values()){
                    if(item.getId().equals(displayId)){
                        originItem = item;
                        break;
                    }
                }

                if (originItem != null) {
                    List<FuzzRequestItem> displayedItems = originItem.getFuzzRequestArrayList().stream()
                            .filter(item -> {
                                if (currentFuzzResultFilter == FuzzResultFilter.ALL) return true;
                                if (currentFuzzResultFilter == FuzzResultFilter.DIFF_LENGTH_ONLY) {
                                    String change = item.getResponseLengthChange();
                                    return change != null && !change.equals("0");
                                }
                                return false;
                            })
                            .filter(item -> {
                                String selectedTypeFilter = (String) fuzzTypeFilterComboBox.getSelectedItem();
                                switch (selectedTypeFilter) {
                                    case "显示全部": return true;
                                    case "参数规则": return item.getFuzzType() == FuzzType.PARAMETER;
                                    case "Header规则": return item.getFuzzType() == FuzzType.HEADER;
                                    case "删除参数": return item.getFuzzType() == FuzzType.PARAMETER_DELETION;
                                    default: return false;
                                }
                            })
                            .collect(Collectors.toList());

                    if (fuzzTableRow < displayedItems.size()) {
                        FuzzRequestItem selectedFuzzItem = displayedItems.get(fuzzTableRow);
                        if(selectedFuzzItem.getFuzzRequestResponse() != null) {
                            requestEditor.setRequest(selectedFuzzItem.getFuzzRequestResponse().request());
                            responseEditor.setResponse(selectedFuzzItem.getFuzzRequestResponse().response());
                        }
                    }
                }
            }
        });

        originRequestItemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = originRequestItemTable.getSelectedRow();
                if (row < 0) {
                    ((DefaultTableModel) fuzzRequestItemTable.getModel()).setRowCount(0);
                    requestEditor.setRequest(null);
                    responseEditor.setResponse(null);
                    return;
                }
                int modelRow = originRequestItemTable.convertRowIndexToModel(row);
                Integer displayId = (Integer) originRequestItemTable.getModel().getValueAt(modelRow, 0);

                OriginRequestItem foundItem = null;
                for (OriginRequestItem itemInMap : ORIGIN_REQUEST_TABLE_DATA.values()) {
                    if (itemInMap.getId().equals(displayId)) {
                        foundItem = itemInMap;
                        break;
                    }
                }

                if (foundItem != null) {
                    requestEditor.setRequest(foundItem.getOriginRequest());
                    responseEditor.setResponse(foundItem.getOriginResponse());
                    refreshFuzzResultTable();
                }
            }
        });

        originRequestItemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2 || (e.isPopupTrigger() && originRequestItemTable.getSelectedRow() < 0)){
                    int row = originRequestItemTable.rowAtPoint(e.getPoint());
                    if(row > -1){
                        originRequestItemTable.setRowSelectionInterval(row,row);
                    }
                }
            }
        });
    }

    private void clearAllHistory() {
        ORIGIN_REQUEST_TABLE_DATA.clear();
        NEW_REQUEST_TO_BE_SENT_DATA.clear();
        ((DefaultTableModel) originRequestItemTable.getModel()).setRowCount(0);
        ((DefaultTableModel) fuzzRequestItemTable.getModel()).setRowCount(0);
        requestEditor.setRequest(null);
        responseEditor.setResponse(null);
        highlightMap.clear();
        FuzzHandler.rebuildExecutor();
    }

    @Override
    public void onOriginRequestAdded(OriginRequestAddedEvent event) {
        OriginRequestItem item = event.getOriginRequestItem();
        if (item == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                DefaultTableModel model = (DefaultTableModel) originRequestItemTable.getModel();
                model.addRow(new Object[]{
                        item.getId(),
                        item.getMethod(),
                        item.getHost(),
                        item.getPath(),
                        item.getResponseLength(),
                        item.getResponseCode()
                });
            } catch (Exception e) {
                Main.LOG.logToError("UI更新时出现异常: " + e.getMessage());
            }
        });
    }
}