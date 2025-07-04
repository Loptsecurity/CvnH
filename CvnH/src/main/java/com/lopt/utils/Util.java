package com.lopt.utils;
import com.lopt.bean.Data;
import com.lopt.bean.OriginRequestItem;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;


public class Util {

    public static String fullyURLEncode(String input) throws UnsupportedEncodingException {
        StringBuilder encodedString = new StringBuilder();
        for (char ch : input.toCharArray()) {
            encodedString.append(String.format("%%%02X", (int) ch));
        }
        return encodedString.toString();
    }


    public synchronized static void setOriginRequestId(OriginRequestItem item) {
        item.setId(Data.ORIGIN_REQUEST_TABLE_DATA.size());
    }


    public static synchronized void flushConfigTable(String type, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);  // 清空表格

        if ("domain".equals(type)) {
            fillTableWithList(model, Data.DOMAIN_LIST);
        }
    }

    private static void fillTableWithList(DefaultTableModel model, ArrayList<String> list) {
        if (list != null) {
            for (String item : list) {
                model.addRow(new Object[]{item});
            }
        }
    }

    public static void addConfigData(String type, JTextArea userInputTextArea) {
        String userInput = userInputTextArea.getText();
        if (isBlankInput(userInput)) {
            return;
        }

        if ("domain".equals(type)) {
            addToDomainList(userInput);
        }
    }

    private static boolean isBlankInput(String input) {
        return input == null || input.trim().length() == 0;
    }

    private static void addToDomainList(String input) {
        processListInput(Data.DOMAIN_LIST, input);
    }

    private static void processListInput(ArrayList<String> targetList, String input) {
        String[] lines = input.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !targetList.contains(line)) {
                targetList.add(line);
            }
        }
    }

    public static void removeConfigData(String type, int[] rows) {
        if (rows.length == 0) {
            return;
        }

        if ("domain".equals(type)) {
            removeFromList(Data.DOMAIN_LIST, rows);
        }
    }

    private static void removeFromList(ArrayList<String> list, int[] rows) {
        // 从后往前删，防止索引错乱
        for (int i = rows.length - 1; i >= 0; i--) {
            if (rows[i] < list.size()) {
                list.remove(rows[i]);
            }
        }
    }

    public static String urlEncode(String input) throws UnsupportedEncodingException {
        StringBuilder encodedString = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.' || c == '~') {
                encodedString.append(c);
            } else {
                encodedString.append("%").append(String.format("%02X", (int) c));
            }
        }
        return encodedString.toString();
    }


    public static Object isNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    public static Object isBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            return value;
        }
    }
}