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

import gr.demokritos.iit.location.extraction.provider.EnhancedOpenNLPTokenProvider;
import gr.demokritos.iit.location.extraction.provider.ITokenProvider;
import gr.demokritos.iit.location.sentsplit.OpenNLPSentenceSplitter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DefaultLocationExtractor implements ILocationExtractor {

    /**
     * The interface for token extraction
     */
    protected ITokenProvider token_provider;

    /**
     *
     * @param token_provider the token provider interface
     */
    public DefaultLocationExtractor(ITokenProvider token_provider) {
        this.token_provider = token_provider;
    }

    @Override
    public Set<String> extractTokens(String document) {
        if (document == null || document.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        return clean(token_provider.getTokens(document));
    }

    private Set<String> clean(Set<String> s) {
        Set<String> ret = new HashSet();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            String token = iter.next().trim();
            String cleaned = token.replaceAll("[^Α-Ωα-ωa-zA-Z0-9άέίόώήύΐΪΊΆΈΏΌΉΎ. ]", "");
            if (cleaned.charAt(0) == '.') {
                cleaned = cleaned.substring(1);
            }

            if (cleaned.charAt(cleaned.length() - 1) == '.') {
                if (cleaned.substring(0, cleaned.length() - 2).contains(".") == false) {
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                }
            }
            ret.add(cleaned);
        }
        return removeNameEntitiesThatAreSimilar(ret);
    }

    private Set<String> removeNameEntitiesThatAreSimilar(Set<String> s) {
        if (s.size() <= 1) {
            return s;
        }
        Set<String> ret = new HashSet();
        String[] allEntities = s.toArray(new String[s.size()]);
        Boolean[] remove = new Boolean[s.size()];
        for (int i = 0; i < allEntities.length; i++) {
            remove[i] = false;
        }

        for (int i = 0; i < allEntities.length; i++) {
            for (int j = (i + 1); j < allEntities.length; j++) {
                String s1 = allEntities[i];
                String s2 = allEntities[j];
                if (distanceBetweenStrings(s1, s2) <= 2) {
                    int worse = getWorse(s1, s2);
                    if (worse == 1) {
                        remove[i] = true;
                    } else {
                        remove[j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < allEntities.length; i++) {
            if (!remove[i]) {
                ret.add(allEntities[i]);
            }
        }
        // force capitalization
        Set<String> res = new HashSet();
        for (String each : ret) {
            res.add(WordUtils.capitalizeFully(each));
        }
        return res;
    }

    private int distanceBetweenStrings(String s1, String s2) {
        int distance = 0;
        String[] components1 = s1.trim().split(" ");
        String[] components2 = s2.trim().split(" ");
        if (components1.length != components2.length) {
            return Integer.MAX_VALUE;
        }

        for (int j = 0; j < components1.length; j++) {

            String first = components1[j];
            String second = components2[j];
            int min = Math.min(first.length(), second.length());
            int max = Math.max(first.length(), second.length());
            for (int i = 0; i < min; i++) {
                if (first.charAt(i) != second.charAt(i)) {
                    distance++;
                }
            }
            distance += (max - min);
        }

        return distance;
    }

    private int getWorse(String s1, String s2) {
        int weight1 = 0;
        int weight2 = 0;

        char[] c1 = s1.toCharArray();
        char[] c2 = s2.toCharArray();

        for (int i = 0; i < c1.length; i++) {
            String c = "" + c1[i];
            if (c.matches("[άέίόώήύΐΪΊΆΈΏΌΉΎ]")) {
                weight1++;
            }
        }
        if (("" + c1[0]).matches("[Α-ΩA-ZΪΊΆΈΏΌΉΎ]")) {
            weight1++;
        }

        for (int i = 0; i < c2.length; i++) {
            String c = "" + c2[i];
            if (c.matches("[άέίόώήύΐΪΊΆΈΏΌΉΎ]")) {
                weight2++;
            }
        }
        if (("" + c2[0]).matches("[Α-ΩA-ZΪΊΆΈΏΌΉΎ]")) {
            weight2++;
        }

        if (s1.length() > s2.length()) {
            weight1++;
        } else if (s1.length() < s2.length()) {
            weight2++;
        }

        if (weight1 >= weight2) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public Set<String> extractLocation(String document) {
        if (document == null || document.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        return clean(token_provider.getLocationTokens(document));
    }

    /**
     *  for testing purposes alone
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        ITokenProvider tp = new EnhancedOpenNLPTokenProvider("../../NewSumV3/NewSumV3/dataEN/Tools/ne_models/", new OpenNLPSentenceSplitter("../../NewSumV3/NewSumV3/dataEN/Tools/en-sent.bin"), 0.8);
        ILocationExtractor ext = new DefaultLocationExtractor(tp);
        Set<String> tokenMap = ext.extractLocation("NEW YORK! - After years in the making, \"Deadpool\" hits cinema screens this week and fans of Marvel's anti-hero got to interact with the movie's cast at a special event in New York on Monday.\n"
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
                + "\"This is awesome. This is a whole world that I didn't know existed,\" she said. \"And my son LESLIE UGGAMS... He's a Deadpool fan, so for him, no matter what other stuff I did, this is the one.\"");
        System.out.println(tokenMap.toString());
    }
}
