package com.payneteasy.http.client.api;

import lombok.Builder;
import lombok.Data;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

@Data
@Builder
public class HttpRequestParameters {

    private final HttpTimeouts        timeouts;
    private final HttpProxyParameters proxyParameters;
    private final HostnameVerifier    hostnameVerifier;
    private final SSLSocketFactory    sslSocketFactory;
    private final X509TrustManager    trustManager;
}
