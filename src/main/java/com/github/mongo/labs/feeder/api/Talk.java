package com.github.mongo.labs.feeder.api;

import com.github.mongo.labs.feeder.model.MongoSpeaker;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 16/03/14
 * Time: 00:57
 * To change this template use File | Settings | File Templates.
 */
public class Talk {

    public String id;
    public String type;
    public String summaryAsHtml;
    public String title;
    public String lang;

    public Link[] speakers;

    public static class Link {
        public Href link;
        public String name;

        public String getSpeakerUid() {
            int index = link.href.lastIndexOf("/");
            return link.href.substring(index+1, link.href.length());
        }
    }

    public static class Href {
        public String href;
    }
}
