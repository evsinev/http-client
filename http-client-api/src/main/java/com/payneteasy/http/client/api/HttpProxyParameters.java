package com.payneteasy.http.client.api;

import lombok.Data;

import java.net.Proxy;

@Data
public class HttpProxyParameters {
    private final Proxy  proxy;
    private final String proxyUsername;
    private final String proxyPassword;
}
