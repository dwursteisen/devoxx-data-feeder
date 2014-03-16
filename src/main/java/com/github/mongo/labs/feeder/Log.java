package com.github.mongo.labs.feeder;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 15/03/14
 * Time: 21:56
 * To change this template use File | Settings | File Templates.
 */
public class Log {

    private final boolean verbose;

    public Log(boolean verbose) {
        this.verbose = false;
    }

    public Log() {
        this(true);
    }

    public void info(String message) {
        System.out.println(message);
    }

    public void info(String message, Object... args) {
        info(String.format(message, args));
    }

    public void info(String message, Throwable ex) {
        info(message);
        ex.printStackTrace();

    }

    public void error(String message) {
        System.err.println(message);
    }

    public void error(String message, Object... args) {
        error(String.format(message, args));
    }

    public void error(String message, Throwable ex) {
        error(message);
        ex.printStackTrace();
    }

}
