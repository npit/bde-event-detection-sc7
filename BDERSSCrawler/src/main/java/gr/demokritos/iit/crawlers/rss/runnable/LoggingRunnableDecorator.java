
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

import gr.demokritos.iit.crawlers.rss.event.EventSink;

public class LoggingRunnableDecorator implements Runnable {

    private final DescribableRunnable runnable;
    private final EventSink eventSink;

    public LoggingRunnableDecorator(DescribableRunnable runnable, EventSink eventSink) {
        this.runnable = runnable;
        this.eventSink = eventSink;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Exception e) {
            String message = "Runnable " + runnable.description() + " just failed";
            eventSink.error(message, e);
        }
    }
}
