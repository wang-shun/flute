/*
 * Copyright 2016 Aitu Software Limited.
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

import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class LoggingAggregatorEvents implements AggregatorEvents
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAggregatorEvents.class);

    @Override
    public void failedToRegisterSenderWithSocket(final IOException e)
    {
        LOGGER.warn("Failed to register sender with socket", e);
    }

    @Override
    public void failedToSendDataForSender(final String identifier, final IOException e)
    {
        LOGGER.warn("Failed to send data for sender: " + identifier, e);
    }

    @Override
    public void selectFailed(final IOException e)
    {
        LOGGER.warn("Select operation failed", e);
    }

    @Override
    public void exceptionInSendLoop(final RuntimeException e)
    {
        LOGGER.warn("Exception in send loop", e);
    }

    @Override
    public void failedToConnectToPersistor(SocketAddress address, final IOException e)
    {
        LOGGER.warn(String.format("Failed to connect to persistor, address: %s", address), e);
    }

    @Override
    public void sendQueueBufferOverflow(final String identifier, final ByteBuffer buffer)
    {
        LOGGER.warn("Send queue overflow for metric {}", identifier);
    }
}
