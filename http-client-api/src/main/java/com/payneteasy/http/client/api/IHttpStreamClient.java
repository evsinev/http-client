package com.payneteasy.http.client.api;

import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

public interface IHttpStreamClient {

    void send(HttpRequest aRequest, HttpRequestParameters aRequestParameters, IHttpStreamResponseListener aListener) throws HttpConnectException, HttpReadException, HttpWriteException;

    IHttpStreamResponse send(HttpRequest aRequest, HttpRequestParameters aRequestParameters) throws HttpConnectException, HttpReadException, HttpWriteException;

}
