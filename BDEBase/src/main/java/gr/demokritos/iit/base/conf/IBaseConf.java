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
package gr.demokritos.iit.base.conf;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IBaseConf {

    String FILE_SEPARATOR = System.getProperty("file.separator");
    String UTF8 = "UTF-8";

    int getCacheSize();

    String getCassandraClusterName();

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    String[] getCassandraHosts();

    /**
     *
     * @return the keyspace to use (Cassandra backend only)
     */
    String getCassandraKeyspace();

    /**
     *
     * @return the cassandra listen port
     */
    int getCassandraPort();

    /**
     * used by the {@link HttpGet} implementation for shutting down inactive
     * hosts
     *
     * @return
     */
    int getConnectionTimeOut();

    int getSocketTimeout();

    String getDatabaseHost();

    /**
     * MySQL backend
     *
     * @return the database name that we write into
     */
    String getDatabaseName();

    /**
     * MySQL backend
     *
     * @return
     */
    String getDatabasePassword();

    /**
     * MySQL backend
     *
     * @return
     */
    String getDatabaseUserName();

    /**
     * the full classname for the repository implementation to utilize
     *
     * @return
     */
    String getRepositoryImpl();

    int getDataSourceMinPoolSize();

    int getDataSourceAcquireIncrement();

    int getDataSourceMaxPoolSize();

    int getDataSourceMaxStatements();
    
    
}
