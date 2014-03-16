package com.github.mongo.labs.feeder;

import com.github.mongo.labs.feeder.api.CfpDevoxx;
import com.github.mongo.labs.feeder.api.Speaker;
import com.github.mongo.labs.feeder.api.Talk;
import com.github.mongo.labs.feeder.model.MongoSpeaker;
import com.github.mongo.labs.feeder.model.MongoTalk;
import com.github.ryenus.rop.OptionParser;
import com.mongodb.MongoURI;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import retrofit.RestAdapter;
import rx.Observable;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 15/03/14
 * Time: 21:28
 * To change this template use File | Settings | File Templates.
 */
@OptionParser.Command(name = "feeder", descriptions = "Will feed an mongodb instance with data from devoxx fr API")
public class Feeder {


    @OptionParser.Option(opt = {"--verbose", "-V"}, description = "Log each operation done")
    private boolean verbose;
    @OptionParser.Option(opt = "--uri", description = "MongoDB uri to use (default: mongodb://localhost:27017/devoxx)")
    private String mongoUri = "mongodb://localhost:27017/devoxx";
    @OptionParser.Option(opt = "--api", description = "Devoxx France CFP API url (default: http://cfp.devoxx.fr/api)")
    private String devoxxApi = "http://cfp.devoxx.fr/api";
    private boolean isRunning = true;

    public static void main(String[] args) {

        // 2. Create the OptionParser instance along with the Command class
        OptionParser parser = new OptionParser(Feeder.class);

        // 3. Parse the args
        parser.parse(args);

    }

    private final List<String> cloudWords = Arrays.asList(
            "agile", // \n
            "nosql", // \n
            "reactive", // \n
            "functionnal", // \n
            "scrum",// \n
            "dart",
            "web",
            "scala",
            "java",
            "devops",
            "docker",
            "cloud",
            "tdd",
            "javascript",
            "git",
            "virtualisation");

    private void run() {
        final Log log = new Log(verbose);
        try {
            log.info("==== Starting Data Feeder ====");

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(devoxxApi)
                    .build();

            final CfpDevoxx service = restAdapter.create(CfpDevoxx.class);
            Jongo jongo = new Jongo(new MongoURI(mongoUri).connectDB());
            final MongoCollection mongoSpeakers = jongo.getCollection("speakers");
            final MongoCollection mongoTalks = jongo.getCollection("talks");


            mongoSpeakers.drop();
            mongoTalks.drop();

            Observable<Speaker> speakers = service.speakers().flatMap(new Func1<List<Speaker>, Observable<Speaker>>() {
                @Override
                public Observable<Speaker> call(List<Speaker> speakers) {
                    return Observable.from(speakers);
                }
            }).flatMap(new Func1<Speaker, Observable<Speaker>>() {
                @Override
                public Observable<Speaker> call(Speaker speaker) {
                    log.info("Gestion du speaker %s %s (%s)", speaker.firstName, speaker.lastName, speaker.uuid);
                    return service.speaker(speaker.uuid);
                }
            });

            Observable<MongoSpeaker> mSpeakers = speakers.map(new Func1<Speaker, MongoSpeaker>() {
                @Override
                public MongoSpeaker call(Speaker speaker) {
                    MongoSpeaker s = new MongoSpeaker();
                    s.name = new MongoSpeaker.Name();
                    s.name.firstName = speaker.firstName;
                    s.name.lastName = speaker.lastName;
                    s.bio = speaker.bioAsHtml;
                    s.geo = new MongoSpeaker.Geo();
                    return s;
                }
            });
            mSpeakers.subscribe(new Action1<MongoSpeaker>() {
                                    @Override
                                    public void call(MongoSpeaker mongoSpeaker) {
                                        log.info("Ajout du speaker %s en base", mongoSpeaker.name);
                                        mongoSpeakers.insert(mongoSpeaker);
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        log.error("Speakers oups", throwable);
                                    }
                                }, new Action0() {
                                    @Override
                                    public void call() {
                                        log.info("Speakers done !");
                                    }
                                }
            );

            Observable<MongoTalk> talks = speakers.flatMap(new Func1<Speaker, Observable<Speaker.AcceptedTalk>>() {
                @Override
                public Observable<Speaker.AcceptedTalk> call(Speaker speaker) {
                    return Observable.from(speaker.acceptedTalks);
                }
            }).flatMap(new Func1<Speaker.AcceptedTalk, Observable<Speaker.Link>>() {
                @Override
                public Observable<Speaker.Link> call(Speaker.AcceptedTalk acceptedTalk) {
                    return Observable.from(acceptedTalk.links);
                }
            }).filter(new Func1<Speaker.Link, Boolean>() {
                @Override
                public Boolean call(Speaker.Link link) {
                    return link.getTalkId() != null;
                }
            }).distinct().flatMap(new Func1<Speaker.Link, Observable<Talk>>() {
                @Override
                public Observable<Talk> call(Speaker.Link link) {
                    log.info("Gestion du talk %s", link.getTalkId());
                    return service.talk(link.getTalkId());
                }
            }).map(new Func1<Talk, MongoTalk>() {
                @Override
                public MongoTalk call(final Talk talk) {
                    MongoTalk t = new MongoTalk();

                    t._id = talk.id;
                    t.title = talk.title;
                    t.lang = talk.lang;
                    t.summary = talk.summaryAsHtml;


                    final String lowerSummary = talk.summaryAsHtml.toLowerCase();
                    t.tags = Observable.from(cloudWords).filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            return lowerSummary.contains(s);
                        }
                    }).reduce(new LinkedList<String>(), new Func2<LinkedList<String>, String, LinkedList<String>>() {
                        @Override
                        public LinkedList<String> call(LinkedList<String> words, String s) {
                            words.add(s);
                            return words;
                        }
                    }).toBlockingObservable().single();


                    t.speakers = Observable.from(talk.speakers).flatMap(new Func1<Talk.Link, Observable<Speaker>>() {
                        @Override
                        public Observable<Speaker> call(Talk.Link link) {
                            log.info("recuperation pour le talk %s du speaker %s", talk.id, link.getSpeakerUid());
                            return service.speaker(link.getSpeakerUid());
                        }
                    }).map(new Func1<Speaker, MongoSpeaker>() {
                        @Override
                        public MongoSpeaker call(Speaker speaker) {
                            MongoSpeaker s = new MongoSpeaker();
                            s.name = new MongoSpeaker.Name();
                            s.name.firstName = speaker.firstName;
                            s.name.lastName = speaker.lastName;
                            // TODO: retrieve _id from mongo
                            // s.id = new ObjectId(speaker.uuid);
                            return s;  //To change body of implemented methods use File | Settings | File Templates.
                        }
                    }).reduce(new LinkedList<MongoSpeaker>(), new Func2<LinkedList<MongoSpeaker>, MongoSpeaker, LinkedList<MongoSpeaker>>() {
                        @Override
                        public LinkedList<MongoSpeaker> call(LinkedList<MongoSpeaker> speakersList, MongoSpeaker mongoSpeaker) {
                            speakersList.add(mongoSpeaker);
                            return speakersList;
                        }
                    }).toBlockingObservable().single();

                    return t;
                }
            });

            talks.subscribe(new Action1<MongoTalk>() {
                                @Override
                                public void call(MongoTalk mongoTalk) {
                                    mongoTalks.insert(mongoTalk);
                                    log.info("Ajout du talk %s en base", mongoTalk._id);
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    log.error("talks Oups", throwable);
                                }
                            }, new Action0() {
                                @Override
                                public void call() {
                                    log.info("Talks : DONE");
                                }
                            }
            );

            Observable.merge(mSpeakers, talks).map(new Func1<Object, Integer>() {
                @Override
                public Integer call(Object o) {
                    return 1;
                }
            }).scan(new Func2<Integer, Integer, Integer>() {
                @Override
                public Integer call(Integer seed, Integer newValue) {
                    return seed.intValue() + newValue.intValue();
                }
            }).subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    log.info("Entité ajouté en base : %d", integer.intValue());
                }
            });
        } catch (UnknownHostException e) {
            log.error("Oups", e);
            return;
        }

    }
}
