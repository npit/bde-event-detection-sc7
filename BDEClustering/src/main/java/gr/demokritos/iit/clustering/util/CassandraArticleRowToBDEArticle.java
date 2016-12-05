package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import gr.demokritos.iit.clustering.model.BDEArticle;
import org.apache.spark.api.java.function.Function;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by nik on 12/2/16.
 */
public class CassandraArticleRowToBDEArticle implements Function<CassandraRow,BDEArticle> {

    private String placesColumn;
    private String urlColumn;
    private String title;
    public CassandraArticleRowToBDEArticle(String urlColumn,String title,String  placesColumn)

    {
        this.urlColumn = urlColumn;
        this.placesColumn = placesColumn;
        this.title = title;
    }
    @Override
    public BDEArticle call(CassandraRow v1) throws Exception {
        // constructor is
        // public BDEArticle(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl, Date date, Map<String, String> placesMap) {
        Set<String> places = (Set) v1.getSet(placesColumn);
        HashMap<String,String> justPlaces = new HashMap<>();
        for(String pl : places)
        {
            justPlaces.put(pl,"");
        }

        return new BDEArticle(urlColumn, title, "","","",null,new Date(),justPlaces);
    }
}
