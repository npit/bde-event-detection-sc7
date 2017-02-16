package gr.demokritos.iit.clustering.model;

import org.scify.newsum.server.model.structures.Article;
import org.scify.newsum.server.model.structures.URLImage;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class BDEArticle extends Article {

    private Map<String, String> places_to_polygons;
    private Set<String> Entities;
    public BDEArticle(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl,
                      Date date, Map<String, String> placesMap) {
        super(sSource, Title, Text, Category, Feed, imageUrl, date);
        this.places_to_polygons = placesMap;

    }
    public BDEArticle(String sSource, String Title, String Text, String Category, String Feed, URLImage imageUrl,
                      Date date, Map<String, String> placesMap, Set<String> entities) {
        super(sSource, Title, Text, Category, Feed, imageUrl, date);
        this.places_to_polygons = placesMap;
        this.Entities = entities;
    }

    public Map<String, String> getPlaces_to_polygons() {
        return places_to_polygons;
    }
    public Set<String> getEntities() {
        return Entities;
    }

    public void setPlaces_to_polygons(Map<String, String> places_to_polygons) {
        this.places_to_polygons = places_to_polygons;
    }
}
