package gr.demokritos.iit.location.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;


import com.vividsolutions.jts.geom.util.GeometryTransformer;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;


import com.vividsolutions.jts.geom.Geometry;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import gr.demokritos.iit.location.mapping.geojson.GeoJsonWriter;


import org.geotools.geojson.geom.GeometryJSON;
import org.json.simple.JSONObject;


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


        String out;
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
//        GeometryJSON gjsn = new GeometryJSON();
//        StringWriter writer2 = new StringWriter();
//        gjsn.write(geom,writer2);
//        out=writer2.toString();

//        System.out.println("Gotta delete one system. Better to drop geoJSONwriter, as it's 3 extra hard files " +
//                "and we alreay use geotools, ask efi");
//        System.out.println("jts geojsonWriter          :\n"+out);
//        System.out.println("geotools geometryJSON.write:\n" + out2);
//        System.out.println("-----------------------------_");

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
    public static String GeoJSONtoWKT(String input) throws IOException, org.json.simple.parser.ParseException
    {
        // initialize geojson reader and WKT writer
        GeometryJSON gjsn = new GeometryJSON();
        WKTWriter wktr = new WKTWriter();

        StringReader geomreader = new StringReader(input);
        Geometry geom = (gjsn.read(geomreader));
        StringWriter writer = new StringWriter();
        wktr.write(geom,writer);

        return writer.toString();
    }

        public static ArrayList<String> GeoJSONtoWKTList(String input) throws IOException, org.json.simple.parser.ParseException {

        ArrayList<String> out = new ArrayList<String>();
        GeometryJSON gjsn = new GeometryJSON();

        // parse JSON objects from the JSON array
        StringReader reader = new StringReader(input);
        JSONParser ps = new JSONParser();
        JSONArray jarray = (JSONArray) ps.parse(reader);

        // initialize WKT writer
        WKTWriter wktr = new WKTWriter();

        for (Object key : jarray)
        {
            JSONObject jkey = (JSONObject) key;
            jkey = GeometryFormatTransformer.filter(jkey);
            //if(jkey.containsKey("crs")) jkey.remove("crs");
            out. add(GeoJSONtoWKT(jkey.toString()));
        }
        return out;
    }


    /***
     *
     * @param id
     * @param title
     * @param date
     * @param locpoly
     * @return
     * @throws IOException
     * @throws ParseException
     */

    /*
    Expected format is

    {"id":"1","title":"test event","eventDate":"2016-02-25T17:48:49+0000","referenceDate":"2016-02-25T17:48:49+0000",
    "areas":[{"name":"Athens","geometry":{"type":"Polygon","coordinates":[[[35.31,25.3],[35.31,19.25],[41.09,19.25],[41.09,25.3],[35.31,25.3]]]}}]}


     */

    // popeye, meaning strabon
    public static String EventRowToStrabonJSON(String id, String title, String date, Map<String,String> locpoly) throws IOException, ParseException {
        JSONObject obj = new JSONObject();
        // date format  in the repository is like:
        // 2016-09-28T08:32+0000
        String sourceFormat = "yyyy-MM-dd'T'HH:mmZ";
        SimpleDateFormat sf = new SimpleDateFormat(sourceFormat);
        Date date_;
        try {
            date_ = sf.parse(date);
        } catch (java.text.ParseException e) {
            System.err.println("Parse date format exception.");
            e.printStackTrace();
            return "";
        }
        // strabon expects format
        //
        String targetFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
        sf.applyPattern(targetFormat);
        date = sf.format(date_);

        obj.put("id",id);
        obj.put("title",title);
        obj.put("eventDate",date);
        obj.put("referenceDate",date);

        JSONArray array = new JSONArray();
        for(String location : locpoly.keySet())
        {
            JSONObject area = new JSONObject();

            String wktgeometry= locpoly.get(location);
            String gjsonGeometry = GeometryFormatTransformer.WKTtoGeoJSON(wktgeometry);
            JSONObject geomobject = new JSONObject();
            try {
                geomobject = (JSONObject) new JSONParser().parse(gjsonGeometry);
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
            geomobject = GeometryFormatTransformer.filter(geomobject);
            area.put("name",location);
            area.put("geometry",geomobject);
            array.add(area);
        }
        obj.put("areas",array);
        String output = obj.toJSONString();
        return output;
    }
    private static final String [] undesirables={"crs"};
   public static JSONObject filter(JSONObject jsobj)
   {
        for(String undesirable : undesirables)
        {
            jsobj.remove(undesirable);
        }
       return jsobj;
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

    public static void main(String [] args)
    {
        String date = "2016-10-17T09:22+0000";
        String sourceFormat = "yyyy-MM-dd'T'HH:mmZ";
        SimpleDateFormat sf = new SimpleDateFormat(sourceFormat);
        Date date_;
        try {
            date_ = sf.parse(date);
        } catch (java.text.ParseException e) {
            System.err.println("Parse date format exception.");
            e.printStackTrace();
            return ;
        }
        // strabon expects format
        //
        String targetFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
        sf.applyPattern(targetFormat);
        date = sf.format(date_);
    }
}
