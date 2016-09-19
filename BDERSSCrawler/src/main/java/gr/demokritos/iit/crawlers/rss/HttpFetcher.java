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
package gr.demokritos.iit.crawlers.rss;

import gr.demokritos.iit.crawlers.rss.model.UrlMetaData;
import gr.demokritos.iit.crawlers.rss.schedule.DomainExtractor;
import gr.demokritos.iit.crawlers.rss.repository.IRepository;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import gr.demokritos.iit.crawlers.rss.model.Content;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Date;
import org.apache.http.util.EntityUtils;

/**
 * User: ade
 */
public class HttpFetcher implements Fetcher {

    private final HttpClient client;
    private final IRepository repository;
    private final boolean shouldApplyRobotsExclusionRules;
    private static final int NOT_MODIFIED = 304;
    private static final int NOT_FOUND = 404;
    private String previousRobotsTxtUrl;
    private final RobotsTxtParser robotsTxtParser;

    /**
     * Create an HttpFetcher which doesn't obey the robots.txt exclusion rules.
     *
     * @param client
     * @param repository
     */
    public HttpFetcher(HttpClient client, IRepository repository) {
        this.client = client;
        this.repository = repository;
        this.shouldApplyRobotsExclusionRules = false;
        this.robotsTxtParser = new RobotsTxtParser("");
    }

    /**
     * Create an HttpFetcher which may or may not obey the robots.txt exclusion
     * rules.
     *
     * @param client
     * @param repository
     * @param shouldApplyRobotsExclusionRules
     */
    public HttpFetcher(HttpClient client, IRepository repository, boolean shouldApplyRobotsExclusionRules) {
        this.client = client;
        this.repository = repository;
        this.shouldApplyRobotsExclusionRules = shouldApplyRobotsExclusionRules;
        this.robotsTxtParser = new RobotsTxtParser("");
    }

    /**
     * Fetch a URL and return the content or null if the content hasn't changed
     * or is not available.
     *
     * @param url
     * @return a Content which wraps <i>all</i> the content including etag and
     * lastModified headers. The connection is closed before this method
     * returns.
     * @throws IOException
     */
    @Override
    public Content fetchUrl(String url) throws IOException {
        //System.out.println("Running fetchUrl()... ");

        if (url == null) {
            return null;
        }

        if (!url.startsWith("http")) {
            // URLs that don't start with http are considered invalid by this fetcher.
            return null;
        }

        if (!shouldApplyRobotsExclusionRules) {
            return fetchUrlWithoutWorryingAboutRobotsTxt(url);
        }

        DomainExtractor extractor = new DomainExtractor();
        String resourceUrl = extractor.extractResourceUrl(url);
        String robotsTxtUrl = extractor.extractRobotsUrl(url);
        if (!robotsTxtUrl.equals(previousRobotsTxtUrl)) {
            Content robotsTxtContent = fetchUrlWithoutWorryingAboutRobotsTxt(robotsTxtUrl);
            // cache the robotsTxtUrl
            previousRobotsTxtUrl = robotsTxtUrl;
            if (requestdisAllowed(resourceUrl, getRawText(robotsTxtContent))) {
                return null;
            }
        }

        if (!robotsTxtParser.allowed(USER_AGENT, resourceUrl)) {
            return null;
        } else {
            return fetchUrlWithoutWorryingAboutRobotsTxt(url);
        }
    }

    private String getRawText(Content content) {
        if (content == null) {
            return null;
        }
        return content.getRawText();
    }

    protected boolean requestdisAllowed(String resourceUrl, String robotsTxtContents) {
        robotsTxtParser.update(robotsTxtContents);
        return !robotsTxtParser.allowed(USER_AGENT, resourceUrl);
    }

    private boolean urlExists(String url) throws IOException {
        HttpHead httpHead = null;
        HttpResponse response = null;
        try {
            httpHead = new HttpHead(url);
            setHeaders(url, httpHead);
            response = client.execute(httpHead);
            StatusLine statusLine = response.getStatusLine();
            return (NOT_FOUND != statusLine.getStatusCode());
        } finally {
        }
    }

    private Content fetchUrlWithoutWorryingAboutRobotsTxt(String url) throws IOException {
        HttpEntity httpEntity = null;
        InputStreamReader reader = null;
        HttpGet httpGet = null;
        try {

            //We need to do a head request because there are sites out there that will apply tarpitting to GET requests
            if (!urlExists(url)) {
                return null;
            }

            //TODO(ade) Add support for GZIP encoding and decoding
            httpGet = new HttpGet(url);
            setHeaders(url, httpGet);

            //Make the request
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            if (NOT_MODIFIED == statusLine.getStatusCode() || NOT_FOUND == statusLine.getStatusCode()) {
                return null;
            }

            //Copy the content into memory
            httpEntity = response.getEntity();
            reader = getEncodedContentStreamReader(httpEntity);
            StringWriter writer = new StringWriter();
            CharStreams.copy(reader, writer);
            Date crawledDate = new Date();
            return createContent(url, response, writer.toString(), crawledDate);
        } finally {
            if (httpEntity != null) {
                EntityUtils.consumeQuietly(httpEntity);
            }
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
            if (reader != null) {
                // We assume that closing the reader closes the inputstream
                // We know that closing the inputstream releases the HTTP connection
                Closeables.close(reader, false);
            }
        }
    }

    private Content createContent(String url, HttpResponse response, String rawText, Date crawledDate) {
        String etag = null;
        if (response.containsHeader("ETag")) {
            etag = response.getFirstHeader("ETag").getValue();
        }

        String lastModified = null;
        if (response.containsHeader("Last-Modified")) {
            lastModified = response.getFirstHeader("Last-Modified").getValue();
        }
        return new Content(url, rawText, etag, lastModified, crawledDate);
    }

    private void setHeaders(String url, HttpRequestBase httpMethod) {
        UrlMetaData urlMetadata = repository.getFeedMetadata(url);
        if (urlMetadata != null) {
            httpMethod.setHeader("If-None-Match", urlMetadata.getEtag());// Typically this is a hash of the content
            httpMethod.setHeader("If-Modified-Since", urlMetadata.getLastModified());// This is a timestamp
        }
    }

    private InputStreamReader getEncodedContentStreamReader(HttpEntity httpEntity) throws IOException {

        /* For utf-8 bom feeds (greek) */
        InputStream contentStream = new BOMStripperInputStream(httpEntity.getContent());

        Header encodingHeader = httpEntity.getContentEncoding();
        Header contentHeader = httpEntity.getContentType();
        if (encodingHeader != null) {
            String encoding = encodingHeader.getValue();
            return new InputStreamReader(contentStream, encoding);
        } else if (contentHeader != null && contentHeader.getValue() != null) {
            String value = contentHeader.getValue();
            if (value.contains("charset=")) {
                String encoding = value.substring(value.indexOf("charset=") + "charset=".length());
                return new InputStreamReader(contentStream, encoding);
            }
            if (value.contains("CHARSET=")) {
                String encoding = value.substring(value.indexOf("CHARSET=") + "CHARSET=".length());
                return new InputStreamReader(contentStream, encoding);
            }
        }
        return new InputStreamReader(contentStream);
    }
}
