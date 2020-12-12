package com.payneteasy.http.client.okhttp;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ProxyAuthenticator implements Authenticator {

    private final String username;
    private final String password;

    public ProxyAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public @Nullable Request authenticate(@Nullable Route aRoute, @NotNull Response aResponse) throws IOException {
        String credential = Credentials.basic(username, password);
        return aResponse.request().newBuilder()
                .header("Proxy-Authorization", credential)
                .build();
    }
}
