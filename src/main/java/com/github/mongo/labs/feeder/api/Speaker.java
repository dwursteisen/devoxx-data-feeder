package com.github.mongo.labs.feeder.api;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 15/03/14
 * Time: 22:58
 * To change this template use File | Settings | File Templates.
 */
public class Speaker {

    public String uuid;
    public String lastName;
    public String firstName;
    public String avatarURL;
    public String bioAsHtml;
    public AcceptedTalk[] acceptedTalks;

    public static class AcceptedTalk {
        public String talkType;
        public String track;
        public Link[] links;
    }

    public static class Link {
        public String href;
        public String title;

        public String getTalkId() {
            if(href.contains("devoxxFR2015/talks")) {
                int index = href.lastIndexOf("/") + 1;
                return href.substring(index, href.length());
            }
            return null;
        }

    }
}
