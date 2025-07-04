package com.lopt.handler;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import com.lopt.Main;
import com.lopt.config.UserConfig;
import com.lopt.bean.Data;
import com.lopt.bean.OriginRequestItem;
import com.lopt.service.FuzzEventService;
import com.lopt.service.FuzzService;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FuzzHandler implements HttpHandler {
    private final FuzzService fuzzService = new FuzzService();
    private static ThreadPoolExecutor executor;

    public FuzzHandler(){
        rebuildExecutor();
    }

    public static void rebuildExecutor() {
        if(executor != null && !executor.isShutdown()){
            executor.shutdownNow();
        }
        executor = new ThreadPoolExecutor(
                UserConfig.CORE_POOL_SIZE,
                UserConfig.MAX_POOL_SIZE,
                UserConfig.KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        try {
            if (UserConfig.TURN_ON && isToolInScope(requestToBeSent)) {
                String host = new URL(requestToBeSent.url()).getHost();
                boolean inScope = false;
                if(Data.DOMAIN_LIST.isEmpty()){
                    // 如果黑白名单都为空，默认在范围内
                    inScope = true;
                } else {
                    for (String domain : Data.DOMAIN_LIST) {
                        boolean isMatch = host.equals(domain) || (UserConfig.INCLUDE_SUBDOMAIN && host.endsWith("." + domain));
                        if(isMatch){
                            inScope = !UserConfig.BLACK_OR_WHITE_CHOOSE;
                            break;
                        } else {
                            inScope = UserConfig.BLACK_OR_WHITE_CHOOSE;
                        }
                    }
                }

                if(inScope){
                    // 直接调用Fuzz服务，不再进行去重预检查
                    fuzzService.preFuzz(requestToBeSent, 0);
                }
            }
        } catch (Exception e) {
            Main.LOG.logToError("request handler 出现异常" + e.getMessage());
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        int msgId = responseReceived.messageId();
        OriginRequestItem originRequestItem = Data.ORIGIN_REQUEST_TABLE_DATA.get(msgId);

        if (originRequestItem != null) {
            originRequestItem.setOriginResponse(responseReceived);
            originRequestItem.setResponseLength(String.valueOf(responseReceived.body().length()));
            originRequestItem.setResponseCode(String.valueOf(responseReceived.statusCode()));

            if(executor != null && !executor.isShutdown()) {
                executor.submit(() -> fuzzService.startFuzz(msgId));
            }
            FuzzEventService.fireOriginRequestAdded(originRequestItem);
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }

    public static boolean isToolInScope(HttpRequestToBeSent requestToBeSent) {
        return (UserConfig.LISTEN_PROXY && requestToBeSent.toolSource().isFromTool(ToolType.PROXY)) ||
                (UserConfig.LISTEN_REPETER && requestToBeSent.toolSource().isFromTool(ToolType.REPEATER));
    }
}