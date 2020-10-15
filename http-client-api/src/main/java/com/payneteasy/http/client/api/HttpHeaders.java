package com.payneteasy.http.client.api;

import java.util.Collections;
import java.util.List;

public class HttpHeaders {

    private final List<HttpHeader> headers;

    public HttpHeaders(List<HttpHeader> headers) {
        this.headers = Collections.unmodifiableList(headers);
    }

    public static HttpHeaders singleHeader(String aName, String aValue) {
        return new HttpHeaders(Collections.singletonList(new HttpHeader(aName, aValue)));
    }

    public  List<HttpHeader> asList() {
        return headers;
    }
}
