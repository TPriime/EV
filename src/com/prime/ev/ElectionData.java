package com.prime.ev;

import java.util.ArrayList;
import java.util.Map;

public class ElectionData {
    private String title;
    private String code;
    private ArrayList<String> parties;
    private Map<String, String> criteria;

    public ElectionData(String title, String code, ArrayList<String> partyList, Map<String, String> criteria){
        this.title = title;
        this.code = code;
        this.parties = partyList;
        this.criteria = criteria;
    }

    public String getTitle(){return title;}
    public String getCode(){return code;}
    public ArrayList<String> getPartyList(){return parties;}
    public Map<String, String> getCriteria(){return criteria;}
}
