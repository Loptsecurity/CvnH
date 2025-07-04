package com.lopt.bean;

public class OriginRequestAddedEvent {
    private final OriginRequestItem originRequestItem;

    public OriginRequestAddedEvent(OriginRequestItem originRequestItem) {
        this.originRequestItem = originRequestItem;
    }

    public OriginRequestItem getOriginRequestItem() {
        return originRequestItem;
    }
}