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
package gr.demokritos.iit.crawlers.twitter.url;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.logging.Level;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
/**
 * Expand short urls. Works with all the major url shorteners (t.co, bit.ly,
 * fb.me, is.gd, goo.gl, etc) // from
 * http://www.baeldung.com/unshorten-url-httpclient
 *
 * @author Baeldung, George K.
 * 
*/
public class DefaultURLUnshortener extends AbstractURLUnshortener implements IURLUnshortener {

    public DefaultURLUnshortener() {
        super();
    }

    public DefaultURLUnshortener(int connectTimeout, int readTimeout, int cacheSize) {
        super(connectTimeout, readTimeout, cacheSize);
    }

    /**
     *
     * @param url the short URL
     * @return the actual long URL
     */
    protected URLData expandSingleLevel(String url) {
        // if in cache, return
        URLData exi = cache.get(url);
        if (exi != null) {
            return exi;
        }
        // init http entities
        HttpGet request = null;
        HttpEntity httpEntity = null;
        InputStream entityContentStream = null;
        int statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        try {
            request = new HttpGet(url);
            // get the response
            HttpResponse httpResponse = client.execute(request);
            // get entity
            httpEntity = httpResponse.getEntity();
            // get status code
            statusCode = httpResponse.getStatusLine().getStatusCode();
            // if not a redirection, this is it
            if (!isRedirect(statusCode)) {
                return new URLData(statusCode, url);
            }
            // get content
            entityContentStream = httpEntity.getContent();
            // obtain headers
            Header[] headers = httpResponse.getHeaders(HttpHeaders.LOCATION);
            Preconditions.checkState(headers.length == 1);
            // get redirected URL
            String newUrl = headers[0].getValue();
            // update cache
            URLData urld = new URLData(statusCode, newUrl);
            cache.put(url, urld);
            // return 
            return urld;
        } catch (IllegalArgumentException | IllegalStateException | IOException uriEx) {
            LOGGER.log(Level.WARNING, String.format("%s: %s", uriEx.getMessage(), url), uriEx);
            return new URLData(statusCode, url);
        } finally {
            if (entityContentStream != null) {
                try {
                    entityContentStream.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            if (httpEntity != null) {
                try {
                    EntityUtils.consume(httpEntity);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    /**
     * Will invoke {@link #expandSingleLevel(java.lang.String)} as many times
     * needed to reach to the final URL
     *
     * @param urlArg the shortened URL
     * @return the full URL
     */
    @Override
    public String expand(String urlArg) {
        String originalUrl = urlArg;
        // get new URL
        URLData urld = expandSingleLevel(originalUrl);
        String newUrl = urld.getUrl();
        List<String> alreadyVisited = Lists.newArrayList(originalUrl, newUrl);
        // if new URL equals the input URL, this is it
        while (!originalUrl.equals(newUrl)) {
            originalUrl = newUrl;
            URLData tmp = expandSingleLevel(originalUrl);
            newUrl = tmp.getUrl();
            if (isRedirect(tmp.getHttpCode()) && alreadyVisited.contains(newUrl)) {
                LOGGER.info("likely a redirect URL, aborting");
                break;
            }
            alreadyVisited.add(newUrl);
        }
        return newUrl;
    }
}
