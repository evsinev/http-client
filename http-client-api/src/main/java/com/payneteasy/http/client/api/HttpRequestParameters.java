package com.payneteasy.http.client.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpRequestParameters {

    private final HttpTimeouts        timeouts;
    private final HttpProxyParameters proxyParameters;
}
