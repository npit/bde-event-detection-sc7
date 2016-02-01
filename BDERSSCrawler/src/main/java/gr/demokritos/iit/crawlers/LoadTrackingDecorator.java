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
package gr.demokritos.iit.crawlers;

import gr.demokritos.iit.crawlers.load.LoadRegistry;
import gr.demokritos.iit.model.Item;

public class LoadTrackingDecorator implements Runnable {

    private final Item item;
    private final LoadRegistry loadRegistry;
    private final Runnable runnable;

    public LoadTrackingDecorator(Item item, LoadRegistry loadRegistry, Runnable runnable) {
        this.item = item;
        this.loadRegistry = loadRegistry;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            loadRegistry.started(item);
            runnable.run();
        } finally {
            loadRegistry.finished(item);
        }
    }
}
