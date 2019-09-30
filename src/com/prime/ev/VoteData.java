package com.prime.ev;

import java.util.List;
import java.util.Map;

public class VoteData {
    private String voter;
    private String device;
    private String voteTime;
    private List<Map<String, String>> votes;

    VoteData(String voter_id, String device_id, List<Map<String, String>> votes, String voteTime){
        this.voter = voter_id;
        this.votes = votes;
        this.device = device_id;
        this.voteTime = voteTime;
    }
}
