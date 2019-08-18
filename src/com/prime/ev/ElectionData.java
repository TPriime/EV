package com.prime.ev;

import java.util.ArrayList;
import java.util.Map;

public class ElectionData {
    private String title;
    private ArrayList<String> parties;
    private Map<String, String> criteria;

    public ElectionData(String title, ArrayList<String> partyList, Map<String, String> criteria){
        this.title = title;
        this.parties = partyList;
        this.criteria = criteria;
    }

    public String getTitle(){return title;}
    public ArrayList<String> getPartyList(){return parties;}
    public Map<String, String> getCriteria(){return criteria;}
}
