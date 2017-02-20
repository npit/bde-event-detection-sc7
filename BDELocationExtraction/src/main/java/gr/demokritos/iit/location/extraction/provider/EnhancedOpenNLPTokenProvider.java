/* Copyright 2016 NCSR Demokritos
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package gr.demokritos.iit.location.extraction.provider;


import gr.demokritos.iit.base.conf.BaseConfiguration;
import gr.demokritos.iit.base.conf.IBaseConf;
import gr.demokritos.iit.base.util.Utils;

import gr.demokritos.iit.location.factory.conf.ILocConf;

import gr.demokritos.iit.location.sentsplit.ISentenceSplitter;
import gr.demokritos.iit.location.sentsplit.OpenNLPSentenceSplitter;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.opengis.filter.expression.Add;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class EnhancedOpenNLPTokenProvider implements ITokenProvider {

    boolean useAdditionalSources, onlyUseAdditionalSources;
    private ISentenceSplitter sentenceSplitter;

    private final NameFinderME[] models;
    private final double prob_cutoff;

    protected static final double DEFAULT_PROB_CUTOFF = 0.80;
    protected static String DEFAULT_NE_MODELS_PATH = "./res/ne_models/";
    protected static String DEFAULT_SENT_SPLIT_MODEL_PATH = "./res/en-sent.bin";

    public static void setDefaultPaths(String modelsPath, String splitterPath)
    {
        DEFAULT_NE_MODELS_PATH = modelsPath;
        DEFAULT_SENT_SPLIT_MODEL_PATH = splitterPath;
    }

    // extra location names
    private HashSet<String> extraNames;
    // additional location names per (original) extra name
    private HashMap<String,ArrayList<String>> extraNamesAssociation;
    // overriding location names per (original) extra name
    private HashMap<String,String> extraNamesOverride;
    private Set<String> immutableNames;
    /**
     *
     * @param basePath the path where the models are located
     * @param sentenceSplitter
     * @param prob_cutoff
     * @throws IOException
     */
    public EnhancedOpenNLPTokenProvider(String basePath, ISentenceSplitter sentenceSplitter, double prob_cutoff) throws IOException {
        if (basePath == null) {
            basePath = DEFAULT_NE_MODELS_PATH;
        }
        File modelsBase = new File(basePath);
        if (!modelsBase.canRead()) {
            throw new IllegalArgumentException("provide models");
        }
        File[] mmodels = modelsBase.listFiles();
        this.models = new NameFinderME[mmodels.length];
        for (int i = 0; i < mmodels.length; i++) {
            this.models[i] = new NameFinderME(new TokenNameFinderModel(new FileInputStream(mmodels[i])));
        }
        this.sentenceSplitter = sentenceSplitter;
        this.prob_cutoff = prob_cutoff;

        useAdditionalSources = false;
        onlyUseAdditionalSources = false;



    }


    public boolean configure(ILocConf conf)
    {
        String extractorConf = conf.getLocationExtractorConfig();
        Properties props = new Properties();
        try {
            FileInputStream instream = new FileInputStream(new File(extractorConf));
            props.load(new InputStreamReader(instream, Charset.forName("UTF-8")));
        } catch (IOException e) {
            System.err.println("Locextractor file error: [" + extractorConf +"]");
            e.printStackTrace();
            return false;
        }
        if( ! BaseConfiguration.isTrue(props.getProperty("use_extras",""))) return true;

        extraNames = new HashSet<>();
        extraNamesAssociation = new HashMap<>();

        extraNamesOverride = new HashMap<>();
        String [] extraLocations = null;
        String value = props.getProperty("extra_locations","");
        if(value.isEmpty())
        {
            value = props.getProperty("extra_locations_file","");
            ArrayList<String> lines = Utils.readFileLinesDropComments(value);
            if(lines == null) return true;
            extraLocations = lines.toArray(new String[lines.size()]);
        }
        else
        {
            extraLocations = value.split(",");
        }
        if(value.isEmpty()) return true;


        for(String extraloc : extraLocations)
        {
            parseExtraLocationClause(extraloc,conf);
        }

        System.out.println("done.\n\tRead " + extraNames.size() + " additional location names.");
        System.out.println("\tRead " + extraNamesAssociation.size() + " location associations.");
        System.out.println("\tRead " + extraNamesOverride.size() + " location overrides.");


        useAdditionalSources = true;
        if(BaseConfiguration.isTrue(props.getProperty("only_use_extras","")))
            onlyUseAdditionalSources = true;

        // the default location extractor post-processes discovered names (for close duplicates, etc.). Disable it by
        // providing a list of immutable names
        immutableNames = new HashSet();
        for(String name : extraNames)
        {

            if(extraNamesOverride.containsKey(name)) name = (extraNamesOverride.get(name));
            immutableNames.add(name);
            if(extraNamesAssociation.containsKey(name)) immutableNames.addAll(extraNamesAssociation.get(name));
        }
    return true;
    }

    void parseExtraLocationClause(String extraloc, ILocConf conf)
    {
        String baseName="";
        String override="";
        ArrayList<String> additionals = new ArrayList<>();
        String metaData="";

        String [] nameAndMeta = extraloc.split("/"); // check for overrides

        // parse override
        if(nameAndMeta.length == 1)
        {
            //no override
            metaData = nameAndMeta[0];
        }
        else if(nameAndMeta.length == 2)
        {
            baseName = nameAndMeta[0].trim();
            metaData = nameAndMeta[1];
        }
        else
        {
            System.err.println("Skipping extra location clause [" + extraloc + "] : Encountered more than 2  overrides (slashes) ");
            System.err.println("Can have at most 1.");
            return;
        }
        // parse metadata : check additionals
        nameAndMeta = metaData.split("\\+");
        if(nameAndMeta.length == 1)
        {
            // no additionals
            if(baseName.isEmpty())
            {
                // no metadata at all
                baseName = metaData.trim();
            }
            else
            {
                // meta data is only an override
                override = metaData.trim();
            }
        }
        else
        {
            // additionals exist
            if(baseName.isEmpty())
            {
                // no overrides were encountered, so 1st token is the base name
                baseName = nameAndMeta[0].trim();
            }
            else
                override = (nameAndMeta[0]).trim();
            // all the rest are additionals
            for(int i=1;i<nameAndMeta.length;++i) additionals.add(nameAndMeta[i].trim());
        }
        if(conf.hasModifier(ILocConf.Modifiers.VERBOSE.toString()))
            System.out.println(baseName);
        boolean inserted = extraNames.add(baseName);

        if(! inserted) System.err.println("Duplicate location: " + baseName);
        if(! additionals.isEmpty())
        {
            for(String a : additionals)
                extraNames.add(a);
            extraNamesAssociation.put(override, additionals);
        }
        if(! override.isEmpty())
        {
            extraNames.add(override);           // add it to the directly matched
            extraNamesOverride.put(baseName, override);
        }

    }
    /**
     *
     * @param models the array of model files to load
     * @param sentenceSplitter
     * @param prob_cutoff
     * @throws IOException
     */
    public EnhancedOpenNLPTokenProvider(File[] models, ISentenceSplitter sentenceSplitter, double prob_cutoff) throws IOException {
        this.models = new NameFinderME[models.length];
        for (int i = 0; i < models.length; i++) {
            this.models[i] = new NameFinderME(new TokenNameFinderModel(new FileInputStream(models[i])));
        }
        this.sentenceSplitter = sentenceSplitter;
        this.prob_cutoff = prob_cutoff;
    }

    /**
     * Uses default hardcoded base path for models ('/res/ne_models/'), default
     * OpenNLPSentenceSplitter (for EN), and 0.80 probability cut_off threshold.
     *
     * @throws IOException
     */
    public EnhancedOpenNLPTokenProvider() throws IOException {
        this(DEFAULT_NE_MODELS_PATH, new OpenNLPSentenceSplitter(DEFAULT_SENT_SPLIT_MODEL_PATH), DEFAULT_PROB_CUTOFF);
    }

    @Override
    public synchronized Set<String> getTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }
        HashSet<String> ret = new HashSet();
        String[] sentences = sentenceSplitter.splitToSentences(text);
        for (String sentence : sentences) {
            sentence = sentence.replaceAll("([^Α-Ωα-ωa-zA-Z0-9άέίόώήύΐΪΊΆΈΏΌΉΎ. ])", " $1 ");
            if (sentence.charAt(sentence.length() - 1) == '.') {
                String[] words = sentence.split(" ");
                String lastWord = words[words.length - 1];
                if (!lastWord.substring(0, lastWord.length() - 1).contains(".")) {
                    sentence = sentence.substring(0, sentence.length() - 1);
                }
            }
            Iterator<String> iter = getTokenMap(sentence).keySet().iterator();

            while (iter.hasNext()) {
                String nameEntity = iter.next();
                ret.add(nameEntity);
            }
        }

        return ret;
    }


    private Set<String> AdditionalResultsPerArticle;
    public Set<String> getImmutableNames()
    {
        if(immutableNames==null) return new HashSet<>();
        return immutableNames;
    }

    private String processGreek(String word)
    {
        word = word.replaceAll("Α","A");

        return word;
    }
    @Override
    public Set<String> getLocationTokens(String text) {


        if(useAdditionalSources) {
            // compute extras
            AdditionalResultsPerArticle = new HashSet<>();
            searchForAdditionalLocations(text);

            if(onlyUseAdditionalSources)
            {
                AdditionalResultsPerArticle = applyOverrides(AdditionalResultsPerArticle);
                AdditionalResultsPerArticle = addAssociations(AdditionalResultsPerArticle);
                return AdditionalResultsPerArticle;
            }
        }

        Set<String> ret = new HashSet();
        if (text == null || text.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        String[] sentences = sentenceSplitter.splitToSentences(text);
        for (String sentence : sentences) {
            sentence = sentence.replaceAll("([^Α-Ωα-ωa-zA-Z0-9άέίόώήύΐΪΊΆΈΏΌΉΎ. ])", " $1 ");
            if (sentence.charAt(sentence.length() - 1) == '.') {
                String[] words = sentence.split(" ");
                String lastWord = words[words.length - 1];
                if (!lastWord.substring(0, lastWord.length() - 1).contains(".")) {
                    sentence = sentence.substring(0, sentence.length() - 1);
                }
            }
            Map<String, String> qq = getTokenMap(sentence);
            for (Map.Entry<String, String> entrySet : qq.entrySet()) {
                String ne = entrySet.getKey();
                String neType = entrySet.getValue();
                if (TYPE_LOCATION.equalsIgnoreCase(neType)) {
                    ret.add(ne);
                }
            }
        }

        if(useAdditionalSources)
        {
            ret.addAll(AdditionalResultsPerArticle);
            ret = applyOverrides(ret);
            ret = addAssociations(ret);
        }

        return ret;
    }


    void searchForAdditionalLocations(String text)
    {
        for(String extraName : extraNames) {

            // already processed
            if (AdditionalResultsPerArticle.contains(extraName)) continue;
            if(! validOccurence(extraName, text)) continue;
            AdditionalResultsPerArticle.add(extraName);
        }
    }
    Set<String> applyOverrides(Set<String> names)
    {
        Set<String> overridenNames = new HashSet<>();
        for(String name : names)
        {
            if(extraNamesOverride.containsKey(name)) overridenNames.add(extraNamesOverride.get(name));
            else overridenNames.add(name);
        }
        return overridenNames;
    }
    Set<String> addAssociations(Set<String> names)
    {
        Set<String> augmentedNames = new HashSet<>();

        for(String name : names)
        {
            augmentedNames.add(name);
            if(extraNamesAssociation.containsKey(name))
            {
                augmentedNames.addAll(extraNamesAssociation.get(name));
            }
        }
        return augmentedNames;
    }

    boolean validOccurence(String extraName, String text)
    {
        if(text.contains(extraName))
        {
            // if the extra name is space-delimited it will never be matched in a tokenization.
            // Search with contains, with restrictions before and after the word

            // check characters before and after
            int idx = text.indexOf(extraName);

            if(idx > 0) // word is NOT at the start of a sentence
                if(Character.isAlphabetic(text.charAt(idx-1)))
                    return false; // pre-word character is a letter : fail
            if(idx + extraName.length() < text.length() - 1) // word is not at the end of the sentence
                if(Character.isAlphabetic(text.charAt(idx + extraName.length())))
                    return false; // post-word  character is a letter : fail
            // if flow reaches this, all's good: add it
            return true;
        }
        else return false;
    }



    private static final String TYPE_LOCATION = "location";

    protected Map<String, String> getTokenMap(String text) {

//        String text_lowercase = new String(text).toLowerCase();
        if (text == null || text.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map<String, String> res = new HashMap();
        String[] ss = text.split("\\s+");

        if(!onlyUseAdditionalSources) {
            for (NameFinderME model : models) {
                Span[] found_spans = model.find(ss);
                int s = 0;
                for (Span span : found_spans) {
                    double prob = model.probs(found_spans)[s++];
                    if (prob >= prob_cutoff) {
                        int start = span.getStart();
                        int end = span.getEnd();
                        StringBuilder sb = new StringBuilder();
                        for (int i = start; i < end; i++) {
                            sb.append(ss[i]).append(" ");
                        }
                        String ne = sb.toString().trim();
                        String type = span.getType();
                        res.put(ne, type);
                    }
                }
            }
        }
        return res;
    }

}
