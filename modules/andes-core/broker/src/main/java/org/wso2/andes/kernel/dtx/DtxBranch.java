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

package org.wso2.andes.kernel.dtx;

import java.util.HashMap;
import java.util.Map;
import javax.transaction.xa.Xid;

public class DtxBranch {

    private final Xid xid;
    private final DtxRegistry dtxRegistry;
    private Map<Long, State> associatedSessions = new HashMap<>();

    public DtxBranch(Xid xid, DtxRegistry dtxRegistry) {
        this.xid = xid;
        this.dtxRegistry = dtxRegistry;
    }

    public Xid getXid() {
        return xid;
    }

    public boolean associateSession(long sessionID) {
        return associatedSessions.put(sessionID, State.ACTIVE) != null;
    }

    public boolean resumeSession(long sessionID) {
        if(associatedSessions.containsKey(sessionID) && associatedSessions.get(sessionID) == State.SUSPENDED)
        {
            associatedSessions.put(sessionID, State.ACTIVE);
            return true;
        }
        return false;
    }

    public enum State {
        SUSPENDED, ACTIVE
    }
}