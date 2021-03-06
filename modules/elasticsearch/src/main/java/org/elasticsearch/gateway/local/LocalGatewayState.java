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

package org.elasticsearch.gateway.local;

import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.builder.XContentBuilder;
import org.elasticsearch.index.shard.ShardId;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * @author kimchy (shay.banon)
 */
public class LocalGatewayState {

    public static class StartedShard {
        private final long version;
        private final ShardId shardId;

        public StartedShard(long version, ShardId shardId) {
            this.version = version;
            this.shardId = shardId;
        }

        public long version() {
            return version;
        }

        public ShardId shardId() {
            return shardId;
        }
    }

    private final long version;

    private final MetaData metaData;

    private final ImmutableMap<ShardId, Long> shards;

    public LocalGatewayState(long version, MetaData metaData, Map<ShardId, Long> shards) {
        this.version = version;
        this.metaData = metaData;
        this.shards = ImmutableMap.copyOf(shards);
    }

    public long version() {
        return version;
    }

    public MetaData metaData() {
        return metaData;
    }

    public ImmutableMap<ShardId, Long> shards() {
        return this.shards;
    }

    public Long startedShardVersion(ShardId shardId) {
        return shards.get(shardId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long version;

        private MetaData metaData;

        private Map<ShardId, Long> shards = Maps.newHashMap();

        public Builder state(LocalGatewayState state) {
            this.version = state.version();
            this.metaData = state.metaData();
            this.shards.putAll(state.shards);
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder metaData(MetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        public Builder remove(ShardId shardId) {
            this.shards.remove(shardId);
            return this;
        }

        public Builder put(ShardId shardId, long version) {
            this.shards.put(shardId, version);
            return this;
        }

        public LocalGatewayState build() {
            return new LocalGatewayState(version, metaData, shards);
        }

        public static void toXContent(LocalGatewayState state, XContentBuilder builder, ToXContent.Params params) throws IOException {
            builder.startObject("state");

            builder.field("version", state.version());
            MetaData.Builder.toXContent(state.metaData(), builder, params);

            builder.startArray("shards");
            for (Map.Entry<ShardId, Long> entry : state.shards.entrySet()) {
                builder.startObject();
                builder.field("index", entry.getKey().index().name());
                builder.field("id", entry.getKey().id());
                builder.field("version", entry.getValue());
                builder.endObject();
            }
            builder.endArray();

            builder.endObject();
        }

        public static LocalGatewayState fromXContent(XContentParser parser, @Nullable Settings globalSettings) throws IOException {
            Builder builder = new Builder();

            String currentFieldName = null;
            XContentParser.Token token = parser.nextToken();
            if (token == null) {
                // no data...
                return builder.build();
            }
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token == XContentParser.Token.START_OBJECT) {
                    if ("meta-data".equals(currentFieldName)) {
                        builder.metaData = MetaData.Builder.fromXContent(parser, globalSettings);
                    }
                } else if (token == XContentParser.Token.START_ARRAY) {
                    if ("shards".equals(currentFieldName)) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            if (token == XContentParser.Token.START_OBJECT) {
                                String shardIndex = null;
                                int shardId = -1;
                                long version = -1;
                                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                    if (token == XContentParser.Token.FIELD_NAME) {
                                        currentFieldName = parser.currentName();
                                    } else if (token.isValue()) {
                                        if ("index".equals(currentFieldName)) {
                                            shardIndex = parser.text();
                                        } else if ("id".equals(currentFieldName)) {
                                            shardId = parser.intValue();
                                        } else if ("version".equals(currentFieldName)) {
                                            version = parser.longValue();
                                        }
                                    }
                                }
                                builder.shards.put(new ShardId(shardIndex, shardId), version);
                            }
                        }
                    }
                } else if (token.isValue()) {
                    if ("version".equals(currentFieldName)) {
                        builder.version = parser.longValue();
                    }
                }
            }

            return builder.build();
        }

        public static LocalGatewayState readFrom(StreamInput in, @Nullable Settings globalSettings) throws IOException {
            LocalGatewayState.Builder builder = new Builder();
            builder.version = in.readLong();
            builder.metaData = MetaData.Builder.readFrom(in, globalSettings);
            int size = in.readVInt();
            for (int i = 0; i < size; i++) {
                builder.shards.put(ShardId.readShardId(in), in.readLong());
            }
            return builder.build();
        }

        public static void writeTo(LocalGatewayState state, StreamOutput out) throws IOException {
            out.writeLong(state.version());
            MetaData.Builder.writeTo(state.metaData(), out);

            out.writeVInt(state.shards.size());
            for (Map.Entry<ShardId, Long> entry : state.shards.entrySet()) {
                entry.getKey().writeTo(out);
                out.writeLong(entry.getValue());
            }
        }
    }
}
