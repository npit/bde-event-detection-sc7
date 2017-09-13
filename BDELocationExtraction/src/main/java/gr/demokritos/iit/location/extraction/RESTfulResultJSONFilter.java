package gr.demokritos.iit.location.extraction;

import com.google.gson.JsonParseException;
import gr.demokritos.iit.base.util.ComparablePair;
import gr.demokritos.iit.base.util.Utils;
import gr.demokritos.iit.location.util.Pair;
import net.didion.jwnl.data.Verb;
import org.json.JSONStringer;
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
    private final String delimiter = "/"; // delimits nestes objects in the structure
    private final String delimiterSibling = "&"; // delimits sibling objects
    private final String EntityDelimiter = ",";
    private final String scoreField = "score";

    private static final String JOBJECT="\\{\\}";
    private static final String JARRAY="\\[\\]";
    private static final String PRIMITIVE=":";
    private static final String [] COMPARISONS={">=","<=","==",">","<"};

    private final String projectField = "project";
    private boolean AppendProject;

    private boolean StatusGood = true;
    // the filter in the conf file is specified as
    //  output_filter=category/fieldToGet/threshold

    @Override
    public boolean isStatusGood() {
        return StatusGood;
    }

    List<List<Pair<String,String>>> CategoriesLists; // fieldname, type
    Map<String,Pair<String,Pair<String,Double>>> ThreshPerCategory; // fieldname, thresname, thresval

    ArrayList<String> Entities;
    ArrayList<ArrayList<String>> EntityNames;
    ArrayList<Double> EntityScores;

    String Str;

    boolean Verbosity;
    @Override
    public boolean initialize(String str, boolean AppendProject, boolean verbose) {
        Verbosity = verbose;
        this.Str = str;
        this.AppendProject = AppendProject;
        CategoriesLists = new ArrayList<>();
        ThreshPerCategory= new HashMap<>();
        Entities = new ArrayList<>();
        EntityNames = new ArrayList<>();
        EntityScores = new ArrayList<>();


        // format: name1<type>threshname<threshval>/ ...
        // eg: person{}age10/children[]height180    // => from root jobj, get field person . if age < 10 continue. Get children field, it's an array. if height < 180 continue.
        String [] parts = str.split(",");
        for(String catstr : parts)
        {
            CategoriesLists.add(new ArrayList<Pair<String, String>>());

            String [] catparts = catstr.split(delimiter);
            for (String fieldparts : catparts)
            {
                String [] arr = null;
                String currCategory="";
                Pair<String,String> pair = new Pair<String,String>("","");

                pair = fieldIs(fieldparts,JOBJECT);
                if(pair == null)
                    pair = fieldIs(fieldparts,JARRAY);
                if(pair == null)
                    pair = fieldIs(fieldparts,PRIMITIVE);
                currCategory = pair.first();
                String rest = pair.second();

                if(rest.isEmpty()) continue;
                // numeric restriction
                Pair<String,Pair<String,Double>> threshComputation =
                        getNumericRestriction(rest);
                if(threshComputation == null) return false;
                ThreshPerCategory.put(currCategory,threshComputation);

            }
        }


        return true;
    }
    Pair<String,String> fieldIs(String arg,String type)
    {

        String ttype = type.replaceAll("\\\\","");
        if( ! arg.contains(ttype)) return null;
        String category = null;
        String []  arr = arg.split(type);

        category = arr[0];
        // handle multiple categories, maybe
        String []sibls = category.split(delimiterSibling);
        if(sibls.length > 1){
            for(String sibl : sibls) CategoriesLists.get(CategoriesLists.size()-1).add(new Pair<>(sibl, type));
        }
        else
            CategoriesLists.get(CategoriesLists.size()-1).add(new Pair<>(category,type));

        String str = "";
        for(int j=1;j < arr.length;++j) str += arr[j];
        return new Pair<> (category,str);
    }

    Pair<String,Pair<String,Double>> getNumericRestriction(String numstr)
    {
        for(String comp : COMPARISONS)
        {
            String [] arr = numstr.split(comp);
            if( ! numstr.contains(comp)) continue;
            String field = arr[0];
            double num = 0.0d;
            try {
                num = Double.parseDouble( arr[1]);
            }
            catch (NumberFormatException ex)
            {
                ex.printStackTrace();
                return null;
            }

            return new Pair<>(field,new Pair<>(comp,num));
        }
        return null;
    }
    @Override
    public void filter(Object input)
    {
        ArrayList<String> data = (ArrayList<String>) input;
        JSONParser parser = new JSONParser();

        for(String datum : data) {

            for(List<Pair<String,String>> Categories : CategoriesLists) {
                ArrayList<String> EntityNamesCurr = new ArrayList<>();
                try {
                    // get whole outer object
                    JSONObject outer_obj = (JSONObject) parser.parse(new StringReader(datum));
                    Object currentObject = outer_obj;

                    ArrayList<ArrayList<Object>> objectList = new ArrayList<>();
                    for (int j = 0; j < Categories.size() + 1; ++j) objectList.add(new ArrayList<Object>());

                    ArrayList<Double> scores = null; // scores for the final level, if any
                    int level = 0;
                    int maxLevel = Categories.size();
                    // loop over the field chain
                    while (true) {
                        // dismantle current level

                        // if array, break it up to next level
                        if (level < maxLevel) {
                            String field = Categories.get(level).first();
                            String type = Categories.get(level).second();
                            JSONObject currentJSONObject = (JSONObject) currentObject;
                            Object newObject = ((JSONObject) currentObject).get(field);
                            ArrayList<Object> objects = getJSONObjects(newObject, type);

                            Pair<ArrayList<Object>, ArrayList<Double>> objectsScores = restrict(currentJSONObject, objects, field, type);
                            objects = objectsScores.first();
                            scores = objectsScores.second();

                            objectList.get(level + 1).addAll(objects);
                            if (!objectList.get(level).isEmpty())
                                objectList.get(level).remove(currentObject);


                            if (objectList.get(level).isEmpty()) ++level;
                            if (!objectList.get(level).isEmpty())
                                currentObject = objectList.get(level).get(0);



                        } else {
                            // done!
                            ArrayList<Object> finalList = objectList.get(objectList.size() - 1);

                            if (scores == null) {
                                scores = new ArrayList<>();
                                for (Object o : finalList) scores.add(-1.0d);
                            }

                            for (int i = 0; i < finalList.size(); ++i) {

                                // get string values
                                Object o = finalList.get(i);
                                String str = "";
                                if (JSONObject.class.isInstance(o)) {
                                    str = o.toString();
//                                    Entities.put(str, scores.get(i));
                                    EntityNamesCurr.add(str);
                                    EntityScores.add(scores.get(i));

                                } else if (JSONArray.class.isInstance(o)) {
                                    for (Object oo : (JSONArray) o) {
                                        str = oo.toString();
//                                        Entities.put(str, scores.get(i));
                                        EntityNamesCurr.add(str);
                                        EntityScores.add(scores.get(i));
                                    }
                                } else {
                                    // just String
//                                    Entities.put((String) o, scores.get(i));
                                    EntityNamesCurr.add((String) o);
                                    EntityScores.add(scores.get(i));
                                }

                            }
                            break;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    this.setFail();
                    break;

                }
                EntityNames.add(EntityNamesCurr);
            }
            // fuse fields

            if(EntityNames.isEmpty()) continue;

            int numEntitiesPerDatum = EntityNames.get(0).size();
            if(numEntitiesPerDatum==0) continue;
            for(int j=0;j<numEntitiesPerDatum;++j){
                String compositeEntity = "";
                for(int i=0;i<EntityNames.size();++i){
                    if(!compositeEntity.isEmpty()) compositeEntity +=EntityDelimiter;
                    compositeEntity += EntityNames.get(i).get(j);
                }

                Entities.add(compositeEntity);

            }

        }

//                for(String category : Categories) {
//                    // get array of available categories
//                    JSONArray categoryArray = this.getJSONObjects(obj,category);
//                    if(categoryArray == null) continue;
//                    // one level down, on the fields of the category
//                    for(Object armem : categoryArray)
//                    {
//
//                        JSONObject jarrayMember = (JSONObject) armem;
//                        String fieldToGet = FieldPerCategory.get(category);
//                        Object rawName = (String) jarrayMember.get(fieldToGet);
//
//                        if(Verbosity) System.out.print("\t" + category + " - " + rawName);
//                        // apply the score filter
//                        double score = -1.0d;
//                        if(jarrayMember.containsKey(scoreField) && ThreshPerCategory.containsKey(category))
//                        {
//                            score = (Double)jarrayMember.get(scoreField);
//                            if(Verbosity) System.out.print(" - " + score);
//                            if(score < this.ThreshPerCategory.get(category))
//                            {
//                                if(Verbosity) System.out.println(" : dropping due to threshold " + ThreshPerCategory.get(category));
//                                continue;
//                            }
//                            if(Verbosity) System.out.println();
//                        }
//                        String entity = rawName;
//                        if(AppendProject)
//                        {
//                            String projectField = (String) jarrayMember.get(this.projectField);
//                            entity =projectField + "," +  entity;
//                        }
//                        Entities.put(entity, score);
//                    }
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void setFail(){
        StatusGood = false;
    }


    Pair<ArrayList<Object>,ArrayList<Double>> restrict(JSONObject current, ArrayList<Object> obj, String field, String type)
    {
        if( ! ThreshPerCategory.containsKey(field)) return new Pair<>(obj,null);


        Pair<String,Pair<String,Double>> pp = ThreshPerCategory.get(field);
        String threshfield = pp.first();
        String comp = pp.second().first();
        double  threshval =pp.second().second();
        ArrayList<Object> newlist  = new ArrayList<>();
        ArrayList<Double> scores = new ArrayList<>();

        if(type == PRIMITIVE)
        {
            double val = (double) current.get(threshfield);
            if(compare(comp,val,threshval))
            {
                newlist.add(current);
                scores.add(val);
            }
            return new Pair<>(newlist,scores);
        }

        for(Object o : obj)
        {
            JSONObject jo = (JSONObject) o;

            double val = (double) jo.get(threshfield);
            if(compare(comp,val,threshval))
            {
                newlist.add(o);
                scores.add(val);
            }

        }
        return new Pair<>(newlist,scores);

    }
    boolean compare(String op, double val1, double val2) {
        if (op.equals(">")) return val1 > val2;
        if (op.equals("<")) return val1 < val2;
        if (op.equals(">=")) return val1 >= val2;
        if (op.equals("<=")) return val1 <= val2;
        if (op.equals("==")) return val1 == val2;
        return false;
    }
    ArrayList<Object> getJSONObjects(Object obj, String type)
    {
        // try to get array
        ArrayList<Object> olist= new ArrayList<>();
        if(type == JARRAY)
        {
            JSONArray jarr = (JSONArray) obj;
            for(Object o : jarr)
            {
                olist.add(o);
            }
        }
        else
            olist.add(obj);

        return olist;
    }

    @Override
    public String toString()
    {
        return Str;
    }


    @Override
    public List<String> getEntities() {
        // sort the entities by score value
        // scores will be duplicated, we can keep the first copy only
        while(EntityScores.size() > Entities.size()) EntityScores.remove(EntityScores.size()-1);
        List<ComparablePair<?,?extends Comparable>> sorted = Utils.sort_corresponding(Entities, EntityScores);
        if (sorted == null){
            System.err.println("Failed to sort entity - scores lists!");
            return new ArrayList<>();
        }

        for (ComparablePair<?, ? extends  Comparable> p : sorted)
        {
            Entities.add((String) p.getData()); Entities.remove(0);
        }


        return Entities;
    }

    @Override
    public void clear() {
        Entities.clear();
        EntityNames.clear();
        EntityScores.clear();
    }


    public static void main(String [] args)
    {
        String str = "{\"freeTerms\":[{\"textValue\":\"new\",\"score\":99,\"frequencyInDocument\":10},{\"textValue\":\"north\",\"score\":91,\"frequencyInDocument\":10},{\"textValue\":\"kim\",\"score\":71,\"frequencyInDocument\":7},{\"textValue\":\"missile\",\"score\":70,\"frequencyInDocument\":10},{\"textValue\":\"north korea says leader\",\"score\":61,\"frequencyInDocument\":2},{\"textValue\":\"leader kim supervised test\",\"score\":61,\"frequencyInDocument\":2},{\"textValue\":\"leader\",\"score\":61,\"frequencyInDocument\":6},{\"textValue\":\"korea says leader kim\",\"score\":61,\"frequencyInDocument\":2},{\"textValue\":\"test of new anti-ship\",\"score\":60,\"frequencyInDocument\":2},{\"textValue\":\"supervised test of new\",\"score\":60,\"frequencyInDocument\":2},{\"textValue\":\"korea\",\"score\":60,\"frequencyInDocument\":8},{\"textValue\":\"leader kim\",\"score\":58,\"frequencyInDocument\":5},{\"textValue\":\"|\",\"score\":57,\"frequencyInDocument\":4},{\"textValue\":\"test\",\"score\":54,\"frequencyInDocument\":8},{\"textValue\":\"KCNA\",\"score\":53,\"frequencyInDocument\":6},{\"textValue\":\"north korean leader kim\",\"score\":51,\"frequencyInDocument\":2},{\"textValue\":\"korean leader kim jong\",\"score\":51,\"frequencyInDocument\":2},{\"textValue\":\"korean\",\"score\":49,\"frequencyInDocument\":5},{\"textValue\":\"anti-ship\",\"score\":44,\"frequencyInDocument\":4},{\"textValue\":\"supervise\",\"score\":42,\"frequencyInDocument\":3},{\"textValue\":\"launch\",\"score\":41,\"frequencyInDocument\":6},{\"textValue\":\"anti-ship missile\",\"score\":33,\"frequencyInDocument\":3},{\"textValue\":\"new anti-ship\",\"score\":33,\"frequencyInDocument\":3},{\"textValue\":\"new anti-ship missile\",\"score\":33,\"frequencyInDocument\":3},{\"textValue\":\"rocket\",\"score\":33,\"frequencyInDocument\":4}],\"concepts\":[{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/1873107@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":100.0,\"frequencyInDocument\":8,\"uri\":\"http://semantic-web.at/geo/1873107\",\"language\":\"en\",\"prefLabel\":\"Democratic People’s Republic of Korea\",\"altLabels\":[\"Korea, Democratic People's Republic Of\",\"Democratic People’s Republic of Korea\",\"North Korea\"],\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]},{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/1252563@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":25.0,\"frequencyInDocument\":5,\"uri\":\"http://semantic-web.at/geo/1252563\",\"language\":\"en\",\"prefLabel\":\"Ha\",\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]},{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/1835841@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":24.0,\"frequencyInDocument\":2,\"uri\":\"http://semantic-web.at/geo/1835841\",\"language\":\"en\",\"prefLabel\":\"Republic of Korea\",\"altLabels\":[\"South Korea\",\"Korea, Republic Of\",\"Republic of Korea\"],\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]},{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/1871859@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":22.0,\"frequencyInDocument\":2,\"uri\":\"http://semantic-web.at/geo/1871859\",\"language\":\"en\",\"prefLabel\":\"Pyongyang\",\"altLabels\":[\"Pyongyang\"],\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]},{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/1861060@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":7.0,\"frequencyInDocument\":1,\"uri\":\"http://semantic-web.at/geo/1861060\",\"language\":\"en\",\"prefLabel\":\"Japan\",\"altLabels\":[\"Japan\"],\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]},{\"id\":\"1DE00F04-C1B9-0001-404F-52256800BE20:http://semantic-web.at/geo/6252001@en\",\"project\":\"1DE00F04-C1B9-0001-404F-52256800BE20\",\"score\":3.0,\"frequencyInDocument\":1,\"uri\":\"http://semantic-web.at/geo/6252001\",\"language\":\"en\",\"prefLabel\":\"United States\",\"altLabels\":[\"United States\",\"United States of America\",\"America\"],\"conceptSchemes\":[{\"uri\":\"http://semantic-web.at/geo/100000000\",\"title\":\"Geonames Thesaurus\"}]}]}";
    }

}
