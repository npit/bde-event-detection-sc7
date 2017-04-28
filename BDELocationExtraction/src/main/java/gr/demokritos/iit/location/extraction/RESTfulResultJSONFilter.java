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
    private final String projectField = "project";
    private boolean AppendProject;
    // the filter in the conf file is specified as
    //  output_filter=category/fieldToGet/threshold



    List<String> Categories;
    Map<String,Double> ThreshPerCategory;
    Map<String,String> FieldPerCategory;
    Set<String> Entities;
    String Str;

    boolean Verbosity;
    @Override
    public boolean initialize(String str, boolean AppendProject, boolean verbose) {
        Verbosity = verbose;
        this.Str = str;
        this.AppendProject = AppendProject;
        Categories = new ArrayList<>();
        ThreshPerCategory= new HashMap<>();
        FieldPerCategory = new HashMap<>();
        Entities = new HashSet<>();
        // format: category/fieldname/scoreThreshold
        String [] parts = str.split(",");
        for(String catstr : parts)
        {

            String [] catparts = catstr.split(delimiter);
            try
            {
                String category = catparts[0];
                double thresh = Double.parseDouble(catparts[2]);
                String field = catparts[1];

                Categories.add(category);
                FieldPerCategory.put(category,field);
                ThreshPerCategory.put(category, thresh);
            }
            catch (Exception ex)
            {

                System.err.println("JSON output format expected: category" + delimiter +"fieldname" + delimiter + "scoreThreshold"+ delimiter + "type");
                System.err.println("Provided: " + catstr);
                ex.printStackTrace();
                return false;
            }
        }
        return true;
    }
    @Override
    public void filter(Object input)
    {

        ArrayList<String> data = (ArrayList<String>) input;
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
                        String entity = rawName;
                        if(AppendProject)
                        {
                            String projectField = (String) jarrayMember.get(this.projectField);
                            entity =projectField + "," +  entity;
                        }
                        Entities.add(entity);
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
    public Set<String> getEntities() {
        return this.Entities;

    }

    @Override
    public void clear() {
        Entities.clear();
    }

}
