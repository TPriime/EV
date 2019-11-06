package com.prime.ev;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.prime.util.cardio.CardIO;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.smartcardio.CardTerminal;
import java.io.*;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Factory {

    private static JSONObject factoryObject;
    private static String PROPERTY_FILE = "device_properties.json";
    private static boolean newMessage = false;
    private static long voteCount = 0;
    private static String serverResponse = "";
    private static String SERVER = "http://192.168.8.100:8080";//http://127.0.0.1:8080";
    private static String WS_SERVER = "ws://192.168.8.100:8080";//"ws://127.0.0.1:8080";
    //private static String ELECTION_DATA_API = SERVER + "/api/election_data";
    private static String ELECTION_DATA_API = SERVER + "/evoting_api/v1/elections/";

    static final int SERVER_DOWN = 0;
    static final int FETCH_ERROR = 1;
    static final int NO_FETCH = 2;
    static final int USER_FETCH_ERROR = 3;
    static final int INVALID_VOTER = 4;
    static final int FINGERPRINT_MISMATCH = 5;
    static final int INVALID_CARD = 6;
    static final int CARD_READ_ERR = 7;

    private static long MAX_CONNECTION_DELAY_MILLIS = 3000;
    private static final String CONFIG_PATH = "config.properties";

    private static WebSocket webSocket;
    private static PrintWriter voteLogger;

    private static final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwYXlsb2FkIjp7ImlkIjoiNWQ1ZTY0NjQzODdjODI3MmViNDdhNmEzIn0sImlhdCI6MTU2NjQ2NzI4NSwiZXhwIjoxNTY5MDU5Mjg1fQ.JNw0G7mcOHB1EJdEGfu8mdrrW-6-41SnloIy2sXWbPA";

    public static final String VOTE_LOG_PATH = "vote_log.txt";

    public static Map<String, Integer> voteSummary;
    public static List<Map.Entry<String, Integer>> presidentialVoteCount;


    static {
        try{
            factoryObject = (JSONObject) new JSONParser()
                    .parse(new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream(PROPERTY_FILE)))
                            .lines().collect(Collectors.joining()));
        } catch(Exception e){ e.printStackTrace(); }

        File file = new File(VOTE_LOG_PATH);
        try {
            voteLogger = new PrintWriter(new FileWriter(
                    file, false)
                    , true);
        } catch(IOException ioe){ ioe.printStackTrace(); }

        try(InputStream in = new FileInputStream(new File(CONFIG_PATH))){
            Properties props = new Properties();
            props.load(in);
            /*@debug*/System.out.println("loading properties from file...");

            DisplayAccessor.setScreenSize(
                    Integer.parseInt(props.getProperty("screen-width")),
                    Integer.parseInt(props.getProperty("screen-height")));

            DisplayAccessor.MAXIMIZE_SCREEN = Integer.parseInt(props.getProperty("screen-maximized"))==0 ? false:true;

            SERVER = props.getProperty("server")==null ? SERVER : props.getProperty("server");
            WS_SERVER = props.getProperty("ws-server")==null ? WS_SERVER : props.getProperty("ws-server");
            MAX_CONNECTION_DELAY_MILLIS = props.getProperty("max-connection-delay")==null ?
                    3000 : Long.parseLong(props.getProperty("max-connection-delay"));
            ELECTION_DATA_API = SERVER + "/api/election_data";
        } catch(IOException ioe){
            System.out.println("couldn't locate config.properties, falling back to default");
        } catch(NumberFormatException nfe){
            nfe.printStackTrace();
        }
    }


    private static void initCardReaderInterface(){
        CardIO.getInstance().addListener(new CardIO.ICardListener() {
            @Override
            public void onCardInserted(CardTerminal cardTerminal) { }

            @Override
            public void onCardEjected(CardTerminal cardTerminal) { }

            @Override
            public void onDeviceDetected(List<CardTerminal> list) { }

            @Override
            public void onDeviceDetached(CardTerminal cardTerminal) { }
        });
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
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-access-token", token);

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



    static String getElectionBundleForVoter(UserData userData) throws IOException {
        ////////////////////////////////////////////////////////////////////////////should get data via websocket connection
        System.out.println("fetching election data");
        String msgBody = userData.id + ";" + userData.lga;
        String response = awaitResponse(new MessageIntent("GET", "ELECTION_DATA", null, msgBody),
                MAX_CONNECTION_DELAY_MILLIS);
        return response;

        /*
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(ELECTION_DATA_API).openConnection();
            String response;
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-access-token", token);

            conn.setDoOutput(true);
            Map<String, String> req = new HashMap<>();
            req.put("", userData.id);
            //req.put("", "");
            conn.getOutputStream().write(new Gson().toJson(req).getBytes());

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                response = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                        .lines().collect(Collectors.joining());
                System.out.println("server response:" + response);
                return response;
            }
            else if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
                /*@debug/System.out.println("error code: "+ conn.getResponseCode()+"; error msg: "+ conn.getResponseMessage());
            }
            else {
                /*@debug/System.out.println("no server response");
            }
        } catch (ConnectException ce){ ce.printStackTrace();}

        return null;
        */
    }




    static boolean createSocketConnection() throws WebSocketException, IOException{
         MWebSocket connector = new MWebSocket(WS_SERVER, new WebSocketAdapter(){
            @Override public void onTextMessage(WebSocket webSocket, String message){
                incomingMessage(message);
            }
        });
        try{
            webSocket = connector.connect();
        } catch (WebSocketException wse){
            System.out.println(wse.getError()+" - "+wse.getMessage());
            return false;
        }

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
        //System.out.println(serverResponse);
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
                getProperty("device_id")
        );
        sendMessage(msi);
    }



    private static void sendMessage(MessageIntent msi){
        /*@debug*/System.out.println("sending: "+ new Gson().toJson(msi));
        webSocket.sendText(Crypto.encrypt(new Gson().toJson(msi)));
    }



    static String fetchUserData(String voterId) {
        System.out.println("fetching user data");
        String response = awaitResponse(new MessageIntent("GET", "USER_DATA", null, voterId),
                MAX_CONNECTION_DELAY_MILLIS);
        return response;
    }


    static String readCard(){
        String cardID = null;
        try{ cardID = _readCard(); }
        catch (IOException ioe){ ioe.printStackTrace(); }
        if(cardID.trim().equals("-1")) return null; //error flag
        return cardID; //"12345";//////////////////////////////////////////////////////////////////////////////
    }


    private static String _readCard() throws IOException{
        Process process = Runtime.getRuntime().exec(new String[]{"python", "read_card.py"});
        BufferedInputStream buff  = new BufferedInputStream(process.getInputStream());
        byte[] output = new byte[20];
        buff.read(output);
        return new String(output).trim();
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


    static boolean matchFingerprint(String fingerprints){
        final List<Integer> result = new ArrayList<>();

        List<List<String>> fprints = new Gson().fromJson(fingerprints, new TypeToken<List<List<String>>>(){}.getType());
        fprints.stream().map(list->list.toString())
                .forEach(fprint->{
                    try{
                        result.add(_matchFingerprint(fprint));
                    }catch(Exception e){e.printStackTrace();}
                });

        int score = result.stream().max(Integer::compare).get();
        /*@debug*/System.out.printf("match score: %d\n", score);//////////////////////////////////////////
        return score>50;
    }


    private static int _matchFingerprint(String fingerprint) throws IOException{
        Process process = Runtime.getRuntime().exec(new String[]{"python", "match_fingerprint.py", fingerprint});
        BufferedInputStream buff  = new BufferedInputStream(process.getInputStream());
        byte[] output = new byte[50];
        buff.read(output);
        return Integer.parseInt(new String(output).trim());
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
        voteLogger.printf("%d-%s\n", ++voteCount, new Gson().toJson(voteData));
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //MAJOR WORK RIGHT NOW
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //run another python method for this
    static boolean isCardPresent() throws IOException {
        byte[] output = new byte[1];
        Process process = Runtime.getRuntime().exec(new String[]{"python", "detect_card.py"});
        BufferedInputStream buff  = new BufferedInputStream(process.getInputStream());
        buff.read(output);

        return new String(output).equals("1") ? true:false;
    }
}
