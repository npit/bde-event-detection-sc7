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
package gr.demokritos.iit.crawlers.rss.factory.conf;

import gr.demokritos.iit.base.conf.IBaseConf;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public interface IRSSConf extends IBaseConf {

    public int getMaxHttpConnections();

    public int getMaxQueueSize();

    public int getMaxNumberOfCrawlingThreads();

    public String getUrlsFileName();

    public int getDatabaseConnectionPoolSize();

    public long getDelayBetweenCrawls();

    public int getHttpTimeoutInSeconds();

    public boolean runForever();

    boolean getRespectRobots();
    boolean applyHTTPFetchRestrictions();

}
