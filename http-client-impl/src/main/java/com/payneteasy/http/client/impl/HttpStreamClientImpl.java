package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
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
        HttpProxyParameters proxyParameters = aRequestParameters.getProxyParameters();
        if(proxyParameters != null) {
            LocalThreadProxyAuthenticator.setParameters(proxyParameters);
        }
        try {
            String            url        = aRequest.getUrl();
            HttpURLConnection connection = createConnection(url, aRequest.getMethod(), aRequestParameters);

            sendHeaders(connection, aRequest.getHeaders());
            sendBody(url, connection, aRequest.getBody());

            parseResponse(aListener, url, connection, aRequestParameters.getTimeouts());
        } finally {
            if(proxyParameters != null) {
                LocalThreadProxyAuthenticator.clear();
            }
        }
    }

    private void parseResponse(IHttpStreamResponseListener aListener, String aUrl, HttpURLConnection aConnection, HttpTimeouts aTimeouts) throws HttpReadException, HttpConnectException {
        int              statusCode = waitForStatusCode(aUrl, aConnection, aTimeouts);

        String reasonPhrase;
        try {
            reasonPhrase = aConnection.getResponseMessage();
        } catch (IOException e) {
            throw new HttpReadException("Cannot read reason phrase for url " + aUrl, e);
        }

        aListener.onStatus(statusCode, reasonPhrase);

        List<HttpHeader> headers = readHeaders(aConnection);
        aListener.onHeaders(headers);

        readMessageBody(aListener, aUrl, statusCode, aConnection, headers);
    }

    private int waitForStatusCode(String aUrl, HttpURLConnection aConnection, HttpTimeouts aTimeouts) throws HttpReadException, HttpConnectException {
        LOG.fine(String.format("Waiting for response code for %s with timeouts %s ...", aUrl, aTimeouts.toString()));
        int statusCode;
        try {
            statusCode = aConnection.getResponseCode();
        } catch (SSLHandshakeException e) {
            throw new HttpConnectException("Bad ssl certificate at " + aUrl, e);
        } catch (ConnectException e) {
            throw new HttpConnectException("Cannot connect to " + aUrl, e);
        } catch (IOException e) {
            throw new HttpReadException("Cannot wait for response code for url " + aUrl, e);
        }
        return statusCode;
    }

    private void readMessageBody(IHttpStreamResponseListener aListener, String aUrl, int aStatusCode, HttpURLConnection aConnection, List<HttpHeader> aHeaders) throws HttpReadException {
        InputStream inputStream;
        try {
            inputStream = aStatusCode >= 400 ? aConnection.getErrorStream() : aConnection.getInputStream();
        } catch (IOException e) {
            throw new HttpReadException("Cannot create input stream for url " + aUrl, e);
        }

        if(inputStream == null) {
            return;
        }
        
        int length = aConnection.getContentLength();
        if(length <= 0 ) {
            HttpHeaderFinder headerFinder = new HttpHeaderFinder(aHeaders);
            Optional<String> transferEncodingOption            = headerFinder.get("Transfer-Encoding");
            if(!transferEncodingOption.isPresent()) {
                return;
            }
            if(transferEncodingOption.get().contains("chunked")) {
                try {
                    readAllBytes(aListener, inputStream);
                } catch (IOException e) {
                    throw new HttpReadException("Cannot read chunked body from " + aUrl, e);
                }
            }

        }

        try {
            readAllBytes(aListener, inputStream);
        } catch (IOException e) {
            throw new HttpReadException("Cannot read message body from " + aUrl, e);
        }
    }


    private List<HttpHeader> readHeaders(HttpURLConnection aConnection) {
        Map<String, List<String>> headerFields = aConnection.getHeaderFields();
        List<HttpHeader> headers = new ArrayList<>(headerFields.size());

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if(entry == null) {
                continue;
            }

            String       name   = entry.getKey();
            if(name == null) {
                continue;
            }

            List<String> values = entry.getValue();
            if (values == null) {
                headers.add(new HttpHeader(name, ""));
                continue;
            }

            for (String value : values) {
                headers.add(new HttpHeader(name, value));
            }
        }

        return Collections.unmodifiableList(headers);
    }

    private void sendBody(String aUrl, HttpURLConnection aConnection, byte[] aRequestBody) throws HttpWriteException {
        if(aRequestBody == null || aRequestBody.length == 0) {
            return;
        }

        aConnection.setDoOutput(true);
        OutputStream outputStream = null;
        try {
            outputStream = aConnection.getOutputStream();
        } catch (IOException e) {
            throw new HttpWriteException("Cannot create output stream for url " + aUrl, e);
        }

        try {
            outputStream.write(aRequestBody);
        } catch (IOException e) {
            throw new HttpWriteException("Cannot create write body to url " + aUrl, e);
        }
    }

    private void sendHeaders(HttpURLConnection aConnection, HttpHeaders aHeaders) {
        if(aHeaders == null) {
            return;
        }

        for (HttpHeader header : aHeaders.asList()) {
            aConnection.setRequestProperty(header.getName(), header.getValue());
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
