package com.mongodb.hadoop.pig;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.hadoop.testutils.BaseHadoopTest;
import org.apache.pig.tools.parameters.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.UUID;


public class PigTest extends BaseHadoopTest {
    private static final Logger LOG = LoggerFactory.getLogger(PigTest.class);

    private static final MongoClientURI URI =
      new MongoClientURI("mongodb://localhost:27017/mongo_hadoop.pigtests");
    private MongoClient mongoClient;

    @Before
    public void setup() throws UnknownHostException {
        mongoClient = new MongoClient(URI);
        mongoClient.getDB("mongo_hadoop").dropDatabase();
    }

    @After
    public void tearDown() {
        mongoClient.close();
    }

    public void runMongoUpdateStorageTest(
      final String scriptName, final String[] expected)
      throws IOException, ParseException {
        org.apache.pig.pigunit.PigTest pigTest = new org.apache.pig.pigunit
          .PigTest(getClass().getResource(scriptName).getPath());

        // Let the STORE statement do its job so we can test MongoUpdateStorage.
        pigTest.unoverride("STORE");

        pigTest.assertOutput("results", expected);
    }

    @Test
    public void mongoUpdateStorage() throws IOException, ParseException {
        runMongoUpdateStorageTest(
          "/pig/update_simple_mus.pig",
          new String[]{
            "(Daniel,Alabi,([car#a],[car#b],[car#c],[car#a],[car#b],[car#c]))",
            "(Tolu,Alabi,([car#d],[car#e],[car#f],[car#d],[car#e],[car#f]))",
            "(Tinuke,Dada,([car#g],[car#g]))"
          }
        );
    }

    @Test
    public void mongoUpdateStorageMulti() throws IOException, ParseException {
        runMongoUpdateStorageTest(
          "/pig/update_age_alabis_mus.pig",
          new String[]{
            "(Daniel,Alabi,22.0)",
            "(Tolu,Alabi,24.0)",
            "(Tinuke,Dada,53.0)"
          }
        );
    }

    @Test
    public void testPigUUID() throws IOException, ParseException {
        UUID uuid = UUID.randomUUID();
        BasicDBObject doc = new BasicDBObject("uuid", uuid);
        mongoClient.getDB("mongo_hadoop")
          .getCollection("uuid_test").insert(doc);

        org.apache.pig.pigunit.PigTest test =
          new org.apache.pig.pigunit.PigTest(
            getClass().getResource("/pig/pig_uuid.pig").getPath());
        test.assertOutput(new String[]{"(" + uuid.toString() + ")"});
    }

    @Test
    public void testPigBSONOutput() throws IOException, ParseException {
        org.apache.pig.pigunit.PigTest test =
          new org.apache.pig.pigunit.PigTest(
            getClass().getResource("/pig/bson_test.pig").getPath());
        test.unoverride("STORE");

        String[] expected = {
          "([_id#51eb4b4c873b8b9013457e4a,last#Alabi,first#Daniel,cars#(a,b,c),age#19.0])",
          "([_id#51eb4b7a873b8b9013457e4b,last#Alabi,first#Tolu,cars#(d,e,f),age#21.0])",
          "([_id#51eb4bd3873b8b9013457e4c,last#Dada,first#Tinuke,cars#(g),age#50.0])"
        };
        test.assertOutput("persons_read", expected);
    }

}
