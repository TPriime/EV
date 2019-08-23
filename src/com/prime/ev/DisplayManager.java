package com.prime.ev;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.deploy.security.MSCredentialManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


/**
 * Created by Prime on 8/4/2019.
 */
public class DisplayManager {
    private static Stage primaryStage;
    private static ArrayList<Scene> sceneList;
    private final String SCENE_NAME_FORMAT = "scene/scene";

    protected final int DELAY_MILLIS = 2000;
    private final SceneFunction sceneFunction;
    public static int addedScenes = -1;

    protected boolean inFinalScenes = false;

    DisplayManager(Stage primaryStage) throws  Exception{
        this.primaryStage = primaryStage;
        sceneList = new ArrayList<Scene>();
        sceneFunction = new SceneFunction(this);

        new Thread(()-> initializeAndStartFirstScenes(), "Initialize First Scenes").start();
    }



    private void initializeAndStartFirstScenes() {
        setSceneFromIndex(1, DisplayAccessor.NEW_VOTER_SCENE);
        setScene(sceneList.get(0)); //start first scene
    }



    private void setSceneFromIndex(int fromIndex, int toIndex){
        for(int i=fromIndex; i<=toIndex; i++){
            try {
                URL fxml_url = getClass().getResource(SCENE_NAME_FORMAT + i + ".fxml");
                if (fxml_url == null) break;
                Scene scene = new Scene(FXMLLoader.load(fxml_url), DisplayAccessor.SCREEN_WIDTH, DisplayAccessor.SCREEN_HEIGHT);
                scene.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());
                sceneList.add(scene);
            }catch(IOException e){e.printStackTrace();}
        }
    }



    protected int initializeVoterScenes(ArrayList<ElectionData> electionBundle, Map<String, String> userDetails) throws IOException{
        URL fxml_url = getClass().getResource("scene/scene5.fxml");
        int numberOfVoterScenes = 0;

        //backup and restore true scene3 as first scene either as sceneIndex 3(before final scenes)
        // or sceneIndex 1 after final scenes
        if(inFinalScenes){
            Scene scene3 = getScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE); //as sceneIndex 1
            sceneList = new ArrayList<>();
            sceneList.add(scene3);
        } else{
            Scene scene3 = getScene(DisplayAccessor.NEW_VOTER_SCENE); //as sceneIndex 3
            sceneList = new ArrayList<>();
            sceneList.add(scene3);
        }

        //set scene 4 user details
        Parent parent4 = FXMLLoader.load(getClass().getResource("scene/scene4.fxml"));
        Scene scene4 = new Scene(parent4, DisplayAccessor.SCREEN_WIDTH, DisplayAccessor.SCREEN_HEIGHT);
        scene4.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());

        userDetails.forEach((data, value)->{
            try{
                ((Label) scene4.lookup("#"+data)).setText(":    "+value);
            }catch(NullPointerException npe){
                System.out.println("no "+data+" field found on scene");
            }
        });
        Image userImage = new Image(DisplayAccessor.RESOURCES+"/image.jpg");
        ((ImageView) scene4.lookup("#userImage")).setImage(userImage);

        ((ImageView) scene4.lookup("#userImage")).setClip(new Circle(256, 256,256));

        sceneList.add(scene4);


        //set the screened vote scenes for the voter
        for(ElectionData electionData: electionBundle){
            if(isVoterEligible(electionData, userDetails)) {
                Parent parent = FXMLLoader.load(fxml_url);
                Scene scene = new Scene(parent, DisplayAccessor.SCREEN_WIDTH, DisplayAccessor.SCREEN_HEIGHT);
                scene.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());

                ((Label) scene.lookup("#electionTitle")).setText(electionData.getTitle());
                ListView listView = ((ListView) scene.lookup("#partyList"));
                listView.setItems(FXCollections.observableArrayList(wrapInView(electionData.getPartyList())));

                sceneList.add(scene);
                ++numberOfVoterScenes;
            }
        }

        //set the final last 2 scenes
        setSceneFromIndex(6, 7);

        return numberOfVoterScenes;
    }


    private class isEligible{
        private boolean value;
        isEligible(boolean b){value = b;}
        boolean getValue(){return value;}
        void setValue(boolean b){value = b;}
    }

    private boolean isVoterEligible(ElectionData electionData, Map<String, String> userDetails){
        final isEligible eligible = new isEligible(true);
        electionData.getCriteria().forEach((criteria, value)->{
            if(criteria.equals("age")){
                //do some calc in check
                /*@debug*/System.out.println("checking age restriction");
            }
            else if(!userDetails.get(criteria).equalsIgnoreCase(value)) eligible.setValue(false);
        });
        return eligible.getValue();
    }


    private ArrayList<StackPane> wrapInView(ArrayList<String> partyNameList) throws IOException{
        ArrayList<StackPane> sPanes = new ArrayList<>();
        for(String partyName: partyNameList){
            StackPane sPane = FXMLLoader.load(getClass().getResource("customfx/party_box.fxml"));
            ((Label) sPane.lookup("#party_name")).setText(partyName);

            Image partyLogo = null;
            try{ partyLogo = new Image(DisplayAccessor.RESOURCES+"/logo/"+partyName+".jpg"); }
            catch(IllegalArgumentException i){
                partyLogo = new Image(DisplayAccessor.RESOURCES+"/logo/default.jpg");
            }
            ((ImageView) sPane.lookup("#party_logo")).setImage(partyLogo);
            sPanes.add(sPane);
        }
        return sPanes;
    }



    /*
     * Runs the inner scenes
     * Note: i corresponds to scenex.i.fxml
     */
    private void playScene(int scene_no) {
        /*@debug*/System.out.println("\nplayScene invoked with scene number: "+scene_no);

        if(inFinalScenes) return; ///for now

        new Thread(()->{
            try{
                for(int i=1; ; i++){
                    Thread.sleep(DELAY_MILLIS);
                    URL fxml_url = getClass().getResource(SCENE_NAME_FORMAT+scene_no+"."+i+".fxml");
                    /*@debug*/System.out.println("searched resource: "+SCENE_NAME_FORMAT+scene_no+"."+i+".fxml");
                    if(fxml_url == null) {
                        /*@debug*/System.out.println("resource not found"); break;
                    }
                    /*@debug*/System.out.println("found "+fxml_url.toExternalForm());
                    setRoot(fxml_url, i);
                }
            } catch(Exception e){ e.printStackTrace(); }
        }, "Play Scenes").start();
    }


    protected void setScene(Scene scene) {
        Platform.runLater(()->{
            primaryStage.setScene(scene);
            try{
                playScene(indexOfScene(getCurrentScene()));
                invokeSceneFunction(indexOfScene(getCurrentScene()));
            } // +1 to get actual file index
            catch(Exception e){e.printStackTrace();}
        });
    }


    protected void setScene(int sceneConstant){
        switch (sceneConstant){
            case DisplayAccessor.ANOTHER_NEW_VOTER_SCENE:
                setScene(getScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE)); break;
            case DisplayAccessor.NEW_VOTER_SCENE:
                setScene(getScene(DisplayAccessor.NEW_VOTER_SCENE)); break;
            case DisplayAccessor.USER_DETAILS_ERROR_SCENE:
                break;
        }
    }



    protected Scene getCurrentScene(){return primaryStage.getScene();}

    protected Scene getScene(int sceneIndex){
        return sceneList.get(sceneIndex-1);
    }

    protected int indexOfScene(Scene scene) {
        int index = sceneList.indexOf(scene);
        index = index<0 ? index : index+1;
        return index;
    }


    private void setRoot(URL url, int rootNumber) {
        Platform.runLater(()->{
            try{
                getCurrentScene().setRoot(FXMLLoader.load(url));
                invokeRootFunction(rootNumber);
            } catch(Exception e){e.printStackTrace();}
        });
    }


    public void nextScene() {
        /*@debug*/System.out.println("\nnextScene invoked");
        int oldSceneIndex = indexOfScene(getCurrentScene());
        int newSceneIndex = oldSceneIndex + 1;
        /*@debug*/System.out.println("oldSceneIndex in nextScene: "+oldSceneIndex+"; new: "+newSceneIndex);
        if(newSceneIndex >= 1 && newSceneIndex <= sceneList.size()) //range(1 - sceneCount)
            setScene(sceneList.get(newSceneIndex-1)); //actual
    }


    public void prevScene() {
        int currentSceneIndex = sceneList.indexOf(getCurrentScene());
        if(currentSceneIndex > 1)
            setScene(sceneList.get(--currentSceneIndex));
        else /*@debug*/System.out.println("no prev scene");
    }


    protected void invokeSceneFunction(int sceneIndex){
        /*@debug*/System.out.println("\ninvoked scene function with index: "+sceneIndex);

        if(inFinalScenes){
            if(sceneIndex == sceneList.size()-1)
                sceneFunction.castVote(trimScenesToElect(sceneList));
            if(sceneIndex == sceneList.size())
                sceneFunction.newVote();
        }

        switch(sceneIndex) {
            case DisplayAccessor.ANOTHER_NEW_VOTER_SCENE:
                if(!inFinalScenes) break;
                new Thread(()->{ try {
                    if(!sceneFunction.fetchUserDetails()) return; //loop till it returns true
                    Map<String, String> userDetails = sceneFunction.getUserDetailsMap();
                    initializeVoterScenes(sceneFunction.getElectionBundle(), userDetails);
                    DisplayAccessor.nextScene();
                } catch(Exception e){e.printStackTrace();}
                }, "Scene3 - Fetch Voter Details").start();
                break;

            case DisplayAccessor.NEW_VOTER_SCENE:
                if(inFinalScenes) break;
                //inFinalScenes = true;
                new Thread(()->{ try {
                    if(!sceneFunction.fetchUserDetails()) return; //loop till it returns true
                    Map<String, String> userDetails = sceneFunction.getUserDetailsMap();
                    initializeVoterScenes(sceneFunction.getElectionBundle(), userDetails);
                    inFinalScenes = true;
                    DisplayAccessor.nextScene();
                } catch(Exception e){e.printStackTrace();}
                }, "Scene3 - Fetch Voter Details").start();
                break;
            //case DisplayAccessor.USER_DETAILS_ERROR_SCENE:
            //    sceneFunction.userDetailError(); break;
        }
    }


    //Note: sceneX.rootIndex.fxml
    protected void invokeRootFunction(int rootIndex) {
        switch(rootIndex){

            //to do /////////////////bbbbbbbbbbbbbbbbuuuuuuuuuuuuuuuuuuugggggggggggggggggggggssssssssssssssssssss
            //set outter criteria for the specific scenes by collecting and checking
            //scene number for particular root number

            case DisplayAccessor.FETCH_RESOURCES_ROOT:
                new Thread(()->{
                    try{
                        sceneFunction.fetchElectionBundle();
                        sceneFunction.showStartStatus(sceneFunction.createSocketConnection());
                    }
                    catch(Exception e){e.printStackTrace();}
                }, "Fetch Election Resource").start();
                break;
        }
    }


    private List<Scene> trimScenesToElect(ArrayList<Scene> scenes){
        return scenes.subList(DisplayAccessor.FINAL_VOTE_BEGIN_SCENE-1, sceneList.size()-2);
    }
}
