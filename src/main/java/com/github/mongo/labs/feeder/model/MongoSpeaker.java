package com.github.mongo.labs.feeder.model;

import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 16/03/14
 * Time: 00:05
 * To change this template use File | Settings | File Templates.
 */
public class MongoSpeaker {

    public static class Name {
        public String lastName;
        public String firstName;

        @Override
        public String toString() {
            return lastName + " " + firstName;
        }
    }

    public static class Geo {
        public double longitude;
        public double latitude;

        public Geo() {
            Random random = new Random();
            longitude = new BigDecimal(random.nextInt(24808 - 22319) + 22319).divide(new BigDecimal("10000")).setScale(4, RoundingMode.DOWN).doubleValue();
            latitude = new BigDecimal(random.nextInt(489101 - 488010) + 488010).divide(new BigDecimal("10000")).setScale(4, RoundingMode.DOWN).doubleValue();
        }
    }

    public Name name;
    public String bio;
    public Geo geo;

    public ObjectId _id;
    // public byte[] avatar;

    public String toString() {
        return "" + _id + " name : "+name;
    }
}
