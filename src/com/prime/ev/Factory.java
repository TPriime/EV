package com.prime.ev;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class Factory {

    static JSONObject factoryObject;
    static String PROPERTY_FILE = "device_properties.json";

    static {
        try{
            //Factory.class.getResource(PROPERTY_FILE+"dvdv").getFile();
            factoryObject = (JSONObject) new JSONParser()
                    .parse(new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream(PROPERTY_FILE)))
                            .lines().collect(Collectors.joining()));
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public static String getProperty(String property) {
        return (String) factoryObject.get(property);
    }


    public static void setProperty(String property, String value){
        factoryObject.put(property, value);
    }


    public static String fetchElectionData() throws FileNotFoundException {
        //long operation
        try{
                Thread.sleep(2000); //wait for required scene to load properly
        }catch(Exception e){
            e.printStackTrace();
        }

        return new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream("ElectionData.json")))
                .lines().collect(Collectors.joining());
    }



    public static String fetchUserData() throws FileNotFoundException {
        try{Thread.sleep(1000);}catch (Exception e){}
        return new BufferedReader(new InputStreamReader(Factory.class.getResourceAsStream("UserData.json")))
                .lines().collect(Collectors.joining());
    }


    public static String getFingerprint(){
        return "";
    }


    public static void sendAndRecordVote(String voteInstance){
        //nothing for now
    }
}
