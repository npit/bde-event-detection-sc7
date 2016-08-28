package gr.demokritos.iit.location.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


/**
 * Created by nik on 7/14/16.
 */
public class GeometryFormatTransformer {




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
}
