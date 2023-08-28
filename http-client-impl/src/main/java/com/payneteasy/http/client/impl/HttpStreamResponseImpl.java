package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.HttpHeader;
import com.payneteasy.http.client.api.IHttpStreamResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class HttpStreamResponseImpl implements IHttpStreamResponse {

    private final int                   statusCode;
    private final String                reasonPhrase;
    private final List<HttpHeader>      headers;
    private final InputStream           inputStream;
    private final SafeHttpURLConnection connection;

    public HttpStreamResponseImpl(int statusCode, String reasonPhrase, List<HttpHeader> headers, InputStream inputStream, SafeHttpURLConnection connection) {
        this.statusCode   = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers      = headers;
        this.inputStream  = inputStream;
        this.connection   = connection;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    @Nonnull
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    @Nonnull
    public List<HttpHeader> getHeaders() {
        return headers;
    }

    @Override
    @Nonnull
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public void close() {
        connection.disconnect();
    }
}
