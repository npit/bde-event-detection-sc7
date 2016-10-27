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
package gr.demokritos.iit.crawlers.twitter.utils;

import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import gr.demokritos.iit.crawlers.twitter.structures.SearchQuery;
import gr.demokritos.iit.crawlers.twitter.structures.SourceAccount;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class QueryLoader {

    /**
     * the format of the file accepted is query***iso_code***max_results per
     * line. All queries must have an iso_code, indicating the language to
     * perform the search max_results can be omitted in the file.
     *
     * @param sPathToFile The absolute link to the file where the sources are
     * saved
     * @param sDelimiterType
     * @param default_result_threshold
     * @return the queries
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Set<SearchQuery> LoadQueriesFromFile(String sPathToFile, String sDelimiterType, Integer default_result_threshold)
            throws FileNotFoundException, IOException {
        if (sDelimiterType == null || sDelimiterType.trim().isEmpty()) {
            sDelimiterType = "[*]{3}";
        }
        File fFile = new File(sPathToFile);
        if (!fFile.exists()) {
            throw new FileNotFoundException(fFile.getAbsolutePath() + " cannot be found.");
        }
        if (fFile.canRead()) {
            FileInputStream fstream = new FileInputStream(fFile);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("utf-8")));
            String sLine;
            Set<SearchQuery> res = new HashSet();
            while ((sLine = br.readLine()) != null) {
                // if is not a comment line
                if (!sLine.startsWith("?")) {
                    String[] params = sLine.split(sDelimiterType);
                    SearchQuery sQcur = null;
                    if (params.length == 3) {
                        sQcur = new SearchQuery(
                                params[0],
                                params[1],
                                Integer.parseInt(params[2])
                        );
                    } else if (params.length == 2) {
                        if (default_result_threshold != null && default_result_threshold != 0) {
                            sQcur = new SearchQuery(
                                    params[0],
                                    params[1],
                                    default_result_threshold
                            );
                        } else {
                            sQcur = new SearchQuery(
                                    params[0],
                                    params[1]
                            );
                        }
                    } else if (params.length == 1) {
                        LOGGER.severe(String.format("'%s' does not have a language declaration, aborting", sLine));
                    }
                    if (sQcur != null) {
                        res.add(sQcur);
                    }
                }
            }
            in.close();
            return res;
        } else {
            LOGGER.log(Level.SEVERE, "Unable To Read From File {0}", fFile.getName());
            return null;
        }
    }

    public static Set<SourceAccount> loadAccounts(String sourceFile, String sDelimiterType, Boolean defaultAccountStatus)
            throws FileNotFoundException, IOException {
        if (sDelimiterType == null || sDelimiterType.trim().isEmpty()) {
            sDelimiterType = "***";
        }
        File fFile = new File(sourceFile);
        if (!fFile.exists()) {
            throw new FileNotFoundException(fFile.getAbsolutePath() + " cannot be found.");
        }
        if (! fFile.canRead())
        {
            LOGGER.log(Level.SEVERE, "Unable To Read From File {0}", fFile.getName());
            return null;
        }
        FileInputStream fstream = new FileInputStream(fFile);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("utf-8")));
        String sLine;
        Set<SourceAccount> res = new HashSet();
        while ((sLine = br.readLine()) != null) {
            // if a comment line
            if (sLine.startsWith("#")) continue;

            String[] params = sLine.split(sDelimiterType);
            SourceAccount sacc = null;
            if (params.length == 2)
            {
                sacc = new SourceAccount(params[0],Boolean.parseBoolean(params[1]));
            }
            else if (params.length == 1)
            {
                sacc = new SourceAccount(params[0],defaultAccountStatus);
            }
            else {
                LOGGER.severe(String.format("'%s' is a malformed source account file line. Format expected is accName%saccStatus", sLine,sDelimiterType));
            }
            if(sacc!=null) res.add(sacc);


        }
        in.close();
        return res;

    }
}
