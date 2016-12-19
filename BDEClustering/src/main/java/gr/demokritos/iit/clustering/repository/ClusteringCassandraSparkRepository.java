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
package gr.demokritos.iit.clustering.repository;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import gr.demokritos.iit.clustering.config.IClusteringConf;
import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingGraphPairsFuncSerialGraphs;
import gr.demokritos.iit.clustering.newsum.ExtractMatchingPairsFuncSerialGraphs;
import gr.demokritos.iit.clustering.util.*;
import org.apache.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
import scala.Tuple4;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class ClusteringCassandraSparkRepository extends ClusteringCassandraRepository implements IClusteringRepository{

    private  SparkContextJavaFunctions scjf;
    private final String keyspace;
    private JavaSparkContext sc;
    private final int numDaysBatch;
    private SparkConf sparkconf;

    // data
    private JavaRDD<Tuple4<String, String, String,  Long>> articles4RDD;
    private JavaPairRDD<
            Tuple4<String, String, String, Long>,
            Tuple4<String, String, String, Long>> articlePairsRDD;

    private List<Boolean> matches;
    List<Tuple2<String,ArrayList<String>>> PlacesPerArticle;


    @Override
    public void initialize() {

        Level desiredLevel = Level.ERROR;
//        Logger.getRootLogger().setLevel(desiredLevel);
//        Logger.getLogger("org").setLevel(desiredLevel);
//        Logger.getLogger("akka").setLevel(desiredLevel);

        status = true;
        sparkconf = new SparkConf(true)
                .setMaster(configuration.getMaster())
                .setAppName(configuration.getAppName())
                .set(IClusteringConf.SPARK_EXECUTOR_MEMORY, configuration.getClusterExecutorMemory())
                .set(IClusteringConf.SPARK_CASSANDRA_CONNECTION_HOST, configuration.getCassandraHosts()[0]) // TODO FIXME
                .set(IClusteringConf.SPARK_CASSANDRA_CONNECTION_PORT, String.valueOf(configuration.getCassandraPort()));
        this.sc = new JavaSparkContext(sparkconf);
        this.scjf = CassandraJavaUtil.javaFunctions(sc);
        sc.setLogLevel(desiredLevel.toString());

        super.initialize();


    }

    @Override
    public void remoteStoreEvents() {
        System.out.println("Spark - store events is TODO");
    }

    public ClusteringCassandraSparkRepository(Session session, IClusteringConf configuration) {
        super(session,configuration);
        this.keyspace = configuration.getCassandraKeyspace();
        this.numDaysBatch = configuration.getNumDaysBatch();

    }

    public long getLatestTimestamp(String table) {
//        CassandraJavaRDD<CassandraRow> limit = scjf.cassandraTable(keyspace, table).select("last_parsed").limit(1l);
//        List<CassandraRow> collect = limit.collect();
//        return collect.get(0).getLong("last_parsed");
        // dummy.
        System.out.println("FIX getLatestTimestamp");
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) - numDaysBatch);
        return now.getTimeInMillis();
    }


    @Override
    public void loadArticlesToCluster()
    {
        super.loadArticlesToCluster();
        articlesToArticleTuples4();

//        articleOrder = new HashMap<>();
//        for(BDEArticle art : articles)
//        {
//            IdentifiableDocumentWordGraph wg = new IdentifiableDocumentWordGraph();
//            wg.WordEvaluator = new WordEvaluatorListener() {
//                public boolean evaluateWord(String string) {
//                    return string.length() > 3 && string.matches("\\p{javaUpperCase}+.*");
//                }
//            };
//            wg.setDataString(art.getTitle()+" "+art.getDescription());
//            graphs.add(wg);
//            // keep track of the pairings
//            articleOrder.put(wg,art);
//
//        }

    }


    public void articlesToArticleTuples4()
    {
        List<Tuple4<String,String,String,Long>> articlesTuple4List = new ArrayList<>();
        for(BDEArticle art : articles)
        {
            Tuple4<String,String,String,Long> t4 = new Tuple4<>(art.getSource(),art.getTitle(),art.getDescription(),art.getDate().getTimeInMillis());
            articlesTuple4List.add(t4);
        }
        this.articles4RDD = this.sc.parallelize(articlesTuple4List,configuration.getNumPartitions());

    }

//    /**
//     * load articles in a tuple4 format
//     * <br>
//     * we need title for allocating it in clusters
//     *
//     * @param timestamp from epoch
//     * @return <entry_url, title, clean_text, timestamp>
//     */
//    @Override
//    public void loadArticlesToCluster(long timestamp) {
//
//        org.apache.log4j.Logger L = org.apache.log4j.Logger.getRootLogger();
//        L.setLevel(org.apache.log4j.Level.WARN);
//
//        // get data from the database to RDD
//        // -----------------------------------
//        System.out.println(String.format("Loading articles using window :[%s] , max number of articles: [%d]",
//                configuration.getDocumentRetrievalTimeWindow(),configuration.getMaxNumberOfArticles()));
//        // TODO improve query: try to filter by timestamp on cassandra, not afterwards
//        //System.out.println("DEBUG - LIMIT up to 20 articles"); // fixme
//
//        JavaRDD<CassandraRow> fetchedRows = scjf
//                .cassandraTable(keyspace, Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PUBLISHED_DATE.getTableName())
//        .select(Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CRAWLED.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName())
//                .filter(new FilterByAfterTimeStamp(timestamp));
//
//        //sort by crawled date
//        // ---------------------
//        System.out.println("Got " + fetchedRows.count() + " elements from the database. Sorting by crawled date...");
//        JavaRDD<CassandraRow> sortedRows =
//                fetchedRows.sortBy( new SortCassandraRowsByCrawledDate(Cassandra.RSS.TBL_ARTICLES.FLD_CRAWLED.getColumnName()),false,1);
//
//        // limit to max articles
//        // ------------------------
//        // associate each row with a number
//        JavaPairRDD<CassandraRow,Long> sortedRowsIndexed = sortedRows.zipWithIndex();
//        int maxArticles = configuration.getMaxNumberOfArticles();
//        // keep up to the maxArticles-th row
//        JavaPairRDD<CassandraRow,Long> sortedRowsIndexedFiltered =
//                sortedRowsIndexed.filter(new FilterRowNumber(maxArticles));
//        // drop the row number column
//        JavaRDD<CassandraRow> sortedRowsFiltered =
//                sortedRowsIndexedFiltered.map(new FirstPairTuple5PairElement());
//        System.out.println("Sorted " + sortedRowsFiltered.count() + " elements. Mapping to articles.");
//
//        // make the BDEArticle list
//        // ------------------------
//
//        // is it maybe faster to load them normaly and convert what's needed to rdd ?
//
//        // get information that will populate the events. get places from the basic table
//        // getting the geometries needs one additional query. so we'll set it later
//        JavaRDD<BDEArticle>  bdeArticlesRDD = sortedRowsFiltered.map(
//                new CassandraArticleRowToBDEArticle(
//                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
//                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
//                        Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PLACE_LITERAL.getColumnName()
//                ));
//        articles = bdeArticlesRDD.collect();
//        System.out.print("Mapped " + articles.size() + " elements.Getting geometries...");
//
//
//
//        // get geometries
//        // ---------------
//        Map<String,String> placesCache = new HashMap<>();
//        for(BDEArticle art : articles)
//        {
//            String url = art.getSource();
//            Map<String,String> currPlacesGeometries = new HashMap<>();
//            for(String place : art.getPlaces_to_polygons().keySet())
//            {
//                if(!placesCache.containsKey(place))
//                {
//                    // query it
//                    Statement select
//                            = QueryBuilder
//                            .select()
//                            .column(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName())
//                            .from(session.getLoggedKeyspace(), Cassandra.RSS.Tables.NEWS_ARTICLES_PER_PLACE.getTableName())
//                            .where(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_ENTRY_URL.getColumnName(), url))
//                            .and(eq(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_PLACE_LITERAL.getColumnName(), place));
//                    ResultSet result = session.execute(select);
//                    if(result == null)
//                    {
//                        System.err.println(String.format("Geometry-query of entry [%s] , place [%s] yielded null results.",url,place));
//                        continue;
//                    }
//                    Row one = result.one();
//                    if(one == null)
//                    {
//                        System.err.println(String.format("Geometry-query 1st row of entry [%s] , place [%s] yielded null results.",url,place));
//                        continue;
//                    }
//                    String geom = one.getString(Cassandra.RSS.TBL_ARTICLES_PER_PLACE.FLD_BOUNDING_BOX.getColumnName());
//                    placesCache.put(place,geom);
//                    currPlacesGeometries.put(place,geom);
//                }
//                else // already cached
//                    currPlacesGeometries.put(place,placesCache.get(place));
//            }
//            // done with article, set its place mappings
//            art.setPlaces_to_polygons(currPlacesGeometries);
//        }
//
//        System.out.println("done.");
//
//        articles4RDD = sortedRowsFiltered.map(new CassandraArticleRowToTuple4RDD(
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_ENTRY_URL.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_TITLE.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_CLEAN_TEXT.getColumnName(),
//                Cassandra.RSS.TBL_ARTICLES_PER_DATE.FLD_PUBLISHED.getColumnName()
//        ));
//
//    }


    @Override
    public void destroy() {
        super.destroy();
    }


//    @Override
//    public void clusterArticles()
//    {
//
//        JavaRDD<IdentifiableDocumentWordGraph> graphsRDD = sc.parallelize(graphs,configuration.getNumPartitions());
//
//        tic();
//        JavaPairRDD<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph> uniqueGraphPairsRDD =
//                graphsRDD.cartesian(graphsRDD).filter(new GraphPairGenerationFilterFunction());
//        StructUtils.printGraphPairs(uniqueGraphPairsRDD,-1);
//
//        JavaRDD<Boolean> matchesrdd = uniqueGraphPairsRDD.map(new ExtractMatchingGraphPairsFuncSerialGraphs(
//                configuration.getSimilarityMode(), configuration.getCutOffThreshold(), configuration.getNGramMode()));
//        matches =  matchesrdd.collect();
//        tocTell("graph clustering");
//
//
//        List<Tuple2<
//                Tuple4<String, String, String, Long>,
//                Tuple4<String, String, String, Long>>>
//                articlePairsList = new ArrayList<>();
//        List<Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>> graphPairs = uniqueGraphPairsRDD.collect();
//        for(Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph> t : graphPairs)
//        {
//            BDEArticle a1 =  articleOrder.get(t._1());
//            BDEArticle a2 = articleOrder.get(t._2());
//            articlePairsList.add( StructUtils.articlesToTuple4Pair(a1,a2));
//        }
//
//
//        HashMap<Tuple2<
//                Tuple4<String, String, String, Long>,
//                Tuple4<String, String, String, Long>>,Boolean> articlePairsToBoolean = new HashMap<>();
//        for(int i=0;i<matches.size();++i)
//        {
//            articlePairsToBoolean.put(articlePairsList.get(i),matches.get(i));
//        }
//
//        super.setPlacesPerArticle(PlacesPerArticle);
//        this.ArticlesPerCluster = (HashMap) super.justCluster(articlePairsToBoolean);
//
//    }




    private void calcBooleanMatches()
    {
        System.out.println("Calculating boolean similarity matches...");
        // generate article combinations
        DocumentPairGenerationFilterFunction DocPair = new DocumentPairGenerationFilterFunction();
        articlePairsRDD = articles4RDD.cartesian(articles4RDD).filter(DocPair);

        // map to similarity, threshold to boolean matches
        JavaRDD<Boolean> matchesrdd = articlePairsRDD.map(new ExtractMatchingPairsFuncSerialGraphs(
                configuration.getSimilarityMode(), configuration.getCutOffThreshold(), configuration.getNGramMode(), IsVerbose));
        matches =  matchesrdd.collect();
    }

    private void calcBooleanMatches_precomputedGraphs()
    {
        System.out.println("Calculating boolean similarity matches...");
        // generate article graphs
        JavaRDD<Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>> graphsRDD = articles4RDD.map(new ArticleGraphCalculator());

        // generate pairs
        JavaPairRDD<
                Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>,
                Tuple2<Tuple4<String, String, String, Long>, IdentifiableDocumentWordGraph>> articleGraphPairsRDD =
                graphsRDD.cartesian(graphsRDD).filter(new GraphPairGenerationFilterFunction());

        // get article pairs only
        JavaRDD<Tuple2<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>>> articlePairs =
                articleGraphPairsRDD.map(new ArticleGraphPairToArticlePair());

        articlePairsRDD = JavaPairRDD.fromJavaRDD(articlePairs);
        // get graph pairs only
        JavaRDD<Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>> graphPairsRDD_ =
                articleGraphPairsRDD.map(new ArticleGraphPairToGraphPair());

        JavaPairRDD<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>
                graphPairsRDD = JavaPairRDD.fromJavaRDD(graphPairsRDD_);

//        List<Tuple2<
//                Tuple4<String, String, String, Long>,
//                Tuple4<String, String, String, Long>>> articlePairsList = articlePairsRDD.collect();
//        List<Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph>> graphPairsList = graphPairsRDD.collect();

//
//        System.out.println();
//        System.out.println();
//        for(Tuple2<IdentifiableDocumentWordGraph,IdentifiableDocumentWordGraph> t :  graphPairsList)
//        {
//            System.out.println(t._1().getDataString() + " | " + t._2().getDataString());
//        }
//        System.out.println();
//        System.out.println();
//
//
//
//
//        System.out.println();
//        System.out.println();
//        for(Tuple2<
//                Tuple4<String, String, String, Long>,
//                Tuple4<String, String, String, Long>> t :  articlePairsList)
//        {
//            System.out.println(t._1()._2() + " | " + t._2()._2());
//        }
//        System.out.println();
//        System.out.println();




        // map to similarity, threshold to boolean matches
        JavaRDD<Boolean> matchesrdd = graphPairsRDD.map(new ExtractMatchingGraphPairsFuncSerialGraphs(
                configuration.getSimilarityMode(), configuration.getCutOffThreshold(), configuration.getNGramMode()));
        matches =  matchesrdd.collect();


    }



    @Override
    public void clusterArticles()
    {
        // use te base repository
        if(configuration.hasModifier(IClusteringConf.Modifiers.PRECOMPUTE_GRAPHS.toString()))
            calcBooleanMatches_precomputedGraphs();
        else
            calcBooleanMatches();

        List<Tuple2<
                Tuple4<String, String, String, Long>,
                Tuple4<String, String, String, Long>>>
                articlePairsList = articlePairsRDD.collect();

        HashMap<Tuple2<
                Tuple4<String, String, String, Long>,
                Tuple4<String, String, String, Long>>,Boolean> articlePairsToBoolean = new HashMap<>();

//        if(IsVerbose) {
//
//
//            int c = -1;
//            while (++c < matches.size()) {
//                boolean b = matches.get(c);
//                if (b)
//                    System.out.print("true  :");
//                else
//                    System.out.print("false :");
//
//                System.out.println(1 + c + " : " + articlePairsList.get(c)._1()._2() + " | " + articlePairsList.get(c)._2()._2());
//
//            }
//        }

        for(int i=0;i<matches.size();++i)
        {
            articlePairsToBoolean.put(articlePairsList.get(i),matches.get(i));
        }

        super.setPlacesPerArticle(PlacesPerArticle);
        this.ArticlesPerCluster = (HashMap) super.justCluster(articlePairsToBoolean);

    }

}
