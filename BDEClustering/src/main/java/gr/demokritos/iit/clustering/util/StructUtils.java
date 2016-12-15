package gr.demokritos.iit.clustering.util;

import gr.demokritos.iit.clustering.model.BDEArticle;
import gr.demokritos.iit.clustering.model.IdentifiableDocumentWordGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.events.WordEvaluatorListener;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
import scala.Tuple4;

import java.util.List;

public class StructUtils {

    public static IdentifiableDocumentWordGraph articleTupleToDWG(Tuple4<String, String, String, Long> art)
    {
        IdentifiableDocumentWordGraph wg = new IdentifiableDocumentWordGraph();
        wg.WordEvaluator = new WordEvaluatorListener() {
            public boolean evaluateWord(String string) {
                return string.length() > 3 && string.matches("\\p{javaUpperCase}+.*");
            }
        };
        String data = art._2()+" "+art._3();
        wg.setId(data.hashCode());
        wg.setDataString(data);
        return wg;
    }
    public static Tuple4<String, String, String, Long> articleToTuple4(BDEArticle art)
    {
        return new Tuple4<>(art.getSource(), art.getTitle(), art.getDescription(), art.getDate().getTimeInMillis());
    }
    public static Tuple2<Tuple4<String, String, String, Long>,Tuple4<String, String, String, Long>>
            articlesToTuple4Pair(BDEArticle art1 , BDEArticle art2)
    {
        return new Tuple2<>(StructUtils.articleToTuple4(art1),StructUtils.articleToTuple4(art2));
    }
    public static void printGraphPairs(JavaPairRDD<DocumentWordGraph,DocumentWordGraph> rddPairs, int items ) {

        System.out.println("PAIRS:\n");
        List<Tuple2<DocumentWordGraph,DocumentWordGraph>> takeN=null;
        if(items > 0)
            takeN = rddPairs.take(items);
        else
            takeN = rddPairs.collect();

        for (Tuple2<DocumentWordGraph,DocumentWordGraph>  eachPair: takeN) {
            System.out.println("graph1: " + eachPair._1.toString() + ", graph2: " + eachPair._2.toString());
        }
    }

    public static void printArticlePairs(JavaPairRDD<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> rddPairs, int items ) {

        System.out.println("PAIRS:\n");
        List<Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>>> takeN = rddPairs.take(items);

        for (Tuple2<Tuple4<String, String, String, Long>, Tuple4<String, String, String, Long>> eachPair: takeN) {
            System.out.println("url1: " + eachPair._1._1() + ", url2: " + eachPair._2._1());
            System.out.println("title1: " + eachPair._1._2() + ", title2: " + eachPair._2._2());
        }
    }
    public static void printArticles(JavaRDD<Tuple4<String, String, String, Long>> args)
    {
        System.out.println("ARTICLES:");
        List<Tuple4<String, String, String, Long>> L = args.collect();
        int count = 0;
        for(Tuple4<String, String, String, Long> t : L)
        {
            System.out.println(count++ + " - 1:" + t._1().toString() + " 2:" + t._2());
        }
    }
}
