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

package gr.demokritos.iit.crawler;

import com.google.common.collect.LinkedListMultimap;
import java.util.List;

public class RobotsTxtParser {

    private LinkedListMultimap<String, String> linkedListMultimap;

    public RobotsTxtParser(String contentOfFile) {
        update(contentOfFile);
    }

    public void update(String contentOfFile) {
        this.linkedListMultimap = LinkedListMultimap.create();
        if (contentOfFile == null) {
            return;
        }
        String[] lines = contentOfFile.split("\n");
        List<String> pathsForCurrentUserAgent = null;
        for (String line : lines) {
            line = line.trim();
            // Skip comments that span a line
            if (line.startsWith("#")) {
                continue;
            }

            // Remove comments from the ends of directives
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#"));
            }
            String[] elements = line.split(":");
            if (elements.length < 2) {
                // Line is not a directive, so skip
                continue;
            }

            if (elements[0].trim().equalsIgnoreCase("user-agent")) {
                String userAgent = getDirectiveValue(line);
                userAgent = userAgent.toLowerCase();
                pathsForCurrentUserAgent = linkedListMultimap.get(userAgent);
                continue;
            }

            if (elements[0].trim().equalsIgnoreCase("disallow")) {
                String path = getDirectiveValue(line);
                pathsForCurrentUserAgent.add(path);
            }
        }
    }

    private String getDirectiveValue(String line) {
        String directiveValue = line.substring(line.indexOf(":") + 1);
        return directiveValue.trim();
    }

    public boolean allowed(String userAgent, String requestedPath) {
        // If there are no rules then everything is allowed
        if (linkedListMultimap.isEmpty()) {
            return true;
        }

        // Sites are more likely to ban all robots than just the Sync3 robot
        List<String> paths = linkedListMultimap.get("*");
        for (String path : paths) {
            if (requestedPath.startsWith(path)) {
                //This path was disallowed
                return false;
            }
        }

        int end = userAgent.indexOf("/");
        String shortName = userAgent.substring(0, end);
        shortName = shortName.toLowerCase();
        paths = linkedListMultimap.get(shortName);
        for (String path : paths) {
            if (requestedPath.startsWith(path)) {
                //This path was disallowed
                return false;
            }
        }
        return true;
    }
}
