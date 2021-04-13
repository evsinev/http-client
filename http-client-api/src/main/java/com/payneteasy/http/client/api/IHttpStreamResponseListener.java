package com.payneteasy.http.client.api;

import java.util.List;

public interface IHttpStreamResponseListener {

    void onStatus(int aStatusCode, String aReasonPhrase);

    void onHeaders(List<HttpHeader> aHeaders);

    void onBytes(byte[] aBytes, int aOffset, int aCount);

}
