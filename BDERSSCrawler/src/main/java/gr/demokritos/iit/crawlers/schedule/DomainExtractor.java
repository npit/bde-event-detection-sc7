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
package gr.demokritos.iit.crawlers.schedule;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * User: ade
 */
public class DomainExtractor {

    public String extractDomain(String url) {
        String host = convertToHost(url);

        //We need to convert a host like matt.wordpress.com into wordpress.com
        String[] tokens = host.split("\\.");
        return tokens[tokens.length - 2] + "." + tokens[tokens.length - 1];
    }

    private String convertToHost(String sURL) {
        try {
            // http://stackoverflow.com/questions/749709/how-to-deal-with-the-urisyntaxexception
            URL uURL = new URL(sURL);
            String nullFragment = null;
            URI uri = new URI(uURL.getProtocol(), uURL.getHost(), uURL.getPath(), uURL.getQuery(), nullFragment);
            return uri.getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Given a URL like http://example.com/somefolder/a/b/c returns
     * http://example.com/robots.txt
     *
     * @param feedUrl
     * @return
     */
    public String extractRobotsUrl(String feedUrl) {
        String host = convertToHost(feedUrl);
        return "http://" + host + "/robots.txt";
    }

    /**
     * Given a URL like http://example.com/somefolder/a/b/c returns
     * /somefolder/a/b/c
     *
     * @param url
     * @return
     */
    public String extractResourceUrl(String url) {
        String host = convertToHost(url);
        int startLocation = url.indexOf(host);
        String resourceUrl = url.substring(startLocation + host.length());

        if (resourceUrl.isEmpty()) {
            return "/";
        } else {
            return resourceUrl;
        }
    }
}
