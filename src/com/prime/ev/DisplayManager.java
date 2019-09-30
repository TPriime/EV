package com.prime.ev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

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
    private Stage primaryStage;
    private static ArrayList<Scene> sceneList;
    private final String SCENE_NAME_FORMAT = "scene/scene";

    final int DELAY_MILLIS = 2000;
    private final SceneFunction sceneFunction;
    public static int addedScenes = -1;

    boolean inFinalScenes = false;

    private Map<String, String> currentElectionCodeMap;

    DisplayManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        sceneList = new ArrayList<>();
        currentElectionCodeMap = new HashMap<>();
        sceneFunction = new SceneFunction();

        new Thread(this::initializeAndStartFirstScenes, "Initialize First Scenes").start();
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
                Scene scene = new Scene(FXMLLoader.load(fxml_url));
                scene.getStylesheets().add(getClass().getResource("scene/scene_style.css").toExternalForm());
                sceneList.add(scene);
            }catch(IOException e){e.printStackTrace();}
        }
    }


    protected Map<String, String> getCurrentElectionCodeMap(){
        return currentElectionCodeMap;
    }


    private int initializeVoterScenes(ArrayList<ElectionData> electionBundle, Map<String, String> userDetails) throws IOException{
        if (electionBundle==null) throw new NullPointerException("electionBundle is null");

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

        UserData usd = sceneFunction.getUserData();
        ArrayList<Byte> imageByteList = (ArrayList<Byte>) usd.image.get("data");
        byte[] imageBytes = new byte[imageByteList.size()];
        Object[] bytes = imageByteList.toArray();
        for(int i=0; i<imageByteList.size(); i++) { imageBytes[i] = (byte)(double)(Double)bytes[i]; }

        InputStream i = new ByteArrayInputStream(imageBytes);
        Image userImage = new Image(i);


        ImageView imageView = (ImageView)scene4.lookup("#userImage");

        imageView.setFitWidth(512);
        imageView.setFitHeight(512);
        imageView.setPreserveRatio(false);
        imageView.setClip(new Circle(imageView.getFitWidth()/2, imageView.getFitHeight()/2,imageView.getFitWidth()/2));
        imageView.setImage(userImage);

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

                currentElectionCodeMap.put(electionData.getTitle(), electionData.getCode());
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

            Image partyLogo;
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


    private void setScene(Scene scene) {
        Platform.runLater(()->{
            primaryStage.setScene(scene);
            try{
                playScene(indexOfScene(getCurrentScene()));
                invokeSceneFunction(indexOfScene(getCurrentScene()));
            } // +1 to get actual file index
            catch(Exception e){e.printStackTrace();}
        });
    }


    void setScene(int sceneConstant){
        switch (sceneConstant){
            case DisplayAccessor.ANOTHER_NEW_VOTER_SCENE:
                setScene(getScene(DisplayAccessor.ANOTHER_NEW_VOTER_SCENE)); break;
            case DisplayAccessor.NEW_VOTER_SCENE:
                setScene(getScene(DisplayAccessor.NEW_VOTER_SCENE)); break;
            case DisplayAccessor.USER_DETAILS_ERROR_SCENE:
                break;
        }
    }



    Scene getCurrentScene(){return primaryStage.getScene();}

    private Scene getScene(int sceneIndex){
        return sceneList.get(sceneIndex-1);
    }

    int indexOfScene(Scene scene) {
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


    void nextScene() {
        /*@debug*/System.out.println("\nnextScene invoked");
        int oldSceneIndex = indexOfScene(getCurrentScene());
        int newSceneIndex = oldSceneIndex + 1;
        /*@debug*/System.out.println("oldSceneIndex in nextScene: "+oldSceneIndex+"; new: "+newSceneIndex);
        if(newSceneIndex >= 1 && newSceneIndex <= sceneList.size()) //range(1 - sceneCount)
            setScene(sceneList.get(newSceneIndex-1)); //actual
    }


    void prevScene() {
        int currentSceneIndex = sceneList.indexOf(getCurrentScene());
        if(currentSceneIndex > 1)
            setScene(sceneList.get(--currentSceneIndex));
        else /*@debug*/System.out.println("no prev scene");
    }


    void invokeSceneFunction(int sceneIndex){
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
                Thread scene1Thread = new Thread(()->{ try {
                    if(!sceneFunction.fetchUserDetails()) return; //loop till it returns true
                    Map<String, String> userDetails = sceneFunction.getUserDetailsMap();
                    initializeVoterScenes(sceneFunction.getElectionBundle(), userDetails);
                    DisplayAccessor.nextScene();
                }
                catch(Exception e){
                    serverResponseError();
                    e.printStackTrace();
                }
                }, "Scene1 - Fetch Voter Details");
                scene1Thread.start();
                DisplayAccessor.addSceneThread(scene1Thread);
                break;

            case DisplayAccessor.NEW_VOTER_SCENE:
                if(inFinalScenes) break;
                //inFinalScenes = true;
                Thread scene3Thread = new Thread(()->{ try {
                    if(!sceneFunction.fetchUserDetails()) return; //loop till it returns true
                    Map<String, String> userDetails = sceneFunction.getUserDetailsMap();
                    initializeVoterScenes(sceneFunction.getElectionBundle(), userDetails);
                    inFinalScenes = true;
                    DisplayAccessor.nextScene();
                }
                catch(Exception e){
                    serverResponseError();
                    e.printStackTrace();
                }
                }, "Scene3 - Fetch Voter Details");
                scene3Thread.start();
                DisplayAccessor.addSceneThread(scene3Thread);
                break;
            //case DisplayAccessor.USER_DETAILS_ERROR_SCENE:
            //    sceneFunction.userDetailError(); break;
        }
    }


    //Note: sceneX.rootIndex.fxml
    void invokeRootFunction(int rootIndex) {
        switch(rootIndex){

            //to do /////////////////bbbbbbbbbbbbbbbbuuuuuuuuuuuuuuuuuuugggggggggggggggggggggssssssssssssssssssss
            //set outter criteria for the specific scenes by collecting and checking
            //scene number for particular root number

            case DisplayAccessor.FETCH_RESOURCES_ROOT:
                new Thread(()->{
                    try{
                        //sceneFunction.fetchElectionBundle();
                        sceneFunction.showStartStatus(sceneFunction.createSocketConnection());
                    }
                    catch(Exception e){e.printStackTrace();}
                }, "Fetch Election Resource").start();
                break;
        }
    }


    private void serverResponseError(){
        Platform.runLater(()->{
            ((Label) getCurrentScene().lookup("#prompt")).setText("Error occurred fetching election data");
            getCurrentScene().lookup("#retryButton").setVisible(true);
        });
    }


    private List<Scene> trimScenesToElect(ArrayList<Scene> scenes){
        return scenes.subList(DisplayAccessor.FINAL_VOTE_BEGIN_SCENE-1, sceneList.size()-2);
    }
}
