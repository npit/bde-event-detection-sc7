package gr.demokritos.iit.location.mapping;

import org.apache.lucene.document.Document;

/**
 * Created by npittaras on 26/8/2016.
 */
class Location implements Constants {

    private final String city;
    private final String country;
    private final String region;
    private final String regionUnit;
    private final String geometry;

    public Location(Document document) {
        city = document.getField(FIELD_NAMES[3]).stringValue();
        country = document.getField(FIELD_NAMES[0]).stringValue();
        region = document.getField(FIELD_NAMES[1]).stringValue();
        regionUnit = document.getField(FIELD_NAMES[2]).stringValue();
        geometry = document.getField(FIELD_NAMES[4]).stringValue();

    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getGeometry() {
        return geometry;
    }

    public String getRegion() {
        return region;
    }

    public String getRegionUnit() {
        return regionUnit;
    }

    @Override
    public String toString() {
        return "Country : " + country + "\tRegion : " + region + "\tRegion Unit : " + regionUnit + "\tCity : " + city + "\tGeometry : " + geometry;
    }
}
