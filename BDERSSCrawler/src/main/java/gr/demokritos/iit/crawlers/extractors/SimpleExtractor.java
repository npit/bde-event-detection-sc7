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
package gr.demokritos.iit.crawlers.extractors;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.filters.english.DensityRulesClassifier;
import de.l3s.boilerpipe.filters.heuristics.BlockProximityFusion;
import de.l3s.boilerpipe.filters.heuristics.SimpleBlockFusionProcessor;

public class SimpleExtractor extends ExtractorBase {

    @Override
    public boolean process(TextDocument textDocument) throws BoilerpipeProcessingException {
        SimpleBlockFusionProcessor simpleBlockFusionProcessor = new SimpleBlockFusionProcessor();
        BlockProximityFusion blockProximityFusion = new BlockProximityFusion(1, false);
        DensityRulesClassifier densityRulesClassifier = new DensityRulesClassifier();

        ArticleExtractor articleExtractor = new ArticleExtractor();

        return articleExtractor.process(textDocument) | simpleBlockFusionProcessor.process(textDocument) | blockProximityFusion.process(textDocument)
                | densityRulesClassifier.process(textDocument);
    }
}
