package com.prime.ev;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class Main extends Application {

    private  DisplayAccessor displayAccessor;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("Electronic Voting Project");
        //to prevent ugly startup
        Scene firstScene = new Scene(FXMLLoader.load(getClass().getResource("scene/scene1.fxml")));
        firstScene.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());
        primaryStage.setScene(firstScene);
        displayAccessor = new DisplayAccessor(primaryStage);

        primaryStage.show();
        primaryStage.setOnCloseRequest(windowEvent->{
            System.out.println("\nshutting down...");
            Platform.exit();
            System.exit(0);
        });
        primaryStage.setHeight(DisplayAccessor.SCREEN_HEIGHT);
        primaryStage.setWidth(DisplayAccessor.SCREEN_WIDTH);
        primaryStage.setMaximized(true);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
