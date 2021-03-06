/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.andes.server.protocol;

import org.wso2.andes.AMQException;
import org.wso2.andes.framing.AMQMethodBody;
import org.wso2.andes.protocol.AMQMethodEvent;

/**
 * AMQNoMethodHandlerException represents the case where no method handler exists to handle an AQMP method.
 *
 * <p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Represents failure to handle an AMQP method.
 * </table>
 *
 * @todo Not an AMQP exception as no status code.
 *
 * @todo Missing method handler. Unlikely to ever happen, and if it does its a coding error. Consider replacing with a
 *       Runtime.
 */
public class AMQNoMethodHandlerException extends AMQException
{
    public AMQNoMethodHandlerException(AMQMethodEvent<AMQMethodBody> evt)
    {
        super("AMQMethodEvent " + evt + " was not processed by any listener on Broker.");
    }
}
