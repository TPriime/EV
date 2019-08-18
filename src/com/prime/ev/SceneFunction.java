package com.prime.ev;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SceneFunction {

    private ArrayList<ElectionData> electionBundle;
    private UserData currentUserData;


    protected SceneFunction(DisplayManager displayManager){
        //this.displayManager = displayManager;
    }


    protected ArrayList<ElectionData> getElectionBundle(){
        return (ArrayList<ElectionData>) electionBundle.clone();
    }



    protected void fetchUserDetails() throws FileNotFoundException{
        //wait until card is inserted
        currentUserData = new Gson().fromJson(Factory.fetchUserData(/*id*/), UserData.class);
    }


    protected Map<String, String> getUserDetailsMap() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject userData = (JSONObject) jsonParser.parse(Factory.fetchUserData());
        Map<String, String> userDataMap = new HashMap<>();

        //make them all in lowercase
        userData.forEach((userDetail, value)->
            userDataMap.put((String)userDetail, (String)value));

        return userDataMap;
    }


    protected void userDetailError(){
        //wait for voter to retract card
        /*@debug*/System.out.println("user detail error");
        DisplayAccessor.setScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE);
    }


    protected void fetchElectionBundle() throws IOException {
        /*@debug*/System.out.println("\nfetching vote data");

        Gson gson = new Gson();
        Type arrayListType = new TypeToken<ArrayList<ElectionData>>(){}.getType();
        electionBundle = gson.fromJson(Factory.fetchElectionData(), arrayListType);
    }


    public void showStartOption(){
        Platform.runLater(()->{
            DisplayAccessor.getCurrentScene().lookup("#loadingNode").setVisible(false);
            DisplayAccessor.getCurrentScene().lookup("#loadedNode").setVisible(true);
        });
    }


    public void castVote(List<Scene> voteScenes){
        //verify fingerPrint
        String fingerprint = Factory.getFingerprint();
        if(!fingerprint.equals(currentUserData.fingerprint)){
            userDetailError();
            return;
        }

        //at this point inFinalScene = true
        Map<String, String> voteMap = new HashMap<>();
        voteScenes.forEach(voteScene->
            ((ListView)voteScene.lookup("#partyList")).getItems().forEach(partyBox->{
                boolean isSelected = ((Parent) partyBox).lookup("#fingerPrintBox").isVisible();
                if(isSelected){
                    String electionTitle = ((Label)voteScene.lookup("#electionTitle")).getText();
                    String votedParty = ((Label)((Parent) partyBox).lookup("#party_name")).getText();
                    voteMap.put(electionTitle,votedParty);
                }
            }));
        Gson gson = new Gson();
        String voteInstance = gson.toJson(new VoteData(currentUserData.id,voteMap));

        Factory.sendAndRecordVote(voteInstance);

        new Thread(()->{
            try{Thread.sleep(2000);}catch(Exception e){}
            DisplayAccessor.nextScene();//////////////////////////////////////////////////////////////////
        }).start();
    }



    public void newVote() {
        new Thread(()->{
            try{Thread.sleep(2000);}catch(Exception e){}
            DisplayAccessor.setScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE);
        }).start();
    }
}
