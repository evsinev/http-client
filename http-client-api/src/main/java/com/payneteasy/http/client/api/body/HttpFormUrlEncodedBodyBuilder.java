package com.payneteasy.http.client.api.body;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpFormUrlEncodedBodyBuilder {

    private final StringBuilder sb = new StringBuilder();

    public HttpFormUrlEncodedBodyBuilder add(String aName, String aValue) {
        if(sb.length() > 0) {
            sb.append("&");
        }

        sb.append(urlEncode(aName));
        sb.append("=");
        sb.append(urlEncode(aValue));

        return this;
    }

    private String urlEncode(String aName) {
        try {
            return URLEncoder.encode(aName, "utf-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot find utf-8", e);
        }
    }

    public byte[] buildBytes() {
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
