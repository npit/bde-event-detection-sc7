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

import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.clustering.config.*;
import gr.demokritos.iit.clustering.factory.ClusteringFactory;

import java.util.*;

import gr.demokritos.iit.clustering.repository.IClusteringRepository;
import org.apache.commons.lang3.time.StopWatch;

import static gr.demokritos.iit.base.util.Utils.tic;
import static gr.demokritos.iit.base.util.Utils.tocTell;

/**
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class BDEEventDetection {


    public static void main(String[] args)  {

        // we require one argument, the config file
        String properties;
        if (args.length < 1 ) {
            throw new IllegalArgumentException(String.format("USAGE: %s <PATH_TO_CONFIGURATION_FILE> " +
                    "\n\te.g. %s ./res/clustering.properties", BDEEventDetection.class.getName(), BDEEventDetection.class.getName()));
        }
        properties = args[0];

        // load base configuration, initialize repository
        IClusteringConf configuration = new clusteringConf(properties);

        ClusteringFactory factory = new ClusteringFactory(configuration);
        IClusteringRepository repository = factory.getRepository();

        repository.initialize();
        // just send to strabon and exit, if that mode is specified
        if(configuration.justSendToStrabon())
        {
            System.out.print("Note: No clustering: will only send events to strabon, to url:["
                    +configuration.getStrabonURL()+"].");
            repository.remoteStoreEvents();
            if(factory != null)
            {
                factory.releaseResources();
            }
            return;
        }



        // specify the range of news articles to extract from, for clustering
        Calendar cal = Utils.getCalendarFromStringTimeWindow(configuration.getDocumentRetrievalTimeWindow());
        System.out.println("calendar retrieval setting: " + cal.getTime());
        long tstamp = cal.getTimeInMillis();

        tic();
        repository.loadArticlesToCluster(tstamp);
        tocTell("Article loading");
        repository.printArticles();

        tic();

        repository.clusterArticles();

        tocTell("clustering");
        if(!repository.good() || true) // || true to stop here : time measurements
        {
            repository.destroy();
            factory.releaseResources();
            return;
        }

        //repository.printClusters();



       repository.calculateSummarization();
        if(!repository.good())
        {
            repository.destroy();
            factory.releaseResources();
            return;
        }

        // process tweets
        repository.loadTweetsToCluster(tstamp);
        repository.processTweets();

        repository.localStoreEvents();

        if (configuration.sendToStrabon()) {
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
            factory.releaseResources();
        }
        repository.destroy();
        System.out.println("Done");
        return;

    }











}
