package com.payneteasy.http.client.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpTimeouts {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int callTimeoutMs;
    private final int writeTimeoutMs;

    public HttpTimeouts(int aConnectTimeoutMs, int aReadTimeoutMs) {
        this(aConnectTimeoutMs, aReadTimeoutMs, aReadTimeoutMs, aReadTimeoutMs);
    }

    public HttpTimeouts(int connectTimeoutMs, int readTimeoutMs, int callTimeoutMs, int writeTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.callTimeoutMs = callTimeoutMs;
        this.writeTimeoutMs = writeTimeoutMs;
    }
}
