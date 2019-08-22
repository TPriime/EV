package com.prime.ev;

import java.io.IOException;

import com.neovisionaries.ws.client.*;



public class MWebSocket {
    private final String SERVER;
    private WebSocketAdapter adapter;

    public MWebSocket(String server, WebSocketAdapter wsa) {
        SERVER = server;
        adapter = wsa;
    }

    public WebSocket connect() throws WebSocketException, IOException {
        WebSocket ws = new WebSocketFactory()
                .setConnectionTimeout(5000)
                .createSocket(SERVER)
                .addListener(adapter)
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect();

        return ws;
    }
}
