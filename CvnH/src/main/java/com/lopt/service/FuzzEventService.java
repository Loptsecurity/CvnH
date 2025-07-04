package com.lopt.service;
import com.lopt.bean.AutoFuzzListener;
import com.lopt.bean.OriginRequestAddedEvent;
import com.lopt.bean.OriginRequestItem;

import java.util.ArrayList;
import java.util.List;

public class FuzzEventService {
    private static final List<AutoFuzzListener> listeners = new ArrayList<>();

    public static synchronized void registerListener(AutoFuzzListener listener) {
        listeners.add(listener);
    }

    public static synchronized void unregisterListener(AutoFuzzListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void fireOriginRequestAdded(OriginRequestItem item) {
        OriginRequestAddedEvent event = new OriginRequestAddedEvent(item);
        for (AutoFuzzListener listener : listeners) {
            listener.onOriginRequestAdded(event);
        }
    }
}