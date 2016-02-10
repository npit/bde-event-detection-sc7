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
package gr.demokritos.iit.location.extraction;

import gr.demokritos.iit.sentsplit.ISentenceSplitter;
import gr.demokritos.iit.sentsplit.OpenNLPSentenceSplitter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class NERTokenProvider implements ITokenProvider {

    private static final String TYPE_LOCATION = "location";

    private ISentenceSplitter sentenceSplitter;

    private final NameFinderME[] models;
    public static String DEFAULT_BASE_PATH = "./res/ne_models/";
    public static String DEFAULT_SENT_SPLIT_MODEL_PATH = "./res/en-sent.bin";

    private double probability_threshold;
    private static final double DEFAULT_PROBABILITY_THRESHOLD = 0.8;

    /**
     *
     * @param basePath the path where the models are located
     * @param sentenceSplitter
     * @param probability_threshold
     * @throws IOException
     */
    public NERTokenProvider(String basePath, ISentenceSplitter sentenceSplitter, double probability_threshold) throws IOException {
        if (basePath == null) {
            basePath = DEFAULT_BASE_PATH;
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
    }

    /**
     *
     * @param models the array of model files to load
     * @param sentenceSplitter
     * @param probability_threshold
     * @throws IOException
     */
    public NERTokenProvider(File[] models, ISentenceSplitter sentenceSplitter, double probability_threshold) throws IOException {
        this.models = new NameFinderME[models.length];
        for (int i = 0; i < models.length; i++) {
            this.models[i] = new NameFinderME(new TokenNameFinderModel(new FileInputStream(models[i])));
        }
        this.sentenceSplitter = sentenceSplitter;
        this.probability_threshold = probability_threshold;
    }

    /**
     * Uses default hardcoded base path for models ('./res/ne_models')
     *
     * @throws IOException
     */
    public NERTokenProvider() throws IOException {
        this(DEFAULT_BASE_PATH, new OpenNLPSentenceSplitter(DEFAULT_SENT_SPLIT_MODEL_PATH), DEFAULT_PROBABILITY_THRESHOLD);
    }

    @Override
    public synchronized Set<String> getTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set<String> ret = new HashSet();
        String[] sentences = sentenceSplitter.splitToSentences(text);
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            sentence = sentence.replaceAll("([^Α-Ωα-ωa-zA-Z0-9άέίόώήύΐΪΊΆΈΏΌΉΎ. ])", " $1 ");
            if (sentence.charAt(sentence.length() - 1) == '.') {
                String[] words = sentence.split(" ");
                String lastWord = words[words.length - 1];
                if (!lastWord.substring(0, lastWord.length() - 1).contains(".")) {
                    sentence = sentence.substring(0, sentence.length() - 1);
                }
            }
            System.out.println(sentence);
            java.util.Iterator<String> iter = getTokenMap(sentence).keySet().iterator();
            while (iter.hasNext()) {
                String nameEntity = iter.next();
                ret.add(capitalize(nameEntity));
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
            System.out.println(sentence);
            Map<String, String> qq = getTokenMap(sentence);
            for (Map.Entry<String, String> entrySet : qq.entrySet()) {
                String ne = entrySet.getKey();
                String neType = entrySet.getValue();
                if (TYPE_LOCATION.equalsIgnoreCase(neType)) {
                    ret.add(capitalize(ne));
                }
            }
        }
        return ret;
    }

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
                if (prob >= probability_threshold) {
                    int start = span.getStart();
                    int end = span.getEnd();
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        sb.append(ss[i]).append(" ");
                    }
                    String ne = sb.toString().trim();
                    String type = span.getType();
                    res.put(ne, type);
                    System.out.println("ne: " + ne + ", type: " + type);
                }
            }
        }
        return res;
    }

    protected String capitalize(String text) {
        return WordUtils.capitalizeFully(text);
    }

    public static void main(String[] args) throws IOException {

        ITokenProvider tp = new NERTokenProvider("../../NewSumV3/NewSumV3/dataEN/Tools/ne_models/", new OpenNLPSentenceSplitter("../../NewSumV3/NewSumV3/dataEN/Tools/en-sent.bin"), 0.8);

        Set<String> tokenMap = tp.getLocationTokens("NEW YORK - After years in the making, \"Deadpool\" hits cinema screens this week and fans of Marvel's anti-hero got to interact with the movie's cast at a special event in New York on Monday.\n"
                + "\n"
                + "Ryan Reynolds, Morena Baccarin, Ed Skrein star in the movie, which, with dark humor, violence and offensive language, has an \"R\" rating.\n"
                + "\n"
                + "\"I just felt like it was a character in a comic book universe that could occupy a space that no one else could and no one else could ever in the future as well,\" Reynolds, who plays Deadpool, said.\n"
                + "\n"
                + "\"So I was always really excited about it. He's able to operate in an 'X-Men' universe in a way which other characters could never do that. So I just liked this idea of a protagonist that's morally flexible.\"\n"
                + "\n"
                + "According to the movie's synopsis, the film tells the story of former Special Forces agent turned mercenary Wade Wilson, who undergoes a rogue experiment to treat his cancer.\n"
                + "\n"
                + "The operation leaves him scarred but also with powers that allow him to heal quickly and Wilson, soon Deadpool, seeks revenge on the man who carried out the experiment.\n"
                + "\n"
                + "\"There is a lot of Deadpool in me. I have a much better editing system in my mind then Deadpool does,\" Reynolds said.\n"
                + "\n"
                + "The movie comes with a legion of fans -- a surprise for Tony Award winner Leslie Uggams who plays \"Blind Al\".\n"
                + "\n"
                + "\"This is awesome. This is a whole world that I didn't know existed,\" she said. \"And my son ... He's a Deadpool fan, so for him, no matter what other stuff I did, this is the one.\"");
        System.out.println(tokenMap.toString());

    }

}
