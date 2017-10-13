/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.andes.configuration.models;

import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;

/**
 * Root configuration model of the Broker.
 * This class provide access to all the configuration object representations of the broker.
 */
@Configuration(namespace = "wso2.carbon.broker",
               description = "Broker configuration parameters")
public class BrokerConfiguration {

    @Element(description = "Coordination related configuration.")
    private CoordinationConfig coordination = new CoordinationConfig();

    @Element(description = "Transport related configuration")
    private TransportsConfig transports = new TransportsConfig();

    @Element(description = "Depending on the database type selected in deployment.yaml, you must enable the\n"
            + "relevant Data access classes here. Currently WSO2 MB Supports RDBMS(any RDBMS store).")
    private PersistenceConfig persistance = new PersistenceConfig();

    @Element(description = "Publisher transaction related configurations.")
    private TransactionConfig transaction = new TransactionConfig();

    @Element(description = "This section allows you to tweak memory and processor allocations used by WSO2 MB.\n"
            + "Broken down by critical processes so you have a clear view of which parameters to change in\n"
            + "different scenarios.")
    private PerformanceTuningConfig performanceTuning = new PerformanceTuningConfig();

    public CoordinationConfig getCoordination() {
        return coordination;
    }

    public TransportsConfig getTransports() {
        return transports;
    }

    public PersistenceConfig getPersistance() {
        return persistance;
    }

    public TransactionConfig getTransaction() {
        return transaction;
    }

    public PerformanceTuningConfig getPerformanceTuning() {
        return performanceTuning;
    }
}
