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
package org.wso2.andes.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.amqp.AMQPUtils;
import org.wso2.andes.kernel.*;
import org.wso2.andes.kernel.distruptor.inbound.PubAckHandler;
import org.wso2.andes.server.ClusterResourceHolder;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class mainly focuses on negotiating the connections and exchanging data with the message store
 * The class will interface with the Andes kernel and will ensure that the information that's received from the bridge
 * is conforming to the data structure expected by the kernel, The basic operations done through this class will be
 * converting between the meta data and message content, indicate subscriptions and disconnections
 */
public class DistributedStoreConnector implements MQTTConnector {

    private static Log log = LogFactory.getLog(DistributedStoreConnector.class);
    private static final String MQTT_TOPIC_DESTINATION = "destination";
    private static final String MQTT_QUEUE_IDENTIFIER = "targetQueue";
    private AndesChannel andesChannel;

    /**
     * Will maintain the relation between the publisher client identifiers vs the id generated cluster wide
     * Key of the map would be the mqtt specific client id and the value would be the cluster uuid
     */
    private Map<String, UUID> publisherTopicCorrelate = new HashMap<String, UUID>();

    public DistributedStoreConnector() {
        andesChannel = Andes.getInstance().createChannel(new FlowControlListener() {
            @Override
            public void block() {
                // TODO: Need to implement
            }

            @Override
            public void unblock() {
                // TODO: Need to implement
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void messageAck(long messageID, String topicName, String storageName, UUID subChannelID)
            throws AndesException {
        AndesAckData andesAckData = new AndesAckData(subChannelID, messageID,
                topicName, storageName, true);
        Andes.getInstance().ackReceived(andesAckData);
    }

    /**
     * {@inheritDoc}
     */
    public void addMessage(ByteBuffer message, String topic, int qosLevel,
                           int mqttLocalMessageID, boolean retain,
                           String publisherID, PubAckHandler pubAckHandler) throws MQTTException {
        if (message.hasArray()) {

            UUID publisherClusterID = publisherTopicCorrelate.get(publisherID);
            if (null == publisherClusterID) {
                //We need to generate a uuid
                publisherClusterID = UUID.randomUUID();
                publisherTopicCorrelate.put(publisherID, publisherClusterID);
            }

            //Will get the bytes of the message
            byte[] messageData = message.array();
            long messageID = 0; // unique message Id will be generated By Andes.
            //Will start converting the message body
            AndesMessagePart messagePart = MQTTUtils.convertToAndesMessage(messageData, messageID);
            //Will Create the Andes Header
            AndesMessageMetadata messageHeader = MQTTUtils.convertToAndesHeader(messageID, topic, qosLevel,
                    messageData.length, retain, publisherClusterID);

            // Add properties to be used for publisher acks
            messageHeader.addProperty(MQTTUtils.CLIENT_ID, publisherID);
            messageHeader.addProperty(MQTTUtils.MESSAGE_ID, mqttLocalMessageID);
            messageHeader.addProperty(MQTTUtils.QOSLEVEL, qosLevel);

            // Publish to Andes core
            AndesMessage andesMessage = new AndesMessage(messageHeader);
            andesMessage.addMessagePart(messagePart);
            // TODO: Need to handle Flow control in MQTT properly
            Andes.getInstance().messageReceived(andesMessage, andesChannel, pubAckHandler);
            if(log.isDebugEnabled()) {
                log.debug(" Message added with message id " + mqttLocalMessageID);
            }

        } else {
            throw new MQTTException("Message content is not backed by an array, or the array is read-only .");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addSubscriber(MQTTopicManager channel, String topic, String clientID, String mqttClientID,
                              boolean isCleanSesion, int qos, UUID subscriptionChannelID) throws MQTTException {

        String queue_identifier = topic + mqttClientID;
        if (isCleanSesion) {
            MQTTLocalSubscription mqttTopicSubscriber = createSubscription(channel, topic, clientID, mqttClientID,
                    true, qos, subscriptionChannelID, topic, true, true);
            //Should indicate the record in the cluster
            try {
                Andes.getInstance().openLocalSubscription(mqttTopicSubscriber);
                //First will register the subscription as a queue
                if (log.isDebugEnabled()) {
                    log.debug("Subscription registered to the " + topic + " with channel id " + clientID);
                }
            } catch (SubscriptionAlreadyExistsException e) {
                final String message = "Error occurred while creating the topic subscription in the kernel";
                log.error(message, e);
                throw new MQTTException(message, e);
            } catch (AndesException e) {
                log.error("Error occurred while opening subscription ", e);
            }
        } else {
            //This will be similar to a durable subscription of AMQP
            MQTTLocalSubscription mqttTopicSubscriber = createSubscription(channel, topic, clientID, mqttClientID,
                    true, qos, subscriptionChannelID, queue_identifier, true, true);
            MQTTLocalSubscription mqttQueueSubscriber = createSubscription(channel, queue_identifier, clientID,
                    mqttClientID, false, qos, subscriptionChannelID, queue_identifier, false, true);

            //Should indicate the record in the cluster
            try {
                //Will record the subscription as a topic
                Andes.getInstance().openLocalSubscription(mqttTopicSubscriber);
                //Will record the subscription as a queue
                Andes.getInstance().openLocalSubscription(mqttQueueSubscriber);
                //First will register the subscription as a queue
                if (log.isDebugEnabled()) {
                    log.debug("Subscription registered to the " + topic + " with channel id " + clientID);
                }
            } catch (SubscriptionAlreadyExistsException e) {
                final String message = "Error occurred while creating the topic subscription in the kernel";
                log.error(message, e);
                throw new MQTTException(message, e);
            } catch (AndesException e) {
                log.error("Error occurred while opening subscription", e);
            }

        }

        //Finally will notify on the client connection
        Andes.getInstance().clientConnectionCreated(subscriptionChannelID);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSubscriber(MQTTopicManager channel, String subscribedTopic, String subscriptionChannelID,
                                 UUID subscriberChannel, boolean isCleanSession, String mqttClientID)
            throws MQTTException {
        try {

            String queue_identifier = subscribedTopic + mqttClientID;
            if (isCleanSession) {
                //Here we hard code the QoS level since for subscription removal that doesn't matter
                MQTTLocalSubscription mqttTopicSubscriber = createSubscription(channel, subscribedTopic,
                        subscriptionChannelID, subscriptionChannelID,
                        true, 0, subscriberChannel, subscribedTopic, true, false);
                Andes.getInstance().closeLocalSubscription(mqttTopicSubscriber);
            } else {
                //This will be similar to a durable subscription of AMQP
                MQTTLocalSubscription mqttTopicSubscriber = createSubscription(channel, subscribedTopic,
                        subscriptionChannelID, subscriptionChannelID,
                        false, 0, subscriberChannel, queue_identifier, true, false);
                MQTTLocalSubscription mqttQueueSubscriber = createSubscription(channel, queue_identifier,
                        subscriptionChannelID, subscriptionChannelID, false, 0, subscriberChannel, queue_identifier,
                        false, false);
                Andes.getInstance().closeLocalSubscription(mqttTopicSubscriber);
                Andes.getInstance().closeLocalSubscription(mqttQueueSubscriber);
            }
            //Will indicate the closure of the subscription connection
            Andes.getInstance().clientConnectionClosed(subscriberChannel);
            if (log.isDebugEnabled()) {
                log.debug("Disconnected subscriber from topic " + subscribedTopic);
            }

        } catch (AndesException e) {
            final String message = "Error occured while removing the subscriber ";
            log.error(message, e);
            throw new MQTTException(message, e);
        }
    }

    /**
     * @{inheritDoc}
     */
    public UUID removePublisher(String mqttclientChannelId) {
        return publisherTopicCorrelate.remove(mqttclientChannelId);
    }
    /**
     * Will create subscriptions out of the provided list of information, this will be used when creating durable,
     * non durable subscriptions. As well as creating the subscription object for removal
     *
     * @param channel               the chanel the data communication should be done at
     * @param topic                 the name of the destination
     * @param clientID              the identifier which is unique across the cluster
     * @param mqttClientID          the id of the client which is provided by the protocol
     * @param isCleanSesion         should this be treated as a durable subscription
     * @param qos                   the level in which the messages would be excahnged this will be either 0,1 or 2
     * @param subscriptionChannelID the id of the channel that would be unique accross the cluser
     * @param queueIdentifier       the identifier which will represent the queue will be applicable only when durable
     * @param isTopicBound          should the representation of the object a queue or a topic
     * @param isActive              is the subscription active it will be inactive during removal
     * @return the andes specific object that will be registered in the cluster
     * @throws MQTTException
     */
    private MQTTLocalSubscription createSubscription(MQTTopicManager channel, String topic, String clientID,
                                                     String mqttClientID, boolean isCleanSesion, int qos,
                                                     UUID subscriptionChannelID, String queueIdentifier,
                                                     boolean isTopicBound, boolean isActive) throws MQTTException {
        //Will create a new local subscription object
        final String isBoundToTopic = "isBoundToTopic";
        final String subscribedNode = "subscribedNode";
        final String isDurable = "isDurable";

        final String myNodeID = ClusterResourceHolder.getInstance().getClusterManager().getMyNodeID();
        MQTTLocalSubscription localTopicSubscription = new MQTTLocalSubscription(MQTT_TOPIC_DESTINATION + "=" + topic
                + "," + MQTT_QUEUE_IDENTIFIER + "=" + queueIdentifier + "," + isBoundToTopic + "=" + isTopicBound + "," +
                subscribedNode + "=" + myNodeID + "," + isDurable + "=" + !isCleanSesion);
        localTopicSubscription.setIsTopic(isTopicBound);
        if (isTopicBound) {
            localTopicSubscription.setTargetBoundExchange(AMQPUtils.TOPIC_EXCHANGE_NAME);
        } else {
            localTopicSubscription.setTargetBoundExchange(AMQPUtils.DIRECT_EXCHANGE_NAME);
        }
        localTopicSubscription.setMqqtServerChannel(channel);
        localTopicSubscription.setChannelID(subscriptionChannelID);
        localTopicSubscription.setTopic(topic);
        localTopicSubscription.setSubscriptionID(clientID);
        localTopicSubscription.setMqttSubscriptionID(mqttClientID);
        localTopicSubscription.setSubscriberQOS(qos);
        localTopicSubscription.setIsActive(isActive);

        return localTopicSubscription;

    }

}
