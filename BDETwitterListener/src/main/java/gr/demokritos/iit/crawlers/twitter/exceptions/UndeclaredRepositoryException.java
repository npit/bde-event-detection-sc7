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
package gr.demokritos.iit.crawlers.twitter.exceptions;

import gr.demokritos.iit.crawlers.twitter.repository.CassandraRepository;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class UndeclaredRepositoryException extends IllegalArgumentException {

    private static String ERROR_MSG = String.format("Classname provided does for Repository implementation "
            + "not match anyone found in package '%s'", CassandraRepository.class.getPackage());

    public UndeclaredRepositoryException() {
        super(ERROR_MSG);
    }

    public UndeclaredRepositoryException(String s) {
        super(s);
    }
}