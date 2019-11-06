package com.prime.ev.scene;

import com.google.gson.Gson;
import com.prime.ev.DisplayAccessor;
import com.prime.ev.Factory;
import com.prime.ev.Result;
import com.prime.ev.VoteData;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller {

    public TextField passwordField;
    public Label warningLabel;
    public ListView partyList;


    public void nextScreen(){
        DisplayAccessor.nextScene();
    }

    public void resetScene(){
        //if(DisplayAccessor.inFinalScenes()) DisplayAccessor.setScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE);
        //else DisplayAccessor.setScene(DisplayAccessor.NEW_VOTER_SCENE);
        DisplayAccessor.resetScene();
    }

    public void validatePassword(){
        if(passwordField.getText().equals(Factory.getProperty("password"))) {
            warningLabel.setVisible(false);
            nextScreen(); return;
        }
        passwordField.clear();
        passwordField.requestFocus();
        warningLabel.setVisible(true);
    }

    public void choose(){
        partyList.getItems().forEach((child)->
            ((Parent)child).lookup("#fingerPrintBox").setVisible(false));
        Parent parent = (Parent) partyList.getSelectionModel().getSelectedItem();
        parent.lookup("#fingerPrintBox").setVisible(true);
    }


    public void exitVoter(){
        DisplayAccessor.setScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE);
    }


    public void castVote(){
        DisplayAccessor.getCurrentScene().lookup("#retry")
                .setVisible(false);
        DisplayAccessor.castVote();
    }

    public void endElection(){
        try {
            DisplayAccessor.killSceneThreads();//try to interrupt, doesn't work yet
////////////////////////////////////////////////////////////////////////////use Factory.VOTE_LOG_PATH//////////////////////////////////////////
            BufferedReader reader = new BufferedReader(new FileReader(new File("vote_log.txt")));//Factory.VOTE_LOG_PATH)));
            List<String> voteLog = reader.lines().collect(Collectors.toList());

            try{
                Factory.presidentialVoteCount = Result.computePresidentialCount(voteLog.stream());
            } catch (NullPointerException npe){npe.printStackTrace();}

            try{
                Factory.voteSummary = Result.summary(voteLog.stream());
            }catch(ArrayIndexOutOfBoundsException exception){exception.printStackTrace();}

            System.out.println(Factory.presidentialVoteCount);
            System.out.println(Factory.voteSummary);

            DisplayAccessor.setResultScene();

        } catch(Exception e){e.printStackTrace();}
    }
}
