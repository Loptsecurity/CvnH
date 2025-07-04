package com.lopt.menu;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.lopt.Main;
import com.lopt.bean.Data;
import com.lopt.bean.OriginRequestItem;
import com.lopt.service.FuzzEventService;
import com.lopt.service.FuzzService;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FuzzMenu implements ContextMenuItemsProvider {
    private final FuzzService fuzzService = new FuzzService();
    private static final ExecutorService menuExecutor = Executors.newSingleThreadExecutor();
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(-1);

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        ArrayList<Component> menus = new ArrayList<>();

        event.messageEditorRequestResponse().ifPresent(editorReqResp -> {
            HttpRequest request = editorReqResp.requestResponse().request();

            // 功能一：“Deleting Parameters”
            if (request.parameters().size() > 0) {
                JMenuItem deletingParamsMenuItem = new JMenuItem("Deleting Parameters");
                deletingParamsMenuItem.addActionListener(e -> {
                    menuExecutor.submit(() -> {
                        try {
                            int currentId = ID_COUNTER.getAndDecrement();
                            fuzzService.performParameterDeletionFuzz(request, currentId);
                            HttpRequestResponse baselineResponse = Main.API.http().sendRequest(request);
                            OriginRequestItem originItem = Data.ORIGIN_REQUEST_TABLE_DATA.get(currentId);
                            if (originItem != null) {
                                originItem.setOriginResponse(baselineResponse.response());
                                originItem.setResponseLength(String.valueOf(baselineResponse.response().body().length()));
                                originItem.setResponseCode(String.valueOf(baselineResponse.response().statusCode()));
                                fuzzService.startFuzz(currentId);
                                FuzzEventService.fireOriginRequestAdded(originItem);
                            }
                        } catch (Exception ex) {
                            Main.LOG.logToError("[ERROR] 右键删除参数fuzz出现异常: " + ex.getMessage());
                        }
                    });
                });
                menus.add(deletingParamsMenuItem);
            }

            // 功能二：“Fuzz Headers”
            JMenuItem fuzzHeadersMenuItem = new JMenuItem("Fuzz Headers");
            fuzzHeadersMenuItem.addActionListener(e -> {
                menuExecutor.submit(() -> {
                    try {
                        int currentId = ID_COUNTER.getAndDecrement();
                        // 调用新的服务方法
                        fuzzService.performHeaderFuzz(request, currentId);
                        HttpRequestResponse baselineResponse = Main.API.http().sendRequest(request);
                        OriginRequestItem originItem = Data.ORIGIN_REQUEST_TABLE_DATA.get(currentId);
                        if (originItem != null) {
                            originItem.setOriginResponse(baselineResponse.response());
                            originItem.setResponseLength(String.valueOf(baselineResponse.response().body().length()));
                            originItem.setResponseCode(String.valueOf(baselineResponse.response().statusCode()));
                            fuzzService.startFuzz(currentId);
                            FuzzEventService.fireOriginRequestAdded(originItem);
                        }
                    } catch (Exception ex) {
                        Main.LOG.logToError("[ERROR] 右键Fuzz Headers出现异常: " + ex.getMessage());
                    }
                });
            });
            menus.add(fuzzHeadersMenuItem);

        });
        return menus;
    }
}