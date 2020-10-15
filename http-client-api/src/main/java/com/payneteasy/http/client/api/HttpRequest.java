package com.payneteasy.http.client.api;

import com.payneteasy.http.client.api.HttpHeaders;
import com.payneteasy.http.client.api.HttpMethod;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class HttpRequest {

    @NonNull
    private final String      url;
    @Builder.Default
    private final HttpMethod  method = HttpMethod.GET;
    private final HttpHeaders headers;
    private final byte[]      body;

}
