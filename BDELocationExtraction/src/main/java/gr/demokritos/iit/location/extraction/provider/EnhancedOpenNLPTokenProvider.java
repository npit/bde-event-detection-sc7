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

import gr.demokritos.iit.location.factory.conf.ILocConf;
import gr.demokritos.iit.location.sentsplit.ISentenceSplitter;
import gr.demokritos.iit.location.sentsplit.OpenNLPSentenceSplitter;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.HashList;
import opennlp.tools.util.Span;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


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


    private HashMap<String,String> extraNames;
    private HashMap<String,ArrayList<String>> extraNamesAssociation;
    private Set<String> associationCache;
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

    private void readLocationsFile(String extrapath)
    {
        // special optional format:
        // name1***name2***name3 ...
        // In that scenario, if name1 exists in the text, then name2,name3 ...  will (also) be added as a location name.
        // this is helpful if name1 is too specific for a geometry to exist, so we manually add superegions for which
        // we know that a geometry is availabe

        String delimiter = "[*]{3}";

        try (BufferedReader br = new BufferedReader(new FileReader(extrapath))) {
            // read newline delimited source names file
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if(line.startsWith("#")) continue; // skip comments
                String[] parts = line.split(delimiter);

                if(!extraNames.keySet().contains(parts[0]))
                {
                    extraNames.put(parts[0],parts[0].toLowerCase());
                }
                if(parts.length > 1)
                {
                    extraNamesAssociation.put(parts[0], new ArrayList());

                    // add the associated places
                    for (int i = 1; i < parts.length; ++i) {
                        if (!extraNamesAssociation.get(parts[0]).contains(parts[i]))
                            extraNamesAssociation.get(parts[0]).add(parts[i]);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void configure(ILocConf conf)
    {
        if(! conf.useAdditionalExternalNames()) return;

        // naive additions from GPapadakis' dataset
        // get extra names

        extraNames = new HashMap<>();
        extraNamesAssociation = new HashMap<>();
        associationCache = new HashSet<>();
        String extrapath=conf.getLocationExtractionSourceFile();
        readLocationsFile(extrapath);
        System.out.print("Reading extra names file [" + extrapath + "].");

        System.out.println("done.\n\t Read " + extraNames.size() + " additional location names.");

        useAdditionalSources = true;
        if(conf.onlyUseAdditionalExternalNames())
            onlyUseAdditionalSources = true;
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

    @Override
    public Set<String> getLocationTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set<String> ret = new HashSet();
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
        return ret;
    }
    private static final String TYPE_LOCATION = "location";

    protected Map<String, String> getTokenMap(String text) {

        String text_lowercase = new String(text).toLowerCase();
        if (text == null || text.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map<String,String> additional_res = new HashMap<>();
        Map<String, String> res = new HashMap();
        String[] ss = text.split("\\s+");
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

            // check for existence of manually supplied location names
            if( useAdditionalSources )
            {
                // just check the text
                for(String extraName : extraNames.keySet())
                {
                    if(!additional_res.containsKey(extraName))
                    {
                        if (text_lowercase.contains(extraNames.get(extraName)))
                        {
                            additional_res.put(extraName, TYPE_LOCATION);
                            applyLocationAssociations(extraName,additional_res);


                        }
                        //else System.out.println("Does not contain " + extraNames.get(extraName));

                    }
                }
            }
        }
        if( useAdditionalSources )
        {
            if(onlyUseAdditionalSources)
                res.clear();
            res.putAll(additional_res);
        }

        return res;
    }


    private void applyLocationAssociations(String location, Map<String,String> additionalRes)
    {
        // the location argument was just added to the additional results set.

        // if there's no association for the location, done
        if( ! extraNamesAssociation.keySet().contains(location)) return;

        // if we've already processed this location, done
        // this is done to avoid cycles, if the user was dum - not careful enough to declare such
        if(associationCache.contains(location)) return;
        associationCache.add(location);
        // get associated locations
        ArrayList<String> assoc_locations = extraNamesAssociation.get(location);
        for(String loc : assoc_locations)
        {

            if (!additionalRes.containsKey(loc))
            {
                // add it if not already there
                additionalRes.put(location, TYPE_LOCATION);
                System.out.printf("\tAdded location association %s -> %s\n",location,loc);
            }
            // continue traversing the association chain
            applyLocationAssociations(loc,additionalRes);
        }

    }
}
