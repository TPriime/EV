package com.prime.ev;

import java.util.Map;

public class VoteData {
    private String id;
    private Map<String, String> votes;

    VoteData(String id, Map<String, String> votes){
        this.id = id;
        this.votes = votes;
    }
}
