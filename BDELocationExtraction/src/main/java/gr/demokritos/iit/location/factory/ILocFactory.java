/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.factory;

import gr.demokritos.iit.location.extraction.ILocationExtractor;
import gr.demokritos.iit.location.extraction.provider.ITokenProvider;
import gr.demokritos.iit.location.mapping.IPolygonExtraction;
import gr.demokritos.iit.location.repository.ILocationRepository;
import gr.demokritos.iit.location.sentsplit.ISentenceSplitter;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ILocFactory {

    Logger LOG = Logger.getLogger(ILocFactory.class.getName());

    IPolygonExtraction createDefaultPolygonExtractionClient() throws IllegalArgumentException;

    ILocationRepository createLocationCassandraRepository();

    ILocationExtractor createDefaultLocationExtractor() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    ILocationExtractor createLocationExtractor() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    ITokenProvider createTokenProvider() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    ISentenceSplitter createSentenceSplitter() throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * release underlying DB connection pools
     */
    void releaseResources();

}
