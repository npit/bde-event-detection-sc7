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
package gr.demokritos.iit.crawlers.rss.runnable;

public interface DescribableRunnable extends Runnable {

    public String START_SENTINEL = "START_SENTINEL";
    public String END_SENTINEL = "END_SENTINEL";

    /**
     * A Runnable which can describe itself in a way that will be useful for
     * debugging
     *
     * @return a String describing this object in terms of its current state and
     * fields.
     */
    public String description();
}
