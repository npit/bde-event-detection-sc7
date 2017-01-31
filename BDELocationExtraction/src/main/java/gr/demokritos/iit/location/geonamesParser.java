package gr.demokritos.iit.location;
import gr.demokritos.iit.base.util.Utils;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nik on 1/27/17.
 */
public class geonamesParser {
    public static void main(String [] args) {

        try {
            parseLocations();
            parseCountries();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
        This method parses city location names from a file structured as the cities_N.txt format of geonames
        Preset to extract greek - english city name pairs
     */
    static void parseLocations()  throws IOException
    {
        System.out.println("Parsing locations...");
        String infile="/home/nik/work/iit/geonamesGreekLocations/cities15000.txt";
        String outfile="/home/nik/work/iit/geonamesGreekLocations/cities_15K_geonames_GR.txt";

        ArrayList<String> lines = Utils.readFileLinesDropComments(infile);
        ArrayList<ArrayList<String>> greekNames = new ArrayList<>();
        ArrayList<String> standardNames = new ArrayList<>();
        ArrayList<String> asciiNames = new ArrayList<>();

        String regex = "\\p{InGreek}";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);


        // geo format is:
        // TAB-delimited
        // id  name asciiname(no diacritics)
        for(String line : lines)
        {
            greekNames.add(new ArrayList<String>());
            String [] parts = line.split("\t");
            // 4th col is comma separated alternate names, in which there are greek
            standardNames.add(parts[1]);
            asciiNames.add(parts[2]);
            String alternateNames = parts[3];
            String [] alternates = alternateNames.split(",");
            for(String alt : alternates)
            {
                Matcher m = pattern.matcher(alt);
                if(m.find())
                {
                    greekNames.get(greekNames.size()-1).add(alt);
                }
            }

        }
        BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
        // keep just the 1st greek name for simplicity, and the simplified eng name (ascii)
        Map<String,Set<String>> names = new HashMap<>();

        for(int i=0;i<greekNames.size();++i)
        {
            if(greekNames.get(i).isEmpty()) continue;
            String name = greekNames.get(i).get(0);
            String asciiname = asciiNames.get(i);
            if(!names.containsKey(name))
            {
                names.put(name,new HashSet<String>());

            }
            names.get(name).add(asciiname);
        }
        for(String name : names.keySet())
        {
            if(names.get(name).isEmpty()) continue;

            wr.write(name + " / ");
            int count = 0;
            for(String engname : names.get(name))
            {
                if( count++ > 0 ) wr.write(" + ");
                wr.write(engname);
            }
            wr.write("\n");
            //for(String engname : names.get(name)) wr.write(engname + "\n");
        }
        wr.close();
        return;
    }
    // design to parse a (manually corrected) set of country names in the format
    // countryNameEng\t NameLang1Eng - NameLang1Native, NameLang2Eng - NameLang2Native (CountryName)
    // For example
    // Afghanistan 	Æfganisthanaya - ඇෆ්ගනිස්ථානය (Sinhala), Afeganistão (Portuguese), [...]
    // Åland Islands 	Ahvenamaa (Estonian), Ahvenanmaa (Finnish), Áland (Faroese), [...]

    // set to extract greek - english name pairs
    // https://en.wikipedia.org/wiki/List_of_country_names_in_various_languages_(A%E2%80%93C)
    static void parseCountries()  throws IOException
    {
        System.out.println("Parsing countries...");
        String infile="/home/nik/work/iit/geonamesGreekLocations/countryNamesWiki";
        String outfile="/home/nik/work/iit/geonamesGreekLocations/countries_wikipedia_GR.txt";

        ArrayList<String> lines = Utils.readFileLinesDropComments(infile);

        ArrayList<String> greekNames = new ArrayList<>();
        ArrayList<ArrayList<String>> engNames = new ArrayList<>();

        for(String line : lines)
        {
            //System.out.println("line [" + line + "]");
            String [] parts = line.split("\t");
            String basename = parts[0].trim();

            String alternatives = parts[1];
            if(! alternatives.contains("Greek"))continue;
            String [] alternates = alternatives.split(",");

            for(String alt : alternates)
            {
                if(! alt.contains("(Greek)")) continue;
                String [] dashSplit = alt.split("-");
                dashSplit = dashSplit[dashSplit.length-1].split("\\(Greek\\)");
                alt = dashSplit[0].trim();

                if(greekNames.contains(alt)) engNames.get(greekNames.indexOf(alt)).add(basename);
                else {
                    engNames.add(new ArrayList<String>());
                    engNames.get(engNames.size()-1).add(basename);
                    greekNames.add(alt);
                    break;
                }
                //System.out.print( alt + " - " );
            }

            //System.out.println(basename);
        }

        BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
        for(int i=0;i<greekNames.size();++i)
        {
            String greek = (greekNames.get(i).isEmpty()) ? "" : greekNames.get(i);

            if(greek.isEmpty()) continue;

            wr.write(greek + " / " + engNames.get(i).get(0));
            for(int j=1; j < engNames.get(i).size();++j) wr.write(" + " + engNames.get(i).get(j));
            wr.write("\n");
            //for(int j=0; j < engNames.get(i).size();++j) wr.write(engNames.get(i).get(j) + "\n");

        }
        wr.close();
        return;
    }

}
