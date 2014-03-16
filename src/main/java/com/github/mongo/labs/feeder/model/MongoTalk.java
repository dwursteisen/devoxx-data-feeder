package com.github.mongo.labs.feeder.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 16/03/14
 * Time: 01:14
 * To change this template use File | Settings | File Templates.
 */
public class MongoTalk {
    public String _id;
    public String type;
    public String summary;
    public String title;
    public String lang;
    public Collection<TalkSpeaker> speakers = new ArrayList<>();
    public LinkedList<String> tags;

    @Override
    public String toString() {
        return _id + " -> " + speakers;
    }

    public static class TalkSpeaker {
        public MongoSpeaker.Name name;
        public String ref;
    }
}
