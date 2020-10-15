package com.payneteasy.http.client.api;

import com.payneteasy.http.client.api.HttpRequest;
import com.payneteasy.http.client.api.HttpResponse;
import com.payneteasy.http.client.api.HttpTimeouts;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

public interface IHttpClient {

    HttpResponse send(HttpRequest aRequest, HttpTimeouts aTimeouts) throws HttpConnectException, HttpReadException, HttpWriteException;

}
