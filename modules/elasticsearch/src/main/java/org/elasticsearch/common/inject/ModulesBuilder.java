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

package org.elasticsearch.common.inject;

import org.elasticsearch.common.collect.Lists;

import java.util.List;

/**
 * @author kimchy (shay.banon)
 */
public class ModulesBuilder {

    private final List<Module> modules = Lists.newArrayList();

    public ModulesBuilder add(Module... modules) {
        for (Module module : modules) {
            add(module);
        }
        return this;
    }

    public ModulesBuilder add(Module module) {
        modules.add(module);
        if (module instanceof SpawnModules) {
            Iterable<? extends Module> spawned = ((SpawnModules) module).spawnModules();
            for (Module spawn : spawned) {
                add(spawn);
            }
        }
        return this;
    }

    public Injector createInjector() {
        Modules.processModules(modules);
        return Guice.createInjector(modules);
    }

    public Injector createChildInjector(Injector injector) {
        Modules.processModules(modules);
        return injector.createChildInjector(modules);
    }
}
