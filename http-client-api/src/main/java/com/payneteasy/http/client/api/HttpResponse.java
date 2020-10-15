package com.payneteasy.http.client.api;

import com.payneteasy.http.client.api.HttpHeader;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
public class HttpResponse {

    private final int              statusCode;
    private final String           reasonPhrase;
    private final List<HttpHeader> headers;
    private final byte[]           body;

    @Override
    public String toString() {
        return  "HttpResponse("
                + "status=" + statusCode
                + " " + reasonPhrase
                + ", headers=" + headers
                + ", body=" + new String(body, StandardCharsets.UTF_8)
                + ")";
    }

}
