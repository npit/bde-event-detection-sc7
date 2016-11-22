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
package gr.demokritos.iit.crawlers.twitter.structures;

import java.util.Objects;
import net.arnx.jsonic.JSON;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class SourceAccount {

    public static final String SOURCE = "account_name";
    public static final String ACTIVE = "active";

    public int getNumberOfPostsToFetch() {
        return NumberOfPostsToFetch;
    }
    private static Boolean DefaultStatus = true;
    private int NumberOfPostsToFetch;
    private static int DefaultNumberOfPostsToFetch = 100;
    private String account;
    private Boolean active;

    /**
     *
     * @param account the screen_name
     * @param active if true, will be used for monitor
     */
    public SourceAccount(String account, Boolean active) {
        this.account = account;
        this.active = active;
        this.NumberOfPostsToFetch = DefaultNumberOfPostsToFetch;
    }
    public SourceAccount(String account, Boolean active, int NumberOfPostsToFetch ) {
        this.account = account;
        this.active = active;
        this.NumberOfPostsToFetch = NumberOfPostsToFetch;
    }
    public SourceAccount(String account) {
        this.account = account;
        this.active = DefaultStatus;
        this.NumberOfPostsToFetch = DefaultNumberOfPostsToFetch;
    }
    /**
     *
     * @return the account_name (screen_name)
     */
    public String getAccount() {
        return account;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "SourceAccount{" + "account=" + account + ", active=" + active + ", numposts=" + NumberOfPostsToFetch + "}";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.account);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SourceAccount other = (SourceAccount) obj;
        if (!Objects.equals(this.account, other.account)) {
            return false;
        }
        return true;
    }

    public String toJSON() {
        return JSON.encode(this, false);
    }
}
