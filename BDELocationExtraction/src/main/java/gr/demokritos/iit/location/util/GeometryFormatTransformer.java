package gr.demokritos.iit.location.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Location;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import gr.demokritos.iit.location.mapping.geojson.GeoJsonWriter;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.GeoJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.json.simple.JSONObject;
import org.opengis.feature.Feature;


/**
 * Created by nik on 7/14/16.
 */
public class GeometryFormatTransformer {





    /**
     *
     * @param input a string with a WKT geometry, i.e.
     *              MULTIPOLYGON (((8.13125038146978 40.8618049621583, 8.17569065094 40.8618049621583, 8.17569065094 40.9723625183105,
     *              8.20874977111828 40.9723625183105, 8.20874977111828 41.1209716796876, 8.45872211456293 41.1209716796876,
     *              8.45872211456293 40.8618049621583, 8.46539497375488 40.8618049621583, 8.46539497375488 40.8794746398926,
     *               ...
     *              8.21875000000006 39.1965293884278)))
     * @return the geojson representation of the geometry
     * @throws ParseException
     * @throws IOException
     *
     *  used to convert the WKT geometries from the events table to geoJSON, so as to send
     *  them to strabon

     */
    public static String WKTtoGeoJSON(String input) throws ParseException, IOException {


        String out="";
        // read WKT to jts geometry
        WKTReader reader = new WKTReader();
        Geometry geom = reader.read(input);

        // init geo-writer
        StringWriter writer = new StringWriter();

        // alternative 1 : vividsolutions GEOJSON writer (downloaded hard copy)
        GeoJsonWriter gwriter= new GeoJsonWriter();
        gwriter.write(geom,writer);
        out = writer.toString();

        // alternative 2 : geotools
        GeometryJSON gjsn = new GeometryJSON();
        StringWriter writer2 = new StringWriter();
        gjsn.write(geom,writer2);
        String out2=writer2.toString();

        System.out.println("Gotta delete one system. Better to drop geoJSONwriter, as it's 3 extra hard files " +
                "and we alreay use geotools, ask efi");
        System.out.println("jts geojsonWriter          :"+out);
        System.out.println("geotools geometryJSON.write:" + out2);

        return out;
    }

    /***
     *
     * @param input a string containing a list of geoJSON geometries, i.e.
     *              [{"type":"Polygon","coordinates":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]},
     *              {"type":"Polygon","coordinates":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]}]
     * @return An string arraylist containing WKT geometries converted from the input gjson
     * @throws IOException
     * @throws org.json.simple.parser.ParseException
     */
    public static ArrayList<String> GeoJSONtoWKT(String input) throws IOException, org.json.simple.parser.ParseException {

        ArrayList<String> out = new ArrayList<String>();
        GeometryJSON gjsn = new GeometryJSON();

        // parse JSON objects from the JSON array
        StringReader reader = new StringReader(input);
        JSONParser ps = new JSONParser();
        JSONArray jarray = (JSONArray) ps.parse(reader);

        // initialize WKT writer
        StringWriter writer = new StringWriter();
        WKTWriter wktr = new WKTWriter();

        for (Object key : jarray)
        {

            StringReader geomreader = new StringReader(key.toString());

            Geometry geom = (gjsn.read(geomreader));
            wktr.write(geom,writer);
            out.add(writer.toString());
        }
        return out;
    }

    /**
     * Process an event row to a format that the event processing & change detection server expects
     * @param input The event row data
     * @return The formatted data
     */
    /*
    Expected format is

    {"id":"1","title":"test event","eventDate":"2016-02-25T17:48:49+0000","referenceDate":"2016-02-25T17:48:49+0000",
    "areas":[{"name":"Athens","geometry":{"type":"Polygon","coordinates":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]}}]}


     */

    // popeye, meaning strabon
    public static String EventRowToPopeyeProcess(ArrayList<String> input)
    {
        String output = "";
        ArrayList<String> out = new ArrayList<>();
        try {




            int index = 0;

            out.add("\"id\":\"" + input.get(index++) + "\"");
            out.add("\"title\":\"" + input.get(index++) + "\"");
            //typically, seconds are missing from the date, like 2016-05-23T08:27+0000
            String date = input.get(index++);
            final String targetDateFormat = "yyyy-mm-ddThh:mm:ss+zzzz";

            if (date.length() < targetDateFormat.length()) {
                //plug in zero seconds
                String[] tok = date.split("\\+");
                date = tok[0] + ":00+" + tok[1];
            }

            out.add("\"eventDate\":\"" + date + "\"");
            out.add("\"referenceDate\":null");

            String areasString = "";
            int numLocations = 0;
            while (index < input.size()) {
                if (numLocations++ > 0)
                    areasString += ",";
                areasString += "{"; // start object
                // loc. name
                String location = input.get(index++);
                // loc. coord
                String coord = input.get(index++);
                coord = GeometryFormatTransformer.geometryToPointList(coord);

                areasString += "\"name\":" + location + ",\"geometry\":" + "{";
                areasString += "\"type\":\"Polygon\",\"coordinates\":" + coord;
                areasString += "}"; // close geometry
                areasString += "}"; // close location object
            }
            out.add("\"areas\":[" + areasString + "]");
            assert index == input.size() : "Index - input list size mismatch";
        }
        catch(java.lang.IndexOutOfBoundsException exc)
        {
            System.out.println("Malformed input geometry to EventRowToPopeyeProcess.");
            System.out.println(exc.getMessage());
            exc.printStackTrace();
            return "";
        }
        for (int i=0;i<out.size();++i)
        {
            if(i>0 && i < out.size())
                output += ",";
            output += out.get(i);
        }


        return "{" + output + "}";
    }

    private static String geometryToPointList(String geom)
    {
        String out = "";
        String regex = "[()\"]";
        geom = geom.replaceAll(regex,""); // drop parentheses
        String [] values = geom.split("[,\\s]");
        boolean startPair = true;
        for(int s=0;s<values.length; ++s)
        {
            if (values[s].isEmpty()) continue;


            if (startPair)
            {
                // a pair begins
                out += "[";
                out += values[s] + ",";
                startPair = false;
            }
            else {
                out += " ";
                out += values[s];
                // a pair ends
                out += "]";
                if ( s != values.length -1) out +=",";
                startPair = true;
            }
        }
        return "[[" + out + "]]";
    }

    /**
     * Converts the input map to a single string, compatible for use in a CQL query, as a CQL map
     * @param locpoly  Map<String,String> of location names and geometries
     * @return  A string representation of the input
     */
    public static String LocationPolygonsToCQLString(Map<String,String> locpoly)
    {
        String result = "";
        int count = 0;
        for(String location : locpoly.keySet())
        {
            String geometry =locpoly.get(location);
            if(count++ > 0)
                result += ",";
            result += "'" +location+"':'"+geometry +"'";
        }
        return "{" + result + "}";
    }

    /**
     * Converts the input map to a string arraylist, with query data of a valid size (CQL map : 64K Limit)
     * @param locpoly  Map<String,String> of location names and geometries
     * @return  An arraylist-string representation of the input
     */
    public static ArrayList<String> LocationWKTPolygonsToCQLStrings(Map<String,String> locpoly)
    {
        String MapElement = "";
        int a = MapElement.getBytes().length;
        for(String locationName : locpoly.keySet())
        {
            String geometryStr = locpoly.get(locationName);

        }

        return null;
    }


}
