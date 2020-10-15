package com.payneteasy.http.client.api;

import java.util.ArrayList;
import java.util.List;

public class HttpHeadersBuilder {

    private final List<HttpHeader> headers = new ArrayList<>();

    public HttpHeadersBuilder add(String aName, long aValue) {
        return add(aName, "" + aValue);
    }

    public HttpHeadersBuilder add(String aName, String aValue) {
        headers.add(new HttpHeader(aName, aValue));
        return this;
    }

    public HttpHeaders build() {
        return new HttpHeaders(headers);
    }
}
