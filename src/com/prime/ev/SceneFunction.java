package com.prime.ev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Type;
import java.rmi.server.ExportException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SceneFunction {

    private ArrayList<ElectionData> electionBundle;
    private UserData currentUserData;
    private String currentRawUserData;
    private final long MAX_CONNECTION_DELAY_MILLIS = 6000;



    ArrayList<ElectionData> getElectionBundle() throws IOException {
        Gson gson = new Gson();
        MessageIntent msi = gson.fromJson(Factory.getElectionBundleForVoter(currentUserData), MessageIntent.class);
        Type arrayListType = new TypeToken<ArrayList<ElectionData>>(){}.getType();
        //must convert back to json since Gson already assumes the object data to be of type ArrayList
        //therefore it can longer be easily inferred as String
        String arrayListToJson = gson.toJson(msi.body.get("election_data"));
        return (ArrayList<ElectionData>) gson.fromJson(arrayListToJson, arrayListType);
    }



    boolean fetchUserDetails() {
        //set visibility
        Platform.runLater(()->{
            try {
                DisplayAccessor.getCurrentScene().lookup("#retryButton").setVisible(false);
                DisplayAccessor.getCurrentScene().lookup("#prompt").getStyleClass().remove("error-label");
                ((Label) DisplayAccessor.getCurrentScene().lookup("#prompt"))
                        .setText("insert your card");
                DisplayAccessor.getCurrentScene().lookup("#prompt").setVisible(true);
            } catch(NullPointerException npe){
                //voter may have suddenly removed card, but should not halt execution at this point
                // i.e. before runCardEjectListener() is invoked
                npe.printStackTrace();
            }
        });


        //wait until card is inserted
        try{while(!Factory.isCardPresent()) continue; }
        catch(IOException ioe){
            ioe.printStackTrace();
            userDetailError(Factory.CARD_READ_ERR); return false;
        }
        catch(Exception e){e.printStackTrace(); return  false;}


        Platform.runLater(() -> ((Label) DisplayAccessor.getCurrentScene().lookup("#prompt"))
                    .setText("fetching details..."));

        //read card
        String voterId = Factory.readCard();

        //card is bad, invalid or rejected
        if(voterId==null) {
            userDetailError(Factory.INVALID_CARD); //Factory.INVALID_CARD
            return false;
        }

        //begin  card read loop
        runCardEjectListener();

        //get server response
        String rawServerResponse = Factory.fetchUserData(voterId);  //long blocking operation
        //server error
        if(rawServerResponse==null) {
            connectionTimeOut(Factory.USER_FETCH_ERROR); return false;
        }

        MessageIntent msi = new Gson().fromJson(rawServerResponse, MessageIntent.class);

        //server returns "invalid voter"
        if(msi.header.get("title").equalsIgnoreCase("INVALID_VOTER")){
            userDetailError(Factory.INVALID_VOTER);
            return false;
        }

        currentRawUserData = new Gson().toJson(msi.body);
        currentUserData = new Gson().fromJson(currentRawUserData, UserData.class);

        return true;
    }


    Map<String, String> getUserDetailsMap() throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject userData = (JSONObject) jsonParser.parse(currentRawUserData);
        Map<String, String> userDataMap = new HashMap<>();

        userData.forEach((userDetail, value)->{
            if(!((String)userDetail).equalsIgnoreCase("image"))
                userDataMap.put((String)userDetail, (String)value);});

        return userDataMap;
    }

    UserData getUserData(){return currentUserData;}

    private void userDetailError(int cause){
        switch(cause){
            case Factory.CARD_READ_ERR: //this is very unlikely and is cause by an error in running the external python card reader codes
            case Factory.INVALID_CARD:
                Platform.runLater(()->{
                    ((Label) DisplayAccessor.getCurrentScene().lookup("#prompt"))
                            .setText("Invalid card. Retract!");
                    DisplayAccessor.getCurrentScene().lookup("#prompt")
                            .getStyleClass().add("error-label");
                }); break;

            case Factory.INVALID_VOTER:
                Platform.runLater(()->{
                    ((Label) DisplayAccessor.getCurrentScene().lookup("#prompt"))
                            .setText("No valid record!");
                    DisplayAccessor.getCurrentScene().lookup("#prompt")
                            .getStyleClass().add("error-label");
                }); break;

            case Factory.FINGERPRINT_MISMATCH:
                DisplayAccessor.getCurrentScene().lookup("#retry")
                        .setVisible(true); break;
                        ///////////////////////////////////////////////////////implement the ability to kick of user after some trials
        }

        /*@debug*/System.out.println("user detail error");
        /*
         * I think a function similar to connection timeout should replace
         * calling a new scene here.
         *
         * Also when card read function is implemented,
         * this should part of the code should be stripped since
         * the new voter scene will be invoked which will also
         * undo every change/error setting
         */

        //wait for voter to retract card, then undo color setting
        /*try{Thread.sleep(1500);}catch(Exception e){e.printStackTrace();}
        switch(cause){
            case Factory.INVALID_CARD:
            case Factory.CARD_READ_ERR:
            case Factory.INVALID_VOTER:
                DisplayAccessor.getCurrentScene().lookup("#prompt")
                        .getStyleClass().remove("error-label");
                int action = DisplayAccessor.inFinalScenes() ? DisplayAccessor.ANOTHER_NEW_VOTER_SCENE
                        : DisplayAccessor.NEW_VOTER_SCENE;
                DisplayAccessor.invokeSceneFunction(action);
                System.out.println("invoked "+action);//////////////////////////////////////////////////////////////////////////////
                break;
            case Factory.FINGERPRINT_MISMATCH:
                DisplayAccessor.getCurrentScene().lookup("#retry")
                        .setVisible(true); break;
        }*/
    }


    void fetchElectionBundle() throws IOException {
        /*@debug*/System.out.println("\nfetching vote data");

        Gson gson = new Gson();
        Type arrayListType = new TypeToken<ArrayList<ElectionData>>(){}.getType();
        electionBundle = gson.fromJson(Factory.fetchElectionData(), arrayListType);
        if(electionBundle == null) {
            connectionTimeOut(Factory.FETCH_ERROR);
            throw new NullPointerException("no election bundle");
        }
    }



    boolean createSocketConnection() throws Exception{
        long startTime = System.currentTimeMillis();
        while(!Factory.createSocketConnection())
            if((System.currentTimeMillis()-startTime) > MAX_CONNECTION_DELAY_MILLIS)
                return false;
        return true;
    }


    private void connectionTimeOut(int cause) {
        System.out.println("\n\nconnection timeout fetching user data");
        final String[] message = new String[1];
        switch(cause){
            case Factory.SERVER_DOWN: message[0] = "Servers are currently down";
                break;
            case Factory.FETCH_ERROR: message[0] = "Couldn't fetch election data";
                break;
            case Factory.NO_FETCH: message[0] = "No available elections";
                break;
            case Factory.USER_FETCH_ERROR: showUserDetailRetryOption();
                break;
        }

        if(message[0]!=null) Platform.runLater(()->{
            System.out.println("CONNECTION TIMEOUT: "+ message[0]);
            ((Label) DisplayAccessor.getCurrentScene().lookup("#fetchMessage"))
                    .setText(message[0]);
            Button retryButton = ((Button) DisplayAccessor.getCurrentScene().lookup("#serverRetryButton"));
            retryButton.setVisible(true);
            retryButton.setOnAction(e->DisplayAccessor.invokeRootFunction(DisplayAccessor.FETCH_RESOURCES_ROOT));
        });

    }


    void showStartStatus(boolean connected){
        if(connected) showStartOption();
        else connectionTimeOut(Factory.SERVER_DOWN);
    }

    private void showStartOption(){
        Platform.runLater(()->{
            ((Label) DisplayAccessor.getCurrentScene().lookup("#fetchMessage"))
                    .setText("Election resources fetched");
            DisplayAccessor.getCurrentScene().lookup("#serverRetryButton").setVisible(false);
            DisplayAccessor.getCurrentScene().lookup("#loadingNode").setVisible(false);
            new Thread(()->{
                try{Thread.sleep((long)(DisplayAccessor.getDelay()*0.35));}catch(Exception e){e.printStackTrace();}
                DisplayAccessor.getCurrentScene().lookup("#loadedNode").setVisible(true);
            }, "Fancy start delay").start();
        });
    }


    public void setElectionBundle(ArrayList<ElectionData> electionBundle) {
        this.electionBundle = electionBundle;
    }

    private void showUserDetailRetryOption(){
        Platform.runLater(()->{
            ((Label) (DisplayAccessor.getCurrentScene().lookup("#prompt")))
                    .setText("connection timeout");
            DisplayAccessor.getCurrentScene().lookup("#prompt")
                    .getStyleClass().add("error-label");
            Button retryButton = (Button) (DisplayAccessor.getCurrentScene().lookup("#retryButton"));
            retryButton.setVisible(true);
            int action = DisplayAccessor.inFinalScenes() ? DisplayAccessor.ANOTHER_NEW_VOTER_SCENE
                    : DisplayAccessor.NEW_VOTER_SCENE;
            retryButton.setOnAction(e-> DisplayAccessor.invokeSceneFunction(action));
        });
    }


    List<Map<String, String>> getVotes(List<Scene> voteScenes){
        List<Map<String, String>> votes = new ArrayList<>();
        voteScenes.forEach(voteScene->
                ((ListView)voteScene.lookup("#partyList")).getItems().forEach(partyBox->{
                    Map<String, String> voteMap = new HashMap<>();
                    boolean isSelected = ((Parent) partyBox).lookup("#fingerPrintBox").isVisible();
                    if(isSelected){
                        String electionTitle = ((Label)voteScene.lookup("#electionTitle")).getText()
                                .replace("(", "#").split("#")[0]; //remove the bracket containing lga
                        String votedParty = ((Label)((Parent) partyBox).lookup("#party_name")).getText();
                        voteMap.put("election", electionTitle);
                        voteMap.put("electionCode", DisplayAccessor.getCurrentElectionCodeMap().get(electionTitle));
                        voteMap.put("party", votedParty);
                        votes.add(voteMap);
                    }
                }));
        return votes;
    }



    void castVote(List<Scene> voteScenes){
        new Thread(()->
            _castVote(voteScenes)
        , "CastVote Thread").start();
    }

    void _castVote(List<Scene> voteScenes){
        //verify fingerPrint
        try{
            if(!Factory.matchFingerprint(currentUserData.fingerprint))
                throw new Exception("Exception in matching fingerprint");
        } catch(Exception e){
            e.printStackTrace();
            userDetailError(Factory.FINGERPRINT_MISMATCH);
            return;
        }

        //at this point inFinalScene = true
        /*
        List<Map<String, String>> votes = new ArrayList<>();
        voteScenes.forEach(voteScene->
            ((ListView)voteScene.lookup("#partyList")).getItems().forEach(partyBox->{
                Map<String, String> voteMap = new HashMap<>();
                boolean isSelected = ((Parent) partyBox).lookup("#fingerPrintBox").isVisible();
                if(isSelected){
                    String electionTitle = ((Label)voteScene.lookup("#electionTitle")).getText();
                    String votedParty = ((Label)((Parent) partyBox).lookup("#party_name")).getText();
                    voteMap.put("election", electionTitle);
                    voteMap.put("electionCode", DisplayAccessor.getCurrentElectionCodeMap().get(electionTitle));
                    voteMap.put("party", votedParty);
                    votes.add(voteMap);
                }
            }));
        */

        String voteTime = new Date(System.currentTimeMillis()).toGMTString();
        Factory.sendAndRecordVote(
                new VoteData(currentUserData.id, Factory.getProperty("device_id"), getVotes(voteScenes), voteTime));

        //new Thread(()->{
        try{Thread.sleep((long)(DisplayAccessor.getDelay()*1.8));}catch(Exception e){e.printStackTrace();}
        DisplayAccessor.nextScene();//////////////////////////////////////////////////////
        //}).start();
    }



    void newVote() {
        /*
         * when the card reading function is implemented,
         * this function should become invalid and stripped since
         * the checkForCardLoop() will wait for a retraction and move to
         * the new voter scene
         */



        //wait for user to remove card to start new vote/////////////////////////////////////////////////
        new Thread(()->{
            try{Thread.sleep(2000);}catch(Exception e){e.printStackTrace();}
            DisplayAccessor.setScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE);
        }, "New Vote").start();
    }


    //resets when card is removed
    private void runCardEjectListener(){
        Thread cardEjectThread = new Thread(()->{
            try{
                while(Factory.isCardPresent()) {
                    if(Thread.interrupted()) return;  //probably now in summary scene
                    continue; //loop till card is ejected
                }
                try{Thread.sleep(1300);}catch(Exception e){e.printStackTrace();}

                DisplayAccessor.killSceneThreads();
                int action = DisplayAccessor.inFinalScenes() ? DisplayAccessor.ANOTHER_NEW_VOTER_SCENE
                        : DisplayAccessor.NEW_VOTER_SCENE;
                DisplayAccessor.setScene(action);
            }catch(IOException ioe){ //thrown by Factory.isCardPresent()
                ioe.printStackTrace();
                userDetailError(Factory.CARD_READ_ERR);
            }
        }, "Check For Card Thread");

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //commented out for now because it immediately gets interrupted by an unknown source
        //instead of only by the Controller().endResult()
        //DisplayAccessor.addSceneThread(cardEjectThread);
        cardEjectThread.start();
    }
}
