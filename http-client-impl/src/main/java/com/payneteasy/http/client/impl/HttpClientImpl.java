package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.*;
import com.payneteasy.http.client.api.exceptions.HttpConnectException;
import com.payneteasy.http.client.api.exceptions.HttpReadException;
import com.payneteasy.http.client.api.exceptions.HttpWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpClientImpl implements IHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientImpl.class);


    @Override
    public HttpResponse send(HttpRequest aRequest, HttpTimeouts aTimeouts) throws HttpConnectException, HttpReadException, HttpWriteException {
        String            url        = aRequest.getUrl();
        HttpURLConnection connection = createConnection(url, aRequest.getMethod(), aTimeouts);

        sendHeaders(connection, aRequest.getHeaders());
        sendBody(url, connection, aRequest.getBody());

        return parseResponse(url, connection);
    }

    private HttpResponse parseResponse(String aUrl, HttpURLConnection aConnection) throws HttpReadException {
        int              statusCode = waitForStatusCode(aUrl, aConnection);

        String reasonPhrase;
        try {
            reasonPhrase = aConnection.getResponseMessage();
        } catch (IOException e) {
            throw new HttpReadException("Cannot read reason phrase for url " + aUrl, e);
        }

        List<HttpHeader> headers = readHeaders(aConnection);
        byte[]           body    = readMessageBody(aUrl, statusCode, aConnection);

        return new HttpResponse(statusCode, reasonPhrase, headers, body);
    }

    private int waitForStatusCode(String aUrl, HttpURLConnection aConnection) throws HttpReadException {
        LOG.debug("Waiting for response code for {} ...", aUrl);
        int statusCode;
        try {
            statusCode = aConnection.getResponseCode();
        } catch (IOException e) {
            throw new HttpReadException("Cannot wait for response code for url " + aUrl, e);
        }
        return statusCode;
    }

    private byte[] readMessageBody(String aUrl, int aStatusCode, HttpURLConnection aConnection) throws HttpReadException {
        InputStream inputStream;
        try {
            inputStream = aStatusCode >= 400 ? aConnection.getErrorStream() : aConnection.getInputStream();
        } catch (IOException e) {
            throw new HttpReadException("Cannot create input stream for url " + aUrl, e);
        }

        int length = aConnection.getContentLength();

        if(length == -1) {
            return new byte[0];
        }
        
        try {
            return readFully(inputStream, length);
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
        if(aRequestBody == null) {
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

    private HttpURLConnection createConnection(String aUrl, HttpMethod aMethod, HttpTimeouts aTimeouts) throws HttpConnectException {
        URL url;
        try {
            url = new URL(aUrl);
        } catch (MalformedURLException e) {
            throw new HttpConnectException("Cannot parse url: " + aUrl, e);
        }

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new HttpConnectException("Cannot open connection to " + aUrl, e);
        }

        connection.setConnectTimeout(aTimeouts.getConnectTimeoutMs());
        connection.setReadTimeout(aTimeouts.getReadTimeoutMs());

        try {
            connection.setRequestMethod(aMethod.name());
        } catch (ProtocolException e) {
            throw new HttpConnectException("Cannot set request method " + aMethod + " for url " + aUrl, e);
        }

        return connection;
    }

    public static byte[] readFully(InputStream aInputStream, int length) throws IOException {
        if(length <= 0) {
            return new byte[] {};
        }

        byte[] buffer = new byte[length];
        int    count  = 0;

        do {
            int read = aInputStream.read(buffer, count, length - count);
            if (read < 0) {
                throw new IOException("Read only " + count + " but wanted " + length + " (-1)");
            }
            count += read;
        } while (count < length);

        return buffer;
    }


}
