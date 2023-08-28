package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.HttpHeader;

import java.util.List;
import java.util.Optional;

public class HttpHeaderFinder {

    private final List<HttpHeader> headers;

    public HttpHeaderFinder(List<HttpHeader> headers) {
        this.headers = headers;
    }

    public String get(String aHeaderName) {
        String lowerName = aHeaderName.toLowerCase();
        for (HttpHeader header : headers) {
            String name = header.getName();
            if(name.toLowerCase().equals(lowerName)) {
                return header.getValue();
            }
        }
        return null;
    }

    public Optional<String> getOpt(String aHeaderName) {
        return Optional.ofNullable(get(aHeaderName));
    }
}
