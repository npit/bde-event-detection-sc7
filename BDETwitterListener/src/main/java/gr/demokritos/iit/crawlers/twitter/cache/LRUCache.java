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
package gr.demokritos.iit.crawlers.twitter.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * a basic caching implementation
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;
    private final int defaultSize;

    private LRUCache(final int size) {
        super(size, 0.75f, true);
        this.defaultSize = size;
    }

    public static <K, V> LRUCache<K, V> build(int size) {
        return new LRUCache<>(size);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > defaultSize;
    }
}
