/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package org.wso2.andes.server.txn;

import org.apache.commons.lang.NotImplementedException;
import org.wso2.andes.server.message.EnqueableMessage;
import org.wso2.andes.server.queue.BaseQueue;
import org.wso2.andes.server.queue.QueueEntry;

import java.util.Collection;
import java.util.List;

/**
 * Server Transaction type used to handle requests related to distributed transactions.
 */
public class DistributedTransaction implements ServerTransaction {
    @Override
    public long getTransactionStartTime() {
        return 0;
    }

    @Override
    public void addPostTransactionAction(Action postTransactionAction) {
        throw new NotImplementedException();
    }

    @Override
    public void dequeue(BaseQueue queue, EnqueableMessage message, Action postTransactionAction) {
        throw new NotImplementedException();
    }

    @Override
    public void dequeue(Collection<QueueEntry> messages, Action postTransactionAction) {
        throw new NotImplementedException();
    }

    @Override
    public void enqueue(BaseQueue queue, EnqueableMessage message, Action postTransactionAction) {
        throw new NotImplementedException();
    }

    @Override
    public void enqueue(List<? extends BaseQueue> queues, EnqueableMessage message, Action postTransactionAction) {
        throw new NotImplementedException();
    }

    @Override
    public void commit() {
        throw new IllegalStateException("Cannot call tx.commit() on a distributed transaction");
    }

    @Override
    public void rollback() {
        throw new IllegalStateException("Cannot call tx.rollback() on a distributed transaction");
    }
}