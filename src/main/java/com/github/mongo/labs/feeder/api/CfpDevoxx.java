package com.github.mongo.labs.feeder.api;

import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 15/03/14
 * Time: 22:57
 * To change this template use File | Settings | File Templates.
 */
public interface CfpDevoxx {

    @GET("/conferences/devoxxFR2014/speakers")
    Observable<List<Speaker>> speakers();

    @GET("/conferences/devoxxFR2014/speakers/{uid}")
    Observable<Speaker> speaker(@Path("uid") String uid);

    @GET("/conferences/devoxxFR2014/talks/{id}")
    Observable<Talk> talk(@Path("id") String id);
}
