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
package gr.demokritos.iit.crawlers.twitter;

import gr.demokritos.iit.crawlers.twitter.impl.ITwitterRestConsumer;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import gr.demokritos.iit.crawlers.twitter.factory.conf.TConfig;
import gr.demokritos.iit.crawlers.twitter.factory.conf.ITwitterConf;
import gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory;
import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository.CrawlEngine;
import gr.demokritos.iit.crawlers.twitter.stream.IStreamConsumer;
import gr.demokritos.iit.crawlers.twitter.structures.SearchQuery;
import gr.demokritos.iit.crawlers.twitter.utils.QueryLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CrawlSchedule {

    public static void main(String[] args)
            throws IOException,
            SQLException, PropertyVetoException,
            IllegalArgumentException, ParseException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        loadCmdParams(args);
        switch (operation) {
            case MONITOR:
                monitor();
                break;
            case SEARCH:
                search();
                break;
            case STREAM:
                getStream();
                break;
        }
    }

    public static void monitor() {

        // load properties
        ITwitterConf config = new TConfig(properties);
        // init crawl factory
        TwitterListenerFactory factory = new TwitterListenerFactory(config);
        ITwitterRestConsumer crawler;
        try {
            // instantiate crawler
            crawler = factory.getTwitterListener();
            // start monitoring
            crawler.monitor();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | PropertyVetoException ex) {
            LOGGER.severe(ex.toString());
        } finally {
            factory.releaseResources();
        }
    }

    public static void search() throws IOException, SQLException, PropertyVetoException {

        // load properties
        ITwitterConf config = new TConfig(properties);

        TwitterListenerFactory factory = new TwitterListenerFactory(config);

        ITwitterRestConsumer crawler;
        try {
            crawler = factory.getBaseTwitterListener();
            // search for each of these queries
            crawler.search(queries);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | PropertyVetoException ex) {
            LOGGER.severe(ex.toString());
        } finally {
            factory.releaseResources();
        }

    }

    public static void getStream() {

        // load properties
        ITwitterConf config = new TConfig(properties);

        TwitterListenerFactory factory = new TwitterListenerFactory(config);

        IStreamConsumer stream = null;
        try {
            stream = factory.getStreamImpl();
            // search for each of these queries
            stream.getStream();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | PropertyVetoException ex) {
            LOGGER.severe(ex.toString());
        }
//        finally {
//
//            factory.releaseResources();
//        }

    }

    private static String properties;
    private static Set<SearchQuery> queries;
    private static CrawlEngine operation;

    private static void loadCmdParams(String[] args) throws IllegalArgumentException, ParseException {
        Options options = new Options();
        // add sources option
        options.addOption("q", "queries", true, "provide full path of the document containing the queries for the crawler. "
                + "\n\tEach query is represented by a single line containing 'query***iso_code***max_results' "
                + "\n\t\tmax_results may be ommited. ");
        // add properties option
        options.addOption("p", "properties", true, "provide full path of the file containing the properties for the crawler");
        // add properties option
        options.addOption("o",
                "operation",
                true,
                "operation = 'search' | 'monitor' | 'stream' :\n "
                + "\tif 'search' is applied but 'queries' param is not supplied, system will look for './twitter.queries' file to proceed"
                + "\tdefault operation is 'monitor'");
        // add help
        options.addOption("h", "help", false, "display this help message");
        // read params
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);
        if (args.length == 0 || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CrawlSchedule.class.getName(), options);
            System.exit(0);
        }
        boolean load_queries = false;
        if (cmd.hasOption("operation")) {
            operation = CrawlEngine.valueOf(cmd.getOptionValue("operation").toUpperCase());
            if (operation == CrawlEngine.SEARCH) {
                load_queries = true;
            }
        } else {
            operation = CrawlEngine.MONITOR;
        }
        String queries_file;
        // load queries
        if (cmd.hasOption("queries")) {
            queries_file = cmd.getOptionValue("queries");
        } else {
            queries_file = "./res/twitter.queries";
        }
        // load properties
        if (cmd.hasOption("properties")) {
            properties = cmd.getOptionValue("properties");
        } else {
            properties = "./res/twitter.properties";
        }
        File fProperties = new File(properties);
        // read values
        try {
            boolean exists = fProperties.exists();
            if (!exists) {
                throw new IllegalArgumentException("please provide a properties file for the crawler");
            }
            if (load_queries) {
                queries = QueryLoader.LoadQueriesFromFile(queries_file, null, null);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("please provide a queries file for the crawler", ex);
        }
    }

}
