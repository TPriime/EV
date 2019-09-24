package com.prime.ev;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class DisplayAccessor {
    private static DisplayManager displayManager;
    static final int SCREEN_WIDTH =  1280;
    static final int SCREEN_HEIGHT =  850;
    static final String RESOURCES = "resources";

    static final int FETCH_RESOURCES_ROOT = 1;
    public static final int ANOTHER_NEW_VOTER_SCENE = 1;
    public static final int NEW_VOTER_SCENE = 3;
    static final int USER_DETAILS_ERROR_SCENE = -1;

    static final int FINAL_VOTE_BEGIN_SCENE = 3;

    private static List<Thread> threadList = new ArrayList<>();


    DisplayAccessor(Stage primaryStage) throws Exception{
        displayManager = new DisplayManager(primaryStage);
    }

    public static void nextScene() {displayManager.nextScene();}
    public static void prevScene() {displayManager.prevScene();}
    //public static Scene getScene(int sceneIndex) {return displayManager.getScene(sceneIndex);}
    //public static int indexOfScene(Scene scene) {return displayManager.indexOfScene(scene);}
    public static void setScene(int sceneConstant){ displayManager.setScene(sceneConstant);}
    public static int indexOfScene(Scene scene) { return displayManager.indexOfScene(scene);}
    public static void resetScene(){setScene(indexOfScene(getCurrentScene()));}
    public static Scene getCurrentScene() {return displayManager.getCurrentScene();}
    static void invokeSceneFunction(int sceneIndex) {displayManager.invokeSceneFunction(sceneIndex);}
    static void invokeRootFunction(int rootIndex) {displayManager.invokeRootFunction(rootIndex);}


    public static boolean inFinalScenes() {return displayManager.inFinalScenes;}
    static long getDelay() {return displayManager.DELAY_MILLIS;}


    static void addSceneThread(Thread thread){ threadList.add(thread); }
    static void killSceneThreads(){ threadList.forEach(Thread::interrupt);}
}
