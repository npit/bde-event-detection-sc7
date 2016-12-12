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
package gr.demokritos.iit.clustering.util;

import com.datastax.spark.connector.japi.CassandraRow;
import org.apache.spark.api.java.function.Function;
import scala.Tuple4;
import scala.Tuple5;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class CassandraArticleRowToTuple4RDD implements Function<CassandraRow, Tuple4<String, String, String, Long>> {

    private final String string_row_1_field;
    private final String string_row_2_field;
    private final String string_row_3_field;
    private final String long_row_4_field;

    public CassandraArticleRowToTuple4RDD(String string_row_1_field, String string_row_2_field,
                                          String string_row_3_field, String long_row_4_field) {
        this.string_row_1_field = string_row_1_field;
        this.string_row_2_field = string_row_2_field;
        this.string_row_3_field = string_row_3_field;
        this.long_row_4_field = long_row_4_field;
    }

    @Override
    public Tuple4<String, String, String, Long> call(CassandraRow arg0) throws Exception {
        return new Tuple4(
                arg0.getString(string_row_1_field),
                arg0.getString(string_row_2_field),
                arg0.getString(string_row_3_field),
                arg0.getLong(long_row_4_field));
    }
}
