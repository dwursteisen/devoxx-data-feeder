package com.github.mongo.labs.feeder.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

    public List<String> techTeam;
    public String requirement;
    public Agenda agenda;

    @Override
    public String toString() {
        return _id + " -> " + speakers;
    }

    public static class TalkSpeaker {
        public MongoSpeaker.Name name;
        public String ref;
    }

    public static class Agenda {
        public String status;
        public String description;

        public Agenda() {
        }

        public Agenda(String status, String description) {
            this.status = status;
            this.description = description;
        }
    }
}
