/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facets.geodistance;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.search.facets.Facet;

import java.util.List;

/**
 * @author kimchy (shay.banon)
 */
public interface GeoDistanceFacet extends Facet, Iterable<GeoDistanceFacet.Entry> {

    String fieldName();

    String getFieldName();

    String valueFieldName();

    String getValueFieldName();

    DistanceUnit unit();

    DistanceUnit getUnit();

    /**
     * An ordered list of geo distance facet entries.
     */
    List<Entry> entries();

    /**
     * An ordered list of geo distance facet entries.
     */
    List<Entry> getEntries();

    public class Entry {

        double from = Double.NEGATIVE_INFINITY;

        double to = Double.POSITIVE_INFINITY;

        long count;

        double total;

        Entry() {
        }

        public Entry(double from, double to, long count, double total) {
            this.from = from;
            this.to = to;
            this.count = count;
            this.total = total;
        }

        public double from() {
            return this.from;
        }

        public double getFrom() {
            return from();
        }

        public double to() {
            return this.to;
        }

        public double getTo() {
            return to();
        }

        public long count() {
            return this.count;
        }

        public long getCount() {
            return count();
        }

        public double total() {
            return this.total;
        }

        public double getTotal() {
            return total();
        }

        /**
         * The mean of this facet interval.
         */
        public double mean() {
            return total / count;
        }

        /**
         * The mean of this facet interval.
         */
        public double getMean() {
            return mean();
        }
    }
}
