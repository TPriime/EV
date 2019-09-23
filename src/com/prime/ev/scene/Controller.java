package com.prime.ev.scene;

import com.prime.ev.DisplayAccessor;
import com.prime.ev.Factory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class Controller {

    public TextField passwordField;
    public Label warningLabel;
    public ListView partyList;


    public void nextScreen(){
        DisplayAccessor.nextScene();
    }

    public void resetScene(){
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
}
