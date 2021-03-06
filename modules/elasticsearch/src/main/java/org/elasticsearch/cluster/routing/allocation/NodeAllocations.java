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

package org.elasticsearch.cluster.routing.allocation;

import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.List;
import java.util.Set;

/**
 * Holds several {@link NodeAllocation}s and combines them into a single allocation decision.
 *
 * @author kimchy (shay.banon)
 */
public class NodeAllocations extends NodeAllocation {

    private final NodeAllocation[] allocations;

    public NodeAllocations(Settings settings) {
        this(settings, ImmutableSet.<NodeAllocation>builder()
                .add(new SameShardNodeAllocation(settings))
                .add(new ReplicaAfterPrimaryActiveNodeAllocation(settings))
                .add(new ThrottlingNodeAllocation(settings))
                .add(new RebalanceOnlyWhenActiveNodeAllocation(settings))
                .build()
        );
    }

    @Inject public NodeAllocations(Settings settings, Set<NodeAllocation> allocations) {
        super(settings);
        this.allocations = allocations.toArray(new NodeAllocation[allocations.size()]);
    }

    @Override public void applyStartedShards(NodeAllocations nodeAllocations, RoutingNodes routingNodes, DiscoveryNodes nodes, List<? extends ShardRouting> startedShards) {
        for (NodeAllocation allocation : allocations) {
            allocation.applyStartedShards(nodeAllocations, routingNodes, nodes, startedShards);
        }
    }

    @Override public void applyFailedShards(NodeAllocations nodeAllocations, RoutingNodes routingNodes, DiscoveryNodes nodes, List<? extends ShardRouting> failedShards) {
        for (NodeAllocation allocation : allocations) {
            allocation.applyFailedShards(nodeAllocations, routingNodes, nodes, failedShards);
        }
    }

    @Override public boolean canRebalance(ShardRouting shardRouting, RoutingNodes routingNodes, DiscoveryNodes nodes) {
        for (NodeAllocation allocation : allocations) {
            if (!allocation.canRebalance(shardRouting, routingNodes, nodes)) {
                return false;
            }
        }
        return true;
    }

    @Override public boolean allocateUnassigned(NodeAllocations nodeAllocations, RoutingNodes routingNodes, DiscoveryNodes nodes) {
        boolean changed = false;
        for (NodeAllocation allocation : allocations) {
            changed |= allocation.allocateUnassigned(nodeAllocations, routingNodes, nodes);
        }
        return changed;
    }

    @Override public Decision canAllocate(ShardRouting shardRouting, RoutingNode node, RoutingNodes routingNodes) {
        Decision ret = Decision.YES;
        for (NodeAllocation allocation : allocations) {
            Decision decision = allocation.canAllocate(shardRouting, node, routingNodes);
            if (decision == Decision.NO) {
                return Decision.NO;
            } else if (decision == Decision.THROTTLE) {
                ret = Decision.THROTTLE;
            }
        }
        return ret;
    }
}
