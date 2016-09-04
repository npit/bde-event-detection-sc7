package gr.demokritos.iit.location.mapping;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.lucene.document.Document;

/**
 *
 * @author G.A.P. II
 */

public class Location implements Constants, Comparable<Location> {

    private final double similarity;
    private String city;
    private String country;
    private String region;
    private String regionUnit;
    private final Geometry geometry;
    private final static WKTReader reader = new WKTReader();

    public Location(double sim, Document document) throws ParseException {
        similarity = sim;

        String segmentedLocation = document.getField(FIELD_NAMES[1]).stringValue();
        String[] parts = segmentedLocation.split(DATASET_DELIMITER);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        if (0 < parts.length && !parts[0].isEmpty()) {
            country = parts[0];
        }

        if (1 < parts.length && !parts[1].isEmpty()) {
            region = parts[1];
        }

        if (2 < parts.length && !parts[2].isEmpty()) {
            regionUnit = parts[2];
        }

        if (3 < parts.length && !parts[3].isEmpty()) {
            city = parts[3];
        }

        String geometryWKT = document.getField(FIELD_NAMES[2]).stringValue();
        geometry = reader.read(geometryWKT);
    }

    @Override
    public int compareTo(Location t) {
        Double thisArea = similarity * geometry.getArea();
        Double otherArea = t.getSimilarity() * t.getGeometry().getArea();
        return otherArea.compareTo(thisArea);
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public String getRegion() {
        return region;
    }

    public String getRegionUnit() {
        return regionUnit;
    }

    public double getSimilarity() {
        return similarity;
    }

    @Override
    public String toString() {
        return "Country : " + country + "\tRegion : " + region + "\tRegion Unit : " + regionUnit + "\tCity : " + city + "\tGeometry : " + geometry;
    }
}