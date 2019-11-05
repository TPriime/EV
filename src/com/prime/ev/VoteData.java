package com.prime.ev;

import java.util.List;
import java.util.Map;

public class VoteData {
    public String voter;
    public String device;
    public String voteTime;
    public List<Map<String, String>> votes;

    VoteData(String voter_id, String device_id, List<Map<String, String>> votes, String voteTime){
        this.voter = voter_id;
        this.votes = votes;
        this.device = device_id;
        this.voteTime = voteTime;
    }


}
