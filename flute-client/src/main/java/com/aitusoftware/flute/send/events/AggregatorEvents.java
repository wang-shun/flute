/*
 * Copyright 2016 - 2017 Aitu Software Limited.
 *
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
package com.aitusoftware.flute.send.events;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface AggregatorEvents
{
    /**
     * Called when a histogram sender instance cannot assign a sender with a new socket to the persistor
     * @param e the exception
     */
    void failedToRegisterSenderWithSocket(final IOException e);

    /**
     * Called when a socket write to the persistor fails
     * @param identifier the identifier of the metric
     * @param e the exception
     */
    void failedToSendDataForSender(final String identifier, final IOException e);

    /**
     * Called when the sender select call fails
     * @param e the exception
     */
    void selectFailed(final IOException e);

    /**
     * Called when an unexpected exception occurs during the send loop
     * @param e the exception
     */
    void exceptionInSendLoop(final RuntimeException e);

    /**
     * Called when the system fails to establish a connection to the persistor
     * @param address the target address
     * @param e the exception
     */
    void failedToConnectToPersistor(final SocketAddress address, final IOException e);

    /**
     * Called when queuing of data failed, for example if data rate exceeds the maximum send rate
     * @param identifier identifier of the metric
     * @param buffer the data that was going to be queued
     */
    void sendQueueBufferOverflow(final String identifier, final ByteBuffer buffer);
}
