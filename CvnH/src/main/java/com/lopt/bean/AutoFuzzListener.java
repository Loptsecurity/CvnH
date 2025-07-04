package com.lopt.bean;

import java.util.EventListener;

public interface AutoFuzzListener extends EventListener {
    void onOriginRequestAdded(OriginRequestAddedEvent event);
}