package com.prime.ev;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Result {

    public static Map<String, Integer> summary(Stream<String> rawVoteDataJsonStream){
        Map<String, Integer> summaryMap = new HashMap<>();

        //getCountFromRawVoteDataJson(rawVoteDataJsonStream).comp

        final int[] total = {0};
        getCountFromRawVoteDataJson(rawVoteDataJsonStream).forEach((election, votes)->{
            total[0] += votes.size();
            summaryMap.put(election, votes.size());
        });
        summaryMap.put("total", total[0]);
        return summaryMap;
    }


    public static List<Map.Entry<String, Integer>> computePresidentialCount(Stream<String> rawVoteDataJsonStream){
        Map<String, List<String>> votes = getCountFromRawVoteDataJson(rawVoteDataJsonStream);
        return getPresidentialVoteCount(votes).collect(Collectors.toList());
    }



    public static Map<String, List<String>> getCountFromRawVoteDataJson(Stream<String> rawVoteDataJsonStream){
        Map<String, List<String>> votes = new HashMap<>();

        rawVoteDataJsonStream.filter(line->!line.trim().isEmpty()).forEach(line -> {
            VoteData voteData = new Gson().fromJson(line.split("-")[1], VoteData.class);

            voteData.votes.forEach(vote->{
                //System.out.println(vote);
                String election = vote.get("election").toLowerCase();
                votes.putIfAbsent(election, new ArrayList<>());
                votes.get(election).add(vote.get("party").toUpperCase());
            });
        });

        return votes;
    }



    public static Stream<Map.Entry<String, Integer>> getPresidentialVoteCount(Map<String, List<String>> votes) throws NullPointerException{
        return sortedVoteCount(_getPresidentialVoteCount(votes));
    }



    public static Stream<Map.Entry<String, Integer>> sortedVoteCount(Map<String, Integer> voteCountMap){
        return voteCountMap.entrySet().stream().sorted((p1, p2)->{
            if(p1.getValue()>p2.getValue()) return -1;
            else if(p1.getValue()==p2.getValue()) return 0;
            else return 1;
        });
    }
    //public

    public static Map<String, Integer> _getPresidentialVoteCount(Map<String, List<String>> votes) throws NullPointerException{
        Map<String, Integer> presidentialVoteCount = new HashMap<>();
        votes.get("Presidential".toLowerCase()).forEach(party->{
            presidentialVoteCount.putIfAbsent(party, new Integer(0));
            Integer incr  = presidentialVoteCount.get(party).intValue() + 1;
            presidentialVoteCount.put(party, incr);
        });

        return presidentialVoteCount;
    }
}
