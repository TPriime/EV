package com.prime.ev;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.stream.Collectors;



public class Factory {

    private static JSONObject factoryObject;
    private static String PROPERTY_FILE = "device_properties.json";
    private static boolean newMessage = false;
    private static String serverResponse = "";
    private static String SERVER = "http://127.0.0.1:8080";
    private static String WS_SERVER = "ws://127.0.0.1:8080";
    private static String ELECTION_DATA_API = SERVER + "/api/election_data";

    static final int SERVER_DOWN = 0;
    static final int FETCH_ERROR = 1;
    static final int NO_FETCH = 2;
    static final int USER_FETCH_ERROR = 3;
    static final int INVALID_VOTER = 4;
    static final int FINGERPRINT_MISMATCH = 5;
    static final int INVALID_CARD = 6;

    private static long MAX_CONNECTION_DELAY_MILLIS = 3000;
    private static final String VOTE_LOG_PATH = "vote_log.txt";
    private static final String CONFIG_PATH = "config.properties";

    private static WebSocket webSocket;
    private static PrintWriter voteLogger;


    static {
        try{
            factoryObject = (JSONObject) new JSONParser()
                    .parse(new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream(PROPERTY_FILE)))
                            .lines().collect(Collectors.joining()));
        } catch(Exception e){ e.printStackTrace(); }

        File file = new File(VOTE_LOG_PATH);
        try {
            voteLogger = new PrintWriter(new FileWriter(
                    file, true)
                    , true);
        } catch(IOException ioe){ ioe.printStackTrace(); }

        try(InputStream in = new FileInputStream(CONFIG_PATH)){
            Properties prop = new Properties();
            prop.load(in);
            SERVER = prop.getProperty("server")==null ? "http://127.0.0.1:8080" : prop.getProperty("server");
            WS_SERVER = prop.getProperty("ws-server")==null ? "ws://127.0.0.1:8080" : prop.getProperty("ws-server");
            MAX_CONNECTION_DELAY_MILLIS = prop.getProperty("max-connection-delay")==null ?
                    3000 : Long.parseLong(prop.getProperty("max-connection-delay"));
            ELECTION_DATA_API = SERVER + "/api/election_data";
        } catch(IOException ioe){
            System.out.println("couldn't locate config.properties, falling back to default");
        } catch(NumberFormatException nfe){
            nfe.printStackTrace();
        }
    }



    public static String getProperty(String property) {
        return (String) factoryObject.get(property);
    }


    public static void setProperty(String property, String value){
        factoryObject.put(property, value);
    }


    static String fetchElectionData() throws IOException {
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

        return null;
    }




    static boolean createSocketConnection() throws WebSocketException, IOException{
         MWebSocket connector = new MWebSocket(WS_SERVER, new WebSocketAdapter(){
            @Override public void onTextMessage(WebSocket webSocket, String message){
                incomingMessage(message);
            }
        });
        webSocket = connector.connect();

        //reconnect
        new Thread(()->{
            while(true) {
                try {
                    Thread.sleep(500);
                    if (!webSocket.isOpen()) {
                        webSocket = connector.connect();
                    }
                } catch(Exception e){System.out.println("reconnecting...");}
            }
        }, "My Socket Reconnection Thread").start();

        return webSocket.isOpen();
    }



    private static void incomingMessage(String message){
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



    static String fetchUserData(String voterId) {
        try{Thread.sleep(1000);}catch (Exception e){}/////////////////////

        System.out.println("fetching user data");
        String response = awaitResponse(new MessageIntent("GET", "USER_DATA", null, voterId),
                MAX_CONNECTION_DELAY_MILLIS);
        return response;
    }


    static String readCard(){
        return "fa45689c";///////for now
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



    static String getFingerprint(){
        return "";
    }


    static void sendAndRecordVote(VoteData voteInstance){
        recordVote(voteInstance);  //log vote
        MessageIntent voteIntent = new MessageIntent("POST", "vote_data", null, voteInstance);
        sendMessage(voteIntent);
    }


    public static void waitFor(long millis){
        long startTime = System.currentTimeMillis();
        while(true){
            if(System.currentTimeMillis()-startTime >= millis) return;
        }
    }

    private static void recordVote(VoteData voteData){
        voteLogger.printf("\n\n%d: \n%s", 100, new Gson().toJson(voteData));
    }


    static boolean isCardPresent(){
        return true;
    }
}
