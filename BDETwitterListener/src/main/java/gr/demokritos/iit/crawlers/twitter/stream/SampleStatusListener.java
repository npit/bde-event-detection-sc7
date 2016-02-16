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
package gr.demokritos.iit.crawlers.twitter.stream;

import static gr.demokritos.iit.crawlers.twitter.factory.TwitterListenerFactory.LOGGER;
import gr.demokritos.iit.crawlers.twitter.repository.IRepository;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class SampleStatusListener extends BaseStreamListener implements StatusListener, IStreamConsumer {

    /**
     * will fetch sample stream in the lang specified. default is 'en'
     */
    protected final String iso_code;

    protected final long engine_id;
    private final AtomicInteger cnt = new AtomicInteger();

    public SampleStatusListener(TwitterStream twitterStream, IRepository repos, String iso_code) {
        super(twitterStream, repos);
        this.iso_code = iso_code;
        this.engine_id = repository.scheduleInitialized(IRepository.CrawlEngine.STREAM);
    }

    public SampleStatusListener(TwitterStream twitterStream, IRepository repos) {
        super(twitterStream, repos);
        this.iso_code = Locale.ENGLISH.getLanguage();
        this.engine_id = repository.scheduleInitialized(IRepository.CrawlEngine.STREAM);
    }

    @Override
    public void getStream() {
        this.twitterStream.addListener(this);
        twitterStream.sample(iso_code);
    }

    @Override
    public void onStatus(Status status) {
        LOGGER.info(String.format("{cnt: %d, user: %s, post_id: %d}: %s", cnt.incrementAndGet(), status.getUser().getScreenName(), status.getId(), status.getText()));
        processStatus(status, engine_id);
    }

    @Override
    public void onStallWarning(StallWarning sw) {
        LOGGER.warning(String.format("received onStallWarning: {code: %s, message: %s, percentage: %d}", sw.getCode(), sw.getMessage(), sw.getPercentFull()));
    }

    @Override
    public void onTrackLimitationNotice(int i) {
        LOGGER.warning(String.format("received OnTrackLimitationNotice: %d", i));
    }

    @Override
    public void onException(Exception excptn) {
        LOGGER.severe(excptn.getMessage());
    }

    @Override
    public void onScrubGeo(long l, long l1) {
    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice sdn) {
    }
}
