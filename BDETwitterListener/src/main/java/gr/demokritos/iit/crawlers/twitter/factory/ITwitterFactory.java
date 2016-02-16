/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.crawlers.twitter.factory;

import gr.demokritos.iit.base.exceptions.UndeclaredRepositoryException;
import gr.demokritos.iit.crawlers.twitter.impl.ITwitterRestConsumer;
import gr.demokritos.iit.crawlers.twitter.policy.ICrawlPolicy;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import gr.demokritos.iit.crawlers.twitter.stream.IStreamConsumer;
import gr.demokritos.iit.crawlers.twitter.url.IURLUnshortener;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface ITwitterFactory {
    Logger LOGGER = Logger.getLogger(TwitterListenerFactory.class.getName());

    /**
     * used for search instantiation
     *
     * @return an instance of {@link BaseTwitterRestConsumer}
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws PropertyVetoException
     */
    ITwitterRestConsumer getBaseTwitterListener() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, PropertyVetoException;

    ICrawlPolicy getCrawlPolicy(IRepository repository) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException;

    /**
     * get an implementation class of {@link IURLUnshortener}. The URL expander
     * is utilized when we want to expand URLs from links of tweets
     *
     * @return
     */
    IURLUnshortener getDefaultURLUnshortener();

    /**
     * get an implementation class of {@link IRepository}. Supported
     * repositories are Cassandra, MySQL.
     *
     * @return
     * @throws PropertyVetoException
     * @throws UndeclaredRepositoryException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    IRepository getRepository() throws PropertyVetoException, UndeclaredRepositoryException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * get an implementation class of {@link IStreamConsumer}. Currently, the
     * only meaningful implementation is {@link UserStatusListener}
     *
     * @return
     * @throws PropertyVetoException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws UndeclaredRepositoryException
     * @throws InvocationTargetException
     */
    IStreamConsumer getStreamImpl() throws PropertyVetoException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, UndeclaredRepositoryException, InvocationTargetException;

    /**
     * switch only repository, others use default
     *
     * @param repository
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    ITwitterRestConsumer getTwitterListener(IRepository repository) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * load twitter listener (the implementation provided at
     * 'twitter.properties'), loads all declared implementations of:
     * {@link IRepository}, {@link ICrawlPolicy} along with the {@link TConfig}
     *
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws PropertyVetoException
     */
    ITwitterRestConsumer getTwitterListener() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, PropertyVetoException;

    /**
     * used for search instantiation
     *
     * @return an instance of {@link BaseTwitterRestConsumer}
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    IURLUnshortener getURLUnshortener() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * release underlying DB connection pools
     */
    void releaseResources();
    
}
