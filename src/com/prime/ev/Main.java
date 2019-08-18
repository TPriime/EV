package com.prime.ev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    private  DisplayAccessor displayAccessor;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Electronic Voting Project");
        //to prevent ugly startup
        Scene firstScene = new Scene(FXMLLoader.load(getClass().getResource("scene/scene1.fxml")),
                DisplayAccessor.SCREEN_WIDTH,
                DisplayAccessor.SCREEN_HEIGHT);
        firstScene.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());
        primaryStage.setScene(firstScene);
        displayAccessor = new DisplayAccessor(primaryStage);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
