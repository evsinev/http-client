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
        if (aResponse.request().header("Proxy-Authorization") != null) {
            return null; // Give up, we've already failed to authenticate.
        }

        String credential = Credentials.basic(username, password);

        for (Challenge challenge : aResponse.challenges()) {
            // If this is preemptive auth, use a preemptive credential.
            if (challenge.scheme().equalsIgnoreCase("OkHttp-Preemptive")) {
                return aResponse.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            }
        }

        return aResponse.request().newBuilder()
                .header("Proxy-Authorization", credential)
                .build();
    }
}
