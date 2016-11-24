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

    int getMaxHttpConnections();

    int getMaxQueueSize();

    int getMaxNumberOfCrawlingThreads();

    String getUrlsFileName();

    int getDatabaseConnectionPoolSize();

    long getDelayBetweenCrawls();

    int getHttpTimeoutInSeconds();

    boolean runForever();

    boolean getRespectRobots();
    boolean applyHTTPFetchRestrictions();

    enum OperationMode {

        CRAWL("crawl"), FETCH("fetch");
        private String type;

        private OperationMode(String type) {
            this.type = type;
        }
        public static boolean supports(String candidate)
        {
            for(OperationMode elem : OperationMode.values())
            {
                if(elem.toString().equals(candidate)) return true;
//                System.out.println("["+elem.name() + "] =/= [" + candidate+"]");
            }
            return false;
        }
        @Override
        public String toString() {
            return type;
        }
    }
    OperationMode getOperationMode();

}
