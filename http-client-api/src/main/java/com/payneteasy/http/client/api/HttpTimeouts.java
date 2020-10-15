package com.payneteasy.http.client.api;

import lombok.Data;

@Data
public class HttpTimeouts {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

}
