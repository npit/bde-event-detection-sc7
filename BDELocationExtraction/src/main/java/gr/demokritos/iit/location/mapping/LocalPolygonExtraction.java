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
import gr.demokritos.iit.location.util.GeometryFormatTransformer;
import org.apache.lucene.document.Document;

/**
 * Created by npittaras on 26/8/2016.
 */
public class LocalPolygonExtraction implements IPolygonExtraction {

    FuzzySearch fs;
    public LocalPolygonExtraction(String sourceFilePath)
    {
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
            System.out.println(L.toString());
            res =  processRawGeometry(L);
        }
        catch(Exception ex)
        {
            Logger.getLogger(DefaultPolygonExtraction.class.getName()).log(Level.SEVERE, null, ex);

        }
        System.out.println("Local pol-el returning geometry:");
        System.out.println(res);
        return res;
    }

    String processRawGeometry(Location loc)
    {
        if (loc != null) {

            String geom = loc.getGeometry().replaceAll("[^0-9.]", " ");
            Scanner sc = new Scanner(geom);
            Coordinate[] newPolygonCoords = new Coordinate[5];
            Point2D leftPoint = new Point2D.Double(sc.nextDouble(), sc.nextDouble());
            Point2D rightPoint = new Point2D.Double(sc.nextDouble(), sc.nextDouble());
            sc.close();

            newPolygonCoords[0] = new Coordinate(leftPoint.getX(), rightPoint.getY());
            newPolygonCoords[1] = new Coordinate(leftPoint.getX(), leftPoint.getY());
            newPolygonCoords[2] = new Coordinate(rightPoint.getX(), leftPoint.getY());
            newPolygonCoords[3] = new Coordinate(rightPoint.getX(), rightPoint.getY());
            newPolygonCoords[4] = new Coordinate(leftPoint.getX(), rightPoint.getY());

            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon geometry = geometryFactory.createPolygon(newPolygonCoords);
            WKTWriter writer = new WKTWriter();
            System.out.println("parsed : " + writer.write(geometry));
            return geometry.toString();
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
            System.out.print("INitial : " + input.get(location));
            String geometry =input.get(location);
            assert geometry.contains("POLYGON") : "No [POLYGON] found in geometry.";

            geometry = geometry.replaceAll("POLYGON ","");
            geometry = geometry.replaceAll("\\(\\(","(");
            geometry = geometry.replaceAll("\\)\\)",")");


            output.put("\"" + location + "\"", "\"" + geometry + "\"");

        }
        return output;
    }
}
