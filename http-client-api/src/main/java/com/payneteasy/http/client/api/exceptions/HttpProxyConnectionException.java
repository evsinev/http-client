package com.payneteasy.http.client.api.exceptions;

public class HttpProxyConnectionException extends HttpConnectException {

    public HttpProxyConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
