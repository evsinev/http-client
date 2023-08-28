package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class HttpStreamClientImpl implements IHttpStreamClient {

    private static final Logger LOG = Logger.getLogger("http-client.HttpClientImpl");

    /**
     * Registers LocalThreadProxyAuthenticator
     *
     */
    public static void registerGlobalProxyAuthenticator() {
        System.getProperties().put("jdk.http.auth.tunneling.disabledSchemes", ""); // see https://bugs.openjdk.java.net/browse/JDK-8210814
        System.getProperties().put("sun.net.client.defaultReadTimeout"   , "60000");
        System.getProperties().put("sun.net.client.defaultConnectTimeout", "20000");
        Authenticator.setDefault(new LocalThreadProxyAuthenticator());
    }

    @Override
    public void send(HttpRequest aRequest, HttpRequestParameters aRequestParameters, IHttpStreamResponseListener aListener) throws HttpConnectException, HttpReadException, HttpWriteException {
        HttpProxyParameters proxyParameters = configureProxyParameters(aRequestParameters);
        try {
            String url = aRequest.getUrl();
            SafeHttpURLConnection connection = new SafeHttpURLConnection(
                    createConnection(url, aRequest.getMethod(), aRequestParameters)
            );

            connection.sendHeaders(aRequest.getHeaders());
            connection.sendBody(url, aRequest.getBody());

            parseListenerResponse(aListener, url, connection, aRequestParameters.getTimeouts());
        } finally {
            clearProxyParameters(proxyParameters);
        }
    }

    private static void clearProxyParameters(@Nullable HttpProxyParameters proxyParameters) {
        if(proxyParameters != null) {
            LocalThreadProxyAuthenticator.clear();
        }
    }

    @Nullable
    private static HttpProxyParameters configureProxyParameters(HttpRequestParameters aRequestParameters) {
        HttpProxyParameters proxyParameters = aRequestParameters.getProxyParameters();
        if(proxyParameters != null) {
            LocalThreadProxyAuthenticator.setParameters(proxyParameters);
        }
        return proxyParameters;
    }

    @Override
    public IHttpStreamResponse send(HttpRequest aRequest, HttpRequestParameters aRequestParameters) throws HttpConnectException, HttpReadException, HttpWriteException {
        HttpProxyParameters proxyParameters = configureProxyParameters(aRequestParameters);
        try {
            String                url        = aRequest.getUrl();
            SafeHttpURLConnection connection = new SafeHttpURLConnection(createConnection(url, aRequest.getMethod(), aRequestParameters));

            connection.sendHeaders(aRequest.getHeaders());
            connection.sendBody(url, aRequest.getBody());

            return parseListenerResponse(url, connection, aRequestParameters.getTimeouts());
        } finally {
            clearProxyParameters(proxyParameters);
        }
    }

    private void parseListenerResponse(IHttpStreamResponseListener aListener, String aUrl, SafeHttpURLConnection aConnection, HttpTimeouts aTimeouts) throws HttpReadException, HttpConnectException {
        int    statusCode   = aConnection.waitForStatusCode(aUrl, aTimeouts);
        String reasonPhrase = aConnection.readReasonPhrase(aUrl);

        aListener.onStatus(statusCode, reasonPhrase);

        List<HttpHeader> headers = aConnection.readHeaders();
        aListener.onHeaders(headers);

        readMessageBody(aListener, aUrl, statusCode, aConnection, headers);
    }

    private IHttpStreamResponse parseListenerResponse(String aUrl, SafeHttpURLConnection aConnection, HttpTimeouts aTimeouts) throws HttpReadException, HttpConnectException {
        int              statusCode   = aConnection.waitForStatusCode(aUrl, aTimeouts);
        String           reasonPhrase = aConnection.readReasonPhrase(aUrl);
        List<HttpHeader> headers      = aConnection.readHeaders();
        InputStream      inputStream  = aConnection.getInputStream(aUrl, statusCode, headers);

        return new HttpStreamResponseImpl(
                statusCode
                , reasonPhrase
                , headers
                , inputStream
                , aConnection
        );
    }

    private void readMessageBody(IHttpStreamResponseListener aListener, String aUrl, int aStatusCode, SafeHttpURLConnection aConnection, List<HttpHeader> aHeaders) throws HttpReadException {
        InputStream inputStream = aConnection.getInputStream(aUrl, aStatusCode, aHeaders);

        try {
            readAllBytes(aListener, inputStream);
        } catch (IOException e) {
            throw new HttpReadException("Cannot read message body from " + aUrl, e);
        }
    }

    private HttpURLConnection createConnection(String aUrl, HttpMethod aMethod, HttpRequestParameters aParameters) throws HttpConnectException {
        URL url;
        try {
            url = new URL(aUrl);
        } catch (MalformedURLException e) {
            throw new HttpConnectException("Cannot parse url: " + aUrl, e);
        }

        HttpURLConnection connection;
        try {
            connection = openConnection(aParameters, url);
        } catch (IOException e) {
            throw new HttpConnectException("Cannot open connection to " + aUrl, e);
        }

        connection.setConnectTimeout(aParameters.getTimeouts().getConnectTimeoutMs());
        connection.setReadTimeout(aParameters.getTimeouts().getReadTimeoutMs());

        try {
            connection.setRequestMethod(aMethod.name());
        } catch (ProtocolException e) {
            throw new HttpConnectException("Cannot set request method " + aMethod + " for url " + aUrl, e);
        }

        return connection;
    }

    private HttpURLConnection openConnection(HttpRequestParameters aParameters, URL url) throws IOException {
        HttpProxyParameters proxyParameters = aParameters.getProxyParameters();

        if(proxyParameters == null || proxyParameters.getProxy() == null) {
            return (HttpURLConnection) url.openConnection();
        }

        return (HttpURLConnection) url.openConnection(proxyParameters.getProxy());
    }

    public static void readAllBytes(IHttpStreamResponseListener aListener, InputStream aInputStream) throws IOException {
        byte[]       buffer = new byte[4096];
        int          count;

        while( (count = aInputStream.read(buffer)) >= 0) {
            aListener.onBytes(buffer, 0, count);
        }
    }


}
