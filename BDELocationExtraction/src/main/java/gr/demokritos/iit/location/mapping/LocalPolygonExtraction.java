package gr.demokritos.iit.location.mapping;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;



/**
 * Created by npittaras on 26/8/2016.
 */
public class LocalPolygonExtraction implements IPolygonExtraction {

    FuzzySearch fs;
    public LocalPolygonExtraction(String sourceFilePath)
    {
	System.out.println("Initializing local polygon extractor from dataset : " + sourceFilePath + ".");
        try {
            fs = new FuzzySearch(sourceFilePath);
        }
        catch(Exception e)
        {
            System.err.println("Failed to initialize LocalPolygonExtraction class");

            e.printStackTrace();
        }
    }
    /**
     * extract polygon for a location entry
     *
     * @param locationEntity
     * @return
     */
    public String extractPolygon(String locationEntity)
    {
        if (locationEntity == null || locationEntity.trim().isEmpty()) {
            return "";
        }
        String res="";
        try
        {
            Location L = fs.processQuery(locationEntity);
            //System.out.println("Processed query");

            //if (L != null)
            //    System.out.println(L.toString());
            res =  processRawGeometry(L);
        }
        catch(Exception ex)
        {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);

        }
        //System.out.println("Local pol-el returning geometry:");
        //System.out.println(res);
        return res;
    }

    String processRawGeometry(Location loc)
    {
        if (loc != null) {
            return  loc.getGeometry().toText(); // to Well Known Text
        }
        return "";
    }

    /**
     * extract polygon for a series of entries
     *
     * @param locationEntities
     * @return
     */
    public Map<String, String> extractPolygon(Collection<String> locationEntities)
    {
        Map<String, String> res = new HashMap();

        for (String loc : locationEntities) {
            String poly = extractPolygon(loc);
            if (poly != null && !poly.isEmpty()) {
                res.put(loc, poly);
            }
        }
        return res;
    }

    /**
     * Converts the location - geometry pair to a format that is easy to parse JSON from.
     * This is invoked just before merging locations into the events table.
     * @param input a Map<String,String> of location names and geometries
     * geometries are like
     * POLYGON ((72.5866317749023 45.2075157165528,
     *              72.5866317749023 45.1597290039063,
     *              72.556770324707 45.1597290039063,
     *              72.556770324707 45.2075157165528,
     *              72.5866317749023 45.2075157165528))
     * @return the modified Map
     */
    @Override
    public  Map<String, String> parseGeomJSON(Map<String, String> input)
    {



        Map<String,String> output =  new HashMap();
        for(String location : input.keySet())
        {
            //System.out.print("INitial : " + input.get(location));
            String geometry =input.get(location);
            if(geometry.contains("MULTIPOLYGON "))
            {
                geometry = geometry.replaceAll("MULTIPOLYGON ","");
                geometry = geometry.replaceAll("\\(\\(\\(","O"); // external par open
                geometry = geometry.replaceAll("\\)\\)\\)","C");// external par close

                geometry = geometry.replaceAll("\\(\\(","("); // internal par open
                geometry = geometry.replaceAll("\\)\\)",")");// internal par close

                geometry = geometry.replaceAll("O","(("); // external par open
                geometry = geometry.replaceAll("C","))");// external par close
            }
            else if(geometry.contains("POLYGON"))
            {
                geometry = geometry.replaceAll("POLYGON ","");
                geometry = geometry.replaceAll("\\(\\(","(");
                geometry = geometry.replaceAll("\\)\\)",")");
            }
            else
            {
                System.err.println("No [POLYGON] or [MULTIPOLYGON] prefix found in geometry.");
                return output;
            }





            output.put("\"" + location + "\"", "\"" + geometry + "\"");

        }



        return output;
    }
}
