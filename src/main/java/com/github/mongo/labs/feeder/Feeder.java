package com.github.mongo.labs.feeder;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.github.mongo.labs.feeder.api.CfpDevoxx;
import com.github.mongo.labs.feeder.api.Speaker;
import com.github.mongo.labs.feeder.model.MongoSpeaker;
import com.github.mongo.labs.feeder.model.MongoTalk;
import com.github.ryenus.rop.OptionParser;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import retrofit.RestAdapter;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 15/03/14
 * Time: 21:28
 * To change this template use File | Settings | File Templates.
 */
@OptionParser.Command(name = "feeder", descriptions = "Will feed an mongodb instance with data from devoxx fr API")
public class Feeder {


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
            "virtualisation",
            "groovy",
            "clojure");

    private final Map<String, Action1<MongoTalk>> conferenceType = new HashMap<>();

    @OptionParser.Option(opt = {"--verbose", "-V"}, description = "Log each operation done")
    private boolean verbose;
    @OptionParser.Option(opt = "--uri", description = "MongoDB uri to use (default: mongodb://localhost:27017/devoxx)")
    private String mongoUri = "mongodb://localhost:27017/devoxx";
    @OptionParser.Option(opt = "--api", description = "Devoxx France CFP API url (default: http://cfp.devoxx.fr/api)")
    private String devoxxApi = "http://cfp.devoxx.fr/api";
    private boolean isRunning = true;
    @OptionParser.Option(opt = "--drop", description = "should drop collections first ?")
    private boolean drop = false;

    public static void main(String[] args) {

        // 2. Create the OptionParser instance along with the Command class
        OptionParser parser = new OptionParser(Feeder.class);

        // 3. Parse the args
        parser.parse(args);

    }

    private final List<List<String>> techTeams = Arrays.asList(
            Arrays.asList("John Doe", "Michel Bucker"),
            Arrays.asList("Luc Flamant", "Bob Artignon"),
            Arrays.asList("Julie Estaki", "Louis Chinel"),
            Arrays.asList("Brigitte Bluz", "Gerard Lanquest")
    );

    private final List<String> requirements = Arrays.asList(
            "Ordinateur avec Java nécessaire",
            "Venir avec un browser capable d'aller sur Iternet",
            "Un ordinateur avec 8 Go de ram nécessaire : on va faire du Virtual Box !",
            "Un ordinateur avec watercooling pour compiler du code Scala",
            "Windows interdit lors de ce Labs !"
    );

    private final List<MongoTalk.Agenda> agendas = Arrays.asList(
            new MongoTalk.Agenda("Brouillon", "Intro / Développement / Pause / Développement / Conclusion"),
            new MongoTalk.Agenda("Brouillon", "Sujet Technique / Troll / Sujet Technique"),
            new MongoTalk.Agenda("Définitif", "Introduction / Slides 1 / Slides 2 ... Slides 378 / Conclusion")
    );

    private void run() {


        conferenceType.put("Conference", (Action1<MongoTalk>) o -> {
            o.techTeam = techTeams.get(new Random().nextInt(techTeams.size()));
        });
        conferenceType.put("BOF (Bird of a Feather)", (Action1<MongoTalk>) o -> {
            // nothing to do
        });
        conferenceType.put("University", (Action1<MongoTalk>) o -> {
            o.agenda = agendas.get(new Random().nextInt(agendas.size()));
            // nothing to do
        });
        conferenceType.put("Hand's on Labs", (Action1<MongoTalk>) o -> {
            o.requirement = requirements.get(new Random().nextInt(requirements.size()));
        });


        final Log log = new Log(verbose);
        try {
            new Feed().feed();
        } catch (UnknownHostException e) {
            log.error("Oups", e);
            return;
        }

    }

    private class Feed {
        private final Log log;
        private final CfpDevoxx service;
        private final MongoCollection dbSpeakers;
        private final MongoCollection dbTalks;

        public Feed() throws UnknownHostException {
            log = new Log(verbose);
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(devoxxApi)
                    .build();

            service = restAdapter.create(CfpDevoxx.class);
            Jongo jongo = new Jongo(new MongoURI(mongoUri).connectDB());
            dbSpeakers = jongo.getCollection("speakers").withWriteConcern(WriteConcern.SAFE);
            dbTalks = jongo.getCollection("talks").withWriteConcern(WriteConcern.SAFE);
        }

        public void feed() {
            log.info("==== Starting Data Feeder ====");

            if (drop) {
                dbSpeakers.drop();
                dbTalks.drop();
            }


            Observable<Speaker> speakers = speakersStream();


            Observable<MongoTalk> talks = talksStream(speakers).flatMap(id -> {
                log.info("Gestion du talk %s", id);
                return service.talk(id);
            }).flatMap(talk -> {
                MongoTalk t = new MongoTalk();

                log.info("Build du talk %s - %s", talk.id, talk.talkType);
                t._id = talk.id;
                t.title = talk.title;
                t.lang = talk.lang;
                t.summary = talk.summaryAsHtml;
                t.type = talk.talkType;

                // update selon le type de conf
                conferenceType.getOrDefault(t.type, (mongoTalk) -> {}).call(t);

                final String lowerSummary = talk.summaryAsHtml.toLowerCase();
                Observable<List<String>> obsTags = Observable.from(cloudWords).filter(lowerSummary::contains).toList();

                Observable<List<MongoTalk.TalkSpeaker>> obsSpeaker = Observable.from(talk.speakers).flatMap(link -> {
                    log.info("recuperation pour le talk %s du speaker %s", talk.id, link.getSpeakerUid());
                    return service.speaker(link.getSpeakerUid());
                }).map(speaker -> {
                    MongoTalk.TalkSpeaker s = new MongoTalk.TalkSpeaker();
                    s.name = new MongoSpeaker.Name();
                    s.name.firstName = speaker.firstName;
                    s.name.lastName = speaker.lastName;

                    MongoSpeaker dbSpeaker = dbSpeakers.findOne("{name: #}", s.name).as(MongoSpeaker.class);
                    if (dbSpeaker != null) {
                        // TODO: object Id instead of string ?
                        s.ref = dbSpeaker._id.toString();
                    }
                    return s;
                }).toList();


                Observable<MongoTalk> just = Observable.just(t);
                return Observable.zip(just, obsTags, obsSpeaker, (zTalk, zTags, zSpeakers) -> {
                    zTalk.tags = new LinkedList<>(zTags);
                    zTalk.speakers = zSpeakers;
                    return zTalk;
                });
            });


            Observable<MongoSpeaker> mSpeakers = speakers.map(speaker -> {
                MongoSpeaker s = new MongoSpeaker();
                s.name = new MongoSpeaker.Name();
                s.name.firstName = speaker.firstName;
                s.name.lastName = speaker.lastName;
                s.bio = speaker.bioAsHtml;
                s.geo = new MongoSpeaker.Geo();
                return s;
            });

            mSpeakers.doOnNext(mongoSpeaker -> {
                log.info("Ajout du speaker %s en base", mongoSpeaker.name);
                dbSpeakers.insert(mongoSpeaker);
            }).cast(Object.class).concatWith(talks.flatMap(mongoTalk -> {
                return Observable.just(mongoTalk).flatMap((t) -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        log.info("Ajout du talk %s en base", mongoTalk._id);
                        dbTalks.insert(mongoTalk);
                        return Observable.just(t);
                    } catch (Exception ex) {
                        log.error("Problème avec le talk " + mongoTalk._id, ex);
                        return Observable.error(ex);
                    }
                }).retry(2);
            })).reduce(0, (seed, value) -> seed + 1).toBlocking().forEach((i) -> log.info("Nb Entité ajouté en base : %d", i));

        }

        private Observable<String> talksStream(Observable<Speaker> speakers) {

            Observable<String> talksIds = speakers
                    .flatMap((Speaker speaker) -> Observable.from(speaker.acceptedTalks))
                    .flatMap((Speaker.AcceptedTalk acceptedTalk) -> Observable.from(acceptedTalk.links))
                    .filter(link -> link.getTalkId() != null)
                    .filter(link -> drop)
                    .filter(link -> {
                        log.info("Recherche du talk %s du speaker %s", link.getTalkId(), link.title);
                        return dbTalks.findOne("{_id: #}", link.getTalkId()).as(MongoTalk.class) == null;
                    }).map(Speaker.Link::getTalkId)
                    .distinct();


            return talksIds;
        }

        private Observable<Speaker> speakersStream() {
            return service.speakers().flatMap(Observable::from).filter((Speaker speaker) -> {
                if (drop) {
                    return true;
                }
                MongoSpeaker.Name name = new MongoSpeaker.Name();
                name.firstName = speaker.firstName;
                name.lastName = speaker.lastName;
                return dbSpeakers.findOne("{name: #}", name).as(MongoSpeaker.class) == null;
            }).flatMap(speaker -> {
                log.info("Gestion du speaker %s %s (%s)", speaker.firstName, speaker.lastName, speaker.uuid);
                return service.speaker(speaker.uuid);
            });
        }
    }
}
