/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.extraction;

import java.util.Set;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocationExtractor {

    /**
     *
     * @param document
     * @return the tokens found in the doc
     */
    Set<String> extractTokens(String document);

    /**
     * extract location entities from text.
     *
     * @param document
     * @return
     */
    Set<String> extractLocation(String document);
}
