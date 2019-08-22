package com.prime.ev;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.application.Platform;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Collectors;



public class Factory {

    static JSONObject factoryObject;
    static String PROPERTY_FILE = "device_properties.json";
    static boolean newMessage = false;
    static String serverResponse = "";
    static final String SERVER = "http://127.0.0.1:8080";
    static final String WS_SERVER = "ws://127.0.0.1:8080";
    static final String ELECTION_DATA_API = SERVER + "/api/election_data";

    private static final long MAX_CONNECTION_DELAY_MILLIS = 30000;

    private static WebSocket webSocket;

    static {
        try{
            factoryObject = (JSONObject) new JSONParser()
                    .parse(new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream(PROPERTY_FILE)))
                            .lines().collect(Collectors.joining()));
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public static String getProperty(String property) {
        return (String) factoryObject.get(property);
    }


    public static void setProperty(String property, String value){
        factoryObject.put(property, value);
    }


    public static String fetchElectionData() throws IOException {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(ELECTION_DATA_API).openConnection();
            String response;
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                response = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                        .lines().collect(Collectors.joining());
                System.out.println("server response:" + response);
                return response;
            }
            else if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
                /*@debug*/System.out.println("error code: "+ conn.getResponseCode());
            }
            else {
                /*@debug*/System.out.println("no server response");
            }
        } catch (ConnectException ce){ ce.printStackTrace();}
        /*@debug*/Platform.exit();
        return defaultServerResponse();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String defaultServerResponse(){
        System.out.println("couldn't reach server");
        return new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream("ElectionData.json")))
                .lines().collect(Collectors.joining());
    }



    public static boolean createSocketConnection() throws WebSocketException, IOException{
        webSocket = new MWebSocket(WS_SERVER, new WebSocketAdapter(){
            @Override public void onTextMessage(WebSocket webSocket, String message){
                incomingMessage(message);
            }
        }).connect();

        //reconnect
        new Thread(()->{
            while(true) {
                try {
                    Thread.sleep(200);
                    if (!webSocket.isOpen())
                        webSocket.recreate();
                } catch(Exception e){e.printStackTrace();}
            }
        }, "Socket Reconnection Thread").start();
        if(webSocket.isOpen()) return true;
        else return false;
    }



    public static void incomingMessage(String message){
        newMessage = true;
        serverResponse = Crypto.decrypt(message);
        System.out.println(serverResponse);
        /*@debug*/ MessageIntent msi = new Gson().fromJson(serverResponse, MessageIntent.class);
        System.out.println("title: "+ msi.header.get("title"));
        //if(msi.header.get("title").equals("id"))
            loginToServer();
    }


    private static void loginToServer(){
        MessageIntent msi = new MessageIntent(
                "LOGIN",
                "id",
                null,
                getProperty("id")
        );
        sendMessage(msi);
    }



    private static void sendMessage(MessageIntent msi){
        /*@debug*/System.out.println("sending: "+ new Gson().toJson(msi));
        webSocket.sendText(Crypto.encrypt(new Gson().toJson(msi)));
    }




    public static String fetchUserData() throws FileNotFoundException {
        try{Thread.sleep(1000);}catch (Exception e){}
        /*return new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream("UserData.json")))
                .lines().collect(Collectors.joining());
        */

        System.out.println("fetching user data");
        String response = awaitResponse(new MessageIntent("GET", "USER_DATA", null, null), MAX_CONNECTION_DELAY_MILLIS);
        return response;
    }


    private static String awaitResponse(MessageIntent msi, long duration){
        newMessage = false;
        sendMessage(msi);
        long startTime = System.currentTimeMillis();
        while(!newMessage){
            try{Thread.sleep(200);}catch(Exception e){e.printStackTrace();}
            if((System.currentTimeMillis()-startTime) > duration)
                return null;
        }
        return serverResponse;
    }



    public static String getFingerprint(){
        return "";
    }


    public static void sendAndRecordVote(String voteInstance){
        //nothing for now
        MessageIntent voteIntent = new MessageIntent();
    }


    public static void waitFor(long millis){
        long startTime = System.currentTimeMillis();
        while(true){
            if(System.currentTimeMillis()-startTime >= millis) return;
        }
    }
}
