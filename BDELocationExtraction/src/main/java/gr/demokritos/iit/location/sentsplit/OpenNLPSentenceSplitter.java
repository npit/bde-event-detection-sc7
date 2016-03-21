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
package gr.demokritos.iit.location.sentsplit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * Handles sentence splitting Uses openNLP lib
 *
 * @author George K. <gkiom@scify.org>`
 */
public class OpenNLPSentenceSplitter implements ISentenceSplitter {

    private String sModelFilePath = null;

    /**
     * Sentence splitter model
     */
    private SentenceModel smSplitter = null;
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private final SentenceDetectorME sentenceDetector;

    /**
     *
     * @param sModelPath The full path where the model file is located
     */
    public OpenNLPSentenceSplitter(String sModelPath) {
        this.sModelFilePath = sModelPath;
        initSplitter();
        this.sentenceDetector = new SentenceDetectorME(smSplitter);
    }

    @Override
    public String[] splitToSentences(String sDocument) {
        if (smSplitter == null) {
            throw new IllegalArgumentException("no model found");
        }
        String[] saSentences;
        saSentences = sentenceDetector.sentDetect(sDocument.trim());
        return saSentences;
    }

    /**
     * Initializes the sentence splitter model for a specific language.
     */
    private void initSplitter() {
        // Check whether splitter model already exists
        SentenceModel model = null;
        File fTmp;
        if (sModelFilePath == null) {
            throw new IllegalArgumentException("no model found");
        } else {
            fTmp = new File(sModelFilePath);
        }
        // If file exists
        if (fTmp.exists()) {
            // Try to load it
            InputStream modelIn = null;
            try {
                modelIn = new FileInputStream(fTmp);
                model = new SentenceModel(modelIn);
                this.smSplitter = model;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not load sentence splitter model {0}: {1}", new Object[]{fTmp.getPath(), e});
            } finally {
                // Finalize model file access, if possible
                if (modelIn != null) {
                    try {
                        modelIn.close();
                    } catch (IOException e) {
                        LOGGER.warning(e.getMessage());
                    }
                }
            }
        } else {
            throw new IllegalArgumentException(String.format("'%s' not found", sModelFilePath));
        }
    }
}
