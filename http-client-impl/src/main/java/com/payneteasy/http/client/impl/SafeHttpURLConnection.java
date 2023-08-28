package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.HttpHeader;
import com.payneteasy.http.client.api.HttpHeaders;
import com.payneteasy.http.client.api.HttpTimeouts;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SafeHttpURLConnection {

    private static final Logger LOG = Logger.getLogger("http-client.HttpClientImpl");

    private final HttpURLConnection aConnection;

    public SafeHttpURLConnection(HttpURLConnection connection) {
        this.aConnection = connection;
    }

    public void sendHeaders(HttpHeaders aHeaders) {
        if (aHeaders == null) {
            return;
        }

        for (HttpHeader header : aHeaders.asList()) {
            aConnection.setRequestProperty(header.getName(), header.getValue());
        }
    }

    public void sendBody(String aUrl, byte[] aRequestBody) throws HttpWriteException {
        if(aRequestBody == null || aRequestBody.length == 0) {
            return;
        }

        aConnection.setDoOutput(true);
        OutputStream outputStream;
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


    public int waitForStatusCode(String aUrl, HttpTimeouts aTimeouts) throws HttpReadException, HttpConnectException {
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

    public String readReasonPhrase(String aUrl) throws HttpReadException {
        try {
            return aConnection.getResponseMessage();
        } catch (IOException e) {
            throw new HttpReadException("Cannot read reason phrase for url " + aUrl, e);
        }
    }


    public List<HttpHeader> readHeaders() {
        Map<String, List<String>> headerFields = aConnection.getHeaderFields();
        List<HttpHeader>          headers      = new ArrayList<>(headerFields.size());

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

    @Nonnull
    public InputStream getInputStream(String aUrl, int aStatusCode, List<HttpHeader> aHeaders) throws HttpReadException {
        try {
            InputStream inputStream = aStatusCode >= 400 ? aConnection.getErrorStream() : aConnection.getInputStream();
            if(inputStream != null && hasContent(aHeaders)) {
                return inputStream;
            } else {
                return new ByteArrayInputStream(new byte[0]);
            }
        } catch (IOException e) {
            throw new HttpReadException("Cannot create input stream for url " + aUrl, e);
        }
    }

    public void disconnect() {
        aConnection.disconnect();
    }

    private boolean hasContent(List<HttpHeader> aHeaders) {
        int length = aConnection.getContentLength();
        if(length > 0) {
            return true;
        }

        HttpHeaderFinder headerFinder = new HttpHeaderFinder(aHeaders);
        String transferEncoding       = headerFinder.get("Transfer-Encoding");

        if(transferEncoding == null) {
            return false;
        }

        return transferEncoding.contains("chunked");
    }
}
