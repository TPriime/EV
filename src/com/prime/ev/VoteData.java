package com.prime.ev;

import java.util.Map;

public class VoteData {
    private String voter_id;
    private Map<String, String> votes;

    VoteData(String voter_id, Map<String, String> votes){
        this.voter_id = voter_id;
        this.votes = votes;
    }
}
