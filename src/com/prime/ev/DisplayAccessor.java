package com.prime.ev;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DisplayAccessor {
    private static DisplayManager displayManager;
    public static final int SCREEN_WIDTH =  1280;
    public static final int SCREEN_HEIGHT =  800;
    public static final String RESOURCES = "resources";

    public static final int FETCH_RESOURCES_ROOT = 1;
    public static final int ANOTHER_NEW_VOTER_SCENE = 1;
    public static final int NEW_VOTER_SCENE = 3;
    public static final int USER_DETAILS_ERROR_SCENE = -1;

    public static final int FINAL_VOTE_BEGIN_SCENE = 3;



    protected DisplayAccessor(Stage primaryStage) throws Exception{
        displayManager = new DisplayManager(primaryStage);
    }

    public static void nextScene() {displayManager.nextScene();}
    public static void prevScene() {displayManager.prevScene();}
    public static Scene getScene(int sceneIndex) {return displayManager.getScene(sceneIndex);}
    public static int indexOfScene(Scene scene) {return displayManager.indexOfScene(scene);}
    public static void setScene(int sceneConstant){ displayManager.setScene(sceneConstant);}
    public static Scene getCurrentScene() {return displayManager.getCurrentScene();}
    public static void invokeSceneFunction(int sceneIndex) throws IOException {displayManager.invokeSceneFunction(sceneIndex);}
    public static void invokeRootFunction(int rootIndex) {displayManager.invokeRootFunction(rootIndex);}


    public static boolean inFinalScenes() {return displayManager.inFinalScenes;}
    public static long getDelay() {return displayManager.DELAY_MILLIS;}
}
