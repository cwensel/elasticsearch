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

package org.elasticsearch.index.field.data.shorts;

import org.elasticsearch.index.cache.field.data.FieldDataCache;
import org.elasticsearch.index.field.data.FieldData;
import org.elasticsearch.index.field.data.support.NumericFieldDataComparator;

/**
 * @author kimchy (shay.banon)
 */
// LUCENE MONITOR: Monitor against FieldComparator.Short
public class ShortFieldDataComparator extends NumericFieldDataComparator {

    private final short[] values;
    private short bottom;

    public ShortFieldDataComparator(int numHits, String fieldName, FieldDataCache fieldDataCache) {
        super(fieldName, fieldDataCache);
        values = new short[numHits];
    }

    @Override public FieldData.Type fieldDataType() {
        return FieldData.Type.SHORT;
    }

    @Override public int compare(int slot1, int slot2) {
        return values[slot1] - values[slot2];
    }

    @Override public int compareBottom(int doc) {
        return bottom - currentFieldData.shortValue(doc);
    }

    @Override public void copy(int slot, int doc) {
        values[slot] = currentFieldData.shortValue(doc);
    }

    @Override public void setBottom(final int bottom) {
        this.bottom = values[bottom];
    }

    @Override public Comparable value(int slot) {
        return Short.valueOf(values[slot]);
    }
}
