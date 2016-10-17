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

    private Set<String> extraNames;
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

    public void configure(ILocConf conf)
    {
        if(! conf.useAdditionalExternalNames()) return;

        // naive additions from GPapadakis' dataset
        // get extra names
        extraNames = new HashSet<String>();

        String extrapath=conf.getLocationExtractionSourceFile();
        //read file into stream, try-with-resources
        System.out.print("Reading extra names file [" + extrapath + "].");
        try (BufferedReader br = new BufferedReader(new FileReader(extrapath))) {
            // read newline delimited source names file
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if(!extraNames.contains(line))
                    {
                        extraNames.add(line);
                    }

                //System.out.println("line : [" + line + "]");
                //String [] tokens = line.split("\\n");

                // keep all non-empties, but the last 2,
                // the last is the polygon
                // the second to last, is often corrupted
                // above fails. keep first 2/3?
//                for(int i=0;i<2; ++ i)
//                {
//                    if(tokens[i].isEmpty()) break; // no more data in row
//                    String lname = tokens[i].trim();
//                    if(!extraNames.contains(lname))
//                    {
//                       // System.out.println("\t" + count++ + " [" + lname+"]");
//                        extraNames.add(lname);
//                    }
//                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done, read " + extraNames.size() + " additional location names.");
        //Collections.sort(extraNames);

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


            if( useAdditionalSources ) {
                // additions from GPapadakis' dataset
                // iterate through each token
//                Set<String> added = new HashSet<>();
//                for(int idx=0;idx <ss.length; ++idx)
//                {
//                    String token = ss[idx];//.toLowerCase();
//                    if (token.isEmpty()) continue;
//                    if(extraNames.contains(token))
//                    {
//                        if (res.keySet().contains(token)) continue;
//                        res.put(token, TYPE_LOCATION);
//                        added.add(token);
//                    }
//                    if(added.size() > 0 ) {
//                        System.out.print("Added from extra  : {");
//                        for (String k : added) { System.out.println(k + " , "); }
//                        System.out.println("}");
//                    }
//                }
                // just check the text
                for(String extraname : extraNames) {
                    //if (text.contains(extraname) || text_lowercase.contains(extraname.toLowerCase()) || text.contains(extraname.toUpperCase())) {
                    String extranameTrimmed=extraname.trim();
                    if(!additional_res.containsKey(extranameTrimmed))
                    {
                        if (text_lowercase.contains(extraname.toLowerCase()))
                        {
                            additional_res.put(extraname.trim(), TYPE_LOCATION);
                        }

                    }

                }

            }

        }
        if( useAdditionalSources )
        {
            if(onlyUseAdditionalSources)
            {
                res.clear();
            }

            res.putAll(additional_res);
        }

        return res;
    }
}
