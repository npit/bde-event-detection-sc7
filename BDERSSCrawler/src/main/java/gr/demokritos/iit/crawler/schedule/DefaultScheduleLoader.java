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
package gr.demokritos.iit.crawler.schedule;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import gr.demokritos.iit.crawler.event.EventSink;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class DefaultScheduleLoader implements ScheduleLoader {

    private final File file;
    private final EventSink eventSink;

    public DefaultScheduleLoader(File file, EventSink eventSink) {
        if (!file.exists()) {
            throw new IllegalArgumentException(file.toString() + " does not exist");
        }

        this.file = file;
        this.eventSink = eventSink;
    }

    @Override
    public List<String> load() {
        eventSink.loadingSchedule();
        // load URLs from file
        try {
            List<String> lines = Files.readLines(file, Charsets.UTF_8);
            ListIterator<String> linesIter = lines.listIterator();
            while (linesIter.hasNext()) {
                String curLine = linesIter.next();
                // ignore empty lines
                if (curLine == null || curLine.trim().isEmpty()) {
                    linesIter.remove();
                }
            }
            return lines;
        } catch (IOException e) {
            eventSink.error(e);
            return Collections.EMPTY_LIST;
        }
    }
}
