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

import gr.demokritos.iit.location.sentsplit.ISentenceSplitter;
import gr.demokritos.iit.location.sentsplit.OpenNLPSentenceSplitter;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class EnhancedOpenNLPTokenProvider implements ITokenProvider {

    private ISentenceSplitter sentenceSplitter;

    private final NameFinderME[] models;
    private final double prob_cutoff;

    protected static final double DEFAULT_PROB_CUTOFF = 0.80;
    protected static String DEFAULT_NE_MODELS_PATH = "./res/ne_models/";
    protected static String DEFAULT_SENT_SPLIT_MODEL_PATH = "./res/en-sent.bin";

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
        if (text == null || text.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
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
        }
        return res;
    }
}
