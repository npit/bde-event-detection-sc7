package gr.demokritos.iit.location.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nik on 7/14/16.
 */
public class GeometryFormatTransformer {


    /**
     * Converts the location - geometry pair to a format that is easy to parse JSON from
     * @param input a Map<String,String>
     * @return the modified Map
     */
    public static Map<String, String> ToJSON(Map<String, String> input)
    {
        Map<String,String> output =  new HashMap();
        for(String location : input.keySet())
        {
            String geometry =input.get(location);
            String [] tokens = geometry.split(":|,");
            // Token order should be
            // type, TYPE,
            // coordinates, C1,C2, ... , c10, with brackets around value pairs
            // [{"type":"Polygon","coordinates":[[[9.61845970153809,48.3653259277344],
            // [9.61845970153809,48.2986068725587],[9.82220172882086,48.2986068725587],
            // [9.82220172882086,48.3653259277344],[9.61845970153809,48.3653259277344]]]}]
            // TODO: need to ask E-Karr @ di uoa on the formats the geometries are guranteed to take
            // TODO: ask G-Stam @ di uoa if he indeed does not need the type
            // (how many coordinates, any other special characters, etc
            String GeometryType = tokens[1];
            assert tokens[0].contains("type") :  GeometryFormatTransformer.class.toString() + ":ToJSON: Expected \"type\" at the first token position.";
            String coordinates="";
            boolean atPair = false;
            String replregex="[\\[\\]\\{\\}]";
            for(int i=3; i<tokens.length; ++i)
            {
                String coord = tokens[i].replaceAll(replregex,"");
                coordinates += coord;
                if (atPair && i < tokens.length-1) {
                    coordinates += ", ";
                }
                else coordinates += " ";
                atPair = !atPair;

            }
            assert !coordinates.isEmpty() :  GeometryFormatTransformer.class.toString() + ":ToJSON: No coordinates parsed.";
            output.put("\"" + location + "\"", "\"(" + coordinates + ")\"");

        }
        return output;
    }
    public static String LocatonPolygonsToCQLString(Map<String,String> locpoly)
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
}
