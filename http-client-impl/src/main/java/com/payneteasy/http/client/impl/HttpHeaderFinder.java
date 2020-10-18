package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.HttpHeader;

import java.util.List;
import java.util.Optional;

public class HttpHeaderFinder {

    private final List<HttpHeader> headers;

    public HttpHeaderFinder(List<HttpHeader> headers) {
        this.headers = headers;
    }

    public Optional<String> get(String aHeaderName) {
        String lowerName = aHeaderName.toLowerCase();
        for (HttpHeader header : headers) {
            String name = header.getName();
            if(name != null && name.toLowerCase().equals(lowerName)) {
                return Optional.of(header.getValue());
            }
        }
        return Optional.empty();
    }
}
