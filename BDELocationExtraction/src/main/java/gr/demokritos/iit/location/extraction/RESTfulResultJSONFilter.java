package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.base.util.Utils;
import net.didion.jwnl.data.Verb;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Created by nik on 2/15/17.
 */
public class RESTfulResultJSONFilter implements IRestfulFilter{
    private final String delimiter = "/";
    private final String scoreField = "score";

    List<String> Categories;
    Map<String,Double> ThreshPerCategory;
    Map<String,String> FieldPerCategory;
    String Str;

    Map<String,Set<String>> Entities;
    Map<String,Set<String>> CategoriesPerType;
    boolean Verbosity;
    @Override
    public boolean initialize(String str, boolean verbose) {
        Verbosity = verbose;
        this.Str = str;
        Categories = new ArrayList<>();
        ThreshPerCategory= new HashMap<>();
        FieldPerCategory = new HashMap<>();
        CategoriesPerType = new HashMap<>();
        Entities = new HashMap<>();
        for(EntityType ent : IRestfulFilter.EntityType.values())
        {
            Entities.put(ent.toString(),new HashSet<String>());
        }
        // format: category/fieldname/scoreThreshold/type
        String [] parts = str.split(",");
        for(String catstr : parts)
        {

            String [] catparts = catstr.split(delimiter);
            try
            {
                String category = catparts[0];
                double thresh = Double.parseDouble(catparts[2]);
                String field = catparts[1];
                String type = catparts[3];
                if( ! EntityType.supports(type))
                {
                    System.err.println("RESTful filter does not support entity : " + type);
                    System.err.println("Available are : " + Arrays.asList(IRestfulFilter.EntityType.values()));

                    return false;
                }

                Categories.add(category);
                FieldPerCategory.put(category,field);
                ThreshPerCategory.put(category, thresh);
                if(!CategoriesPerType.containsKey(type)) CategoriesPerType.put(type, new HashSet<String>());
                CategoriesPerType.put(type, new HashSet<String>());
                CategoriesPerType.get(type).add(category);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                System.err.println("JSON output format expected: category" + delimiter +"fieldname" + delimiter + "scoreThreshold"+ delimiter + "type");
                System.err.println("Provided: " + catstr);
                return false;
            }
        }
        return true;
    }
    @Override
    public void filter(Object input)
    {

        for(String key : Entities.keySet())
        {
            Entities.get(key).clear();
        }
        ArrayList<String> data = (ArrayList<String>) input;
        Set<String> res = new HashSet<>();
        JSONParser parser = new JSONParser();

        for(String datum : data) {
            try {

                JSONObject obj = (JSONObject) parser.parse(new StringReader(datum));
                for(String category : Categories) {
                    JSONArray categoryArray = (JSONArray) obj.get(category);
                    if(categoryArray == null) continue;
                    for(Object armem : categoryArray)
                    {
                        JSONObject jarrayMember = (JSONObject) armem;
                        String fieldToGet = FieldPerCategory.get(category);
                        String rawName = (String) jarrayMember.get(fieldToGet);

                        if(Verbosity) System.out.print("\t" + category + " - " + rawName);
                        // apply the score filter
                        if(jarrayMember.containsKey(scoreField))
                        {
                            double score = (Double)jarrayMember.get(scoreField);
                            if(Verbosity) System.out.print(" - " + score);
                            if(score < this.ThreshPerCategory.get(category))
                            {
                                if(Verbosity) System.out.println(" : dropping due to threshold " + ThreshPerCategory.get(category));
                                continue;
                            }
                            if(Verbosity) System.out.println();
                        }



                        // if location, add to res, else add to entities
                        if(CategoriesPerType.get(EntityType.GENERIC.toString()).contains(category))
                        {
                            if(Verbosity) System.out.println("\t >>> Adding " + rawName + " to entities");
                            Entities.get(EntityType.GENERIC.toString()).add(rawName);
                        }
                        else if(CategoriesPerType.get(EntityType.LOCATION.toString()).contains(category))
                        {
                            if(Verbosity) System.out.println("Adding " + rawName + " to locations");
                            Entities.get(EntityType.LOCATION.toString()).add(rawName);                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public String toString()
    {
        return Str;
    }

    @Override
    public Set<String> getEntityType(EntityType ent) {
        return Entities.get(ent.toString());
    }

}
