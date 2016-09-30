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
            else
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Polygon extraction failed for ["+loc+"]");

        }
        return res;
    }


    public Map<String,String> postProcessGeometries(Map<String,String> places_polygons)
    {
        return places_polygons;
    }

}
