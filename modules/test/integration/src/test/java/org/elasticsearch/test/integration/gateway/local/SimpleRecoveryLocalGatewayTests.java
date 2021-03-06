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

package org.elasticsearch.test.integration.gateway.local;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.gateway.Gateway;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.elasticsearch.client.Requests.*;
import static org.elasticsearch.common.settings.ImmutableSettings.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.elasticsearch.index.query.xcontent.QueryBuilders.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author kimchy (shay.banon)
 */
public class SimpleRecoveryLocalGatewayTests extends AbstractNodesTests {

    @AfterMethod public void cleanAndCloseNodes() throws Exception {
        for (int i = 0; i < 10; i++) {
            if (node("node" + i) != null) {
                node("node" + i).stop();
                // since we store (by default) the index snapshot under the gateway, resetting it will reset the index data as well
                ((InternalNode) node("node" + i)).injector().getInstance(Gateway.class).reset();
            }
        }
        closeAllNodes();
    }

    @Test public void testSingleNode() throws Exception {
        buildNode("node1", settingsBuilder().put("gateway.type", "local").build());
        cleanAndCloseNodes();

        Node node1 = startNode("node1", settingsBuilder().put("gateway.type", "local").put("index.number_of_shards", 1).build());
        node1.client().prepareIndex("test", "type1", "1").setSource(jsonBuilder().startObject().field("field", "value1").endObject()).execute().actionGet();
        node1.client().admin().indices().prepareFlush().execute().actionGet();
        node1.client().prepareIndex("test", "type1", "2").setSource(jsonBuilder().startObject().field("field", "value2").endObject()).execute().actionGet();
        node1.client().admin().indices().prepareRefresh().execute().actionGet();

        assertThat(node1.client().prepareCount().setQuery(matchAllQuery()).execute().actionGet().count(), equalTo(2l));

        closeNode("node1");
        node1 = startNode("node1", settingsBuilder().put("gateway.type", "local").build());

        logger.info("Running Cluster Health (wait for the shards to startup)");
        ClusterHealthResponse clusterHealth = client("node1").admin().cluster().health(clusterHealthRequest().waitForYellowStatus().waitForActiveShards(1)).actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.status());
        assertThat(clusterHealth.timedOut(), equalTo(false));
        assertThat(clusterHealth.status(), equalTo(ClusterHealthStatus.YELLOW));

        assertThat(node1.client().prepareCount().setQuery(matchAllQuery()).execute().actionGet().count(), equalTo(2l));
    }

    @Test public void testTwoNodeFirstNodeCleared() throws Exception {
        // clean two nodes
        buildNode("node1", settingsBuilder().put("gateway.type", "local").build());
        buildNode("node2", settingsBuilder().put("gateway.type", "local").build());
        cleanAndCloseNodes();

        Node node1 = startNode("node1", settingsBuilder().put("gateway.type", "local").put("index.number_of_shards", 1).build());
        Node node2 = startNode("node2", settingsBuilder().put("gateway.type", "local").put("index.number_of_shards", 1).build());

        node1.client().prepareIndex("test", "type1", "1").setSource(jsonBuilder().startObject().field("field", "value1").endObject()).execute().actionGet();
        node1.client().admin().indices().prepareFlush().execute().actionGet();
        node1.client().prepareIndex("test", "type1", "2").setSource(jsonBuilder().startObject().field("field", "value2").endObject()).execute().actionGet();
        node1.client().admin().indices().prepareRefresh().execute().actionGet();

        assertThat(node1.client().prepareCount().setQuery(matchAllQuery()).execute().actionGet().count(), equalTo(2l));

        logger.info("--> closing nodes");
        closeNode("node1");
        closeNode("node2");

        logger.info("--> cleaning node1 gateway");
        buildNode("node1", settingsBuilder().put("gateway.type", "local").build());
        cleanAndCloseNodes();

        node1 = startNode("node1", settingsBuilder().put("gateway.type", "local").put("gateway.recover_after_nodes", 2).build());
        node2 = startNode("node2", settingsBuilder().put("gateway.type", "local").put("gateway.recover_after_nodes", 2).build());

        logger.info("Running Cluster Health (wait for the shards to startup)");
        ClusterHealthResponse clusterHealth = client("node1").admin().cluster().health(clusterHealthRequest().waitForGreenStatus().waitForActiveShards(2)).actionGet();
        logger.info("Done Cluster Health, status " + clusterHealth.status());
        assertThat(clusterHealth.timedOut(), equalTo(false));
        assertThat(clusterHealth.status(), equalTo(ClusterHealthStatus.GREEN));

        assertThat(node1.client().prepareCount().setQuery(matchAllQuery()).execute().actionGet().count(), equalTo(2l));
    }
}
