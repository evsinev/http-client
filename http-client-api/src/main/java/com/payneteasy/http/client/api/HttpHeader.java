package com.payneteasy.http.client.api;

import lombok.Data;
import lombok.NonNull;

@Data
public class HttpHeader {

    @NonNull
    private final String name;

    @NonNull
    private final String value;


    @Override
    public String toString() {
        return name + "=" + value;
    }
}
