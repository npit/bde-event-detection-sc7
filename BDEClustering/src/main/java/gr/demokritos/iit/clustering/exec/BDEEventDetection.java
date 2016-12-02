/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.clustering.exec;

import com.vividsolutions.jts.io.ParseException;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.clustering.clustering.MCLClusterer;
import gr.demokritos.iit.clustering.clustering.ParameterizedBaseArticleClusterer;
import gr.demokritos.iit.clustering.config.*;
import gr.demokritos.iit.clustering.factory.ClusteringFactory;
import gr.demokritos.iit.clustering.factory.DemoClusteringFactory;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.newsum.IClusterer;
import gr.demokritos.iit.clustering.repository.ClusteringCassandraRepository;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import gr.demokritos.iit.clustering.repository.IClusteringRepository;
import gr.demokritos.iit.location.util.GeometryFormatTransformer;
import org.apache.spark.api.java.JavaSparkContext;
import org.scify.asset.server.model.datacollections.CleanResultCollection;
import org.scify.asset.server.model.structures.social.TwitterResult;
import org.scify.asset.social.classification.IClassifier;
import org.scify.asset.social.clustering.SocialMediaClusterer;
import org.scify.asset.social.data.preprocessing.DefaultSocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.ISocialMediaCleaner;
import org.scify.asset.social.data.preprocessing.IStemmer;
import org.scify.asset.social.data.preprocessing.TwitterStemmer;
import org.scify.newsum.server.clustering.IArticleClusterer;
import org.scify.newsum.server.model.datacollections.Articles;
import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.Sentence;
import org.scify.newsum.server.model.structures.Summary;
import org.scify.newsum.server.model.structures.Topic;
import org.scify.newsum.server.nlp.sentsplit.DefaultSentenceSplitter;
import org.scify.newsum.server.nlp.sentsplit.ISentenceSplitter;
import org.scify.newsum.server.summarization.ISummarizer;
import org.scify.newsum.server.summarization.Summarizer;

import static gr.demokritos.iit.base.util.Utils.tic;
import static gr.demokritos.iit.base.util.Utils.toc;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetection {


    public static void main(String[] args) {

        // we require one argument, the config file
        String properties;
        if (args.length < 1 ) {
            throw new IllegalArgumentException(String.format("USAGE: %s <PATH_TO_CONFIGURATION_FILE> " +
                    "\n\te.g. %s ./res/clustering.properties", BDEEventDetection.class.getName(), BDEEventDetection.class.getName()));
        }
        properties = args[0];

        // load base configuration, initialize repository
        IClusteringConf configuration = new clusteringConf(properties);
        boolean SendToStrabon = configuration.sendToStrabon();
        boolean onlySendToStrabon = configuration.justSendToStrabon();

        ClusteringFactory factory = new ClusteringFactory(configuration);
        IClusteringRepository repository = factory.getRepository();

        repository.initialize();
        // just send to strabon and exit, if that mode is specified
        if(onlySendToStrabon)
        {
            System.out.print("Note: No clustering: will only send events to strabon, to url:["
                    +configuration.getStrabonURL()+"].");
            repository.remoteStoreEvents();
            if(factory != null)
            {
                System.out.println("Releasing resources.");
                factory.releaseResources();
            }
            return;
        }


        tic();
        // specify the range of news articles to extract from, for clustering
        Calendar cal = Utils.getCalendarFromStringTimeWindow(configuration.getDocumentRetrievalTimeWindow());
        System.out.println("calendar retrieval setting: " + cal.getTime());
        long tstamp = cal.getTimeInMillis();

        repository.loadArticlesToCluster(tstamp);

        repository.clusterArticles();
        if(!repository.good())
        {
            repository.destroy();
            factory.releaseResources();
            return;
        }

        repository.printClusters();



       repository.calculateSummarization();
        if(!repository.good())
        {
            repository.destroy();
            factory.releaseResources();
            return;
        }
        System.out.println("loading tweets");
        // get token dictionary from topics

        // process tweets
        repository.loadTweetsToCluster(tstamp);
        repository.processTweets();

        repository.localStoreEvents();

        if (SendToStrabon) {
	        String strabonURL=configuration.getStrabonURL();
            System.out.print("Finally,sending events to strabon to url ["+strabonURL+"].");
            repository.remoteStoreEvents();
        }
        else
        {
            System.out.println("Sending events to Strabon is disabled.");
        }

        if(configuration.shouldTriggerChangeDetection())
        {
            repository.changeDetectionTrigger();
        }
        else
        {
            System.out.println("Change detection is disabled.");
        }

        // clean up

        if(factory != null)
        {
            System.out.println("Releasing resources.");
            factory.releaseResources();
        }

        System.out.println("Done");
        return;

    }











}
