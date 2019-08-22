package com.prime.ev;

import java.util.HashMap;
import java.util.Map;

class MessageIntent {
    protected Map<String, String> header;
    protected Map<String, Object> body;

    MessageIntent(String method, String _title, String _key, String _body){
        header = new HashMap<>();
        header.put("method", method.toUpperCase());
        header.put("title", _title.toUpperCase());
        header.put("key", _key);

        body = new HashMap<>();
        body.put("msg", _body);
    }

    MessageIntent(String method, String _title, String _key, VoteData voteData){
        header = new HashMap<>();
        header.put("method", method.toUpperCase());
        header.put("title", _title.toUpperCase());
        header.put("key", _key);

        body = new HashMap<>();
        body.put("vote_data", voteData);
        //new MessageIntent(method, title, key, voteData);
    }

    MessageIntent(){}
/*
    interface StringOrMap {}
    class MMap extends HashMap implements StringOrMap{}
    class MString extends Strin implements StringOrMap{}*/
}
