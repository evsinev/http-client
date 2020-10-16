package com.payneteasy.http.client.api;

import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

public interface IHttpClient {

    HttpResponse send(HttpRequest aRequest, HttpRequestParameters aRequestParameters) throws HttpConnectException, HttpReadException, HttpWriteException;

}
