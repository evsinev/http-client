package com.payneteasy.http.client.impl;

import com.payneteasy.http.client.api.HttpProxyParameters;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class LocalThreadProxyAuthenticator extends Authenticator {

    private static final ThreadLocal<HttpProxyParameters> THREAD_LOCAL_PARAMETERS = new ThreadLocal<>();

    public static void setParameters(HttpProxyParameters aParameters) {
        THREAD_LOCAL_PARAMETERS.set(aParameters);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        HttpProxyParameters parameters = THREAD_LOCAL_PARAMETERS.get();
        if(parameters == null) {
            return null;
        }

        return new PasswordAuthentication(parameters.getProxyUsername(), parameters.getProxyPassword().toCharArray());
    }

    public static void clear() {
          THREAD_LOCAL_PARAMETERS.remove();
    }
}
