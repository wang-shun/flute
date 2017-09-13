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
package com.aitusoftware.flute.receive;

import com.aitusoftware.flute.protocol.InFlightHistogramPayload;
import com.aitusoftware.flute.protocol.Version;
import com.aitusoftware.flute.protocol.VersionCodec;
import org.HdrHistogram.Histogram;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class StreamingHistogramReceiverTest
{
    private static final String IDENTIFIER = "foo.bar";
    private static final byte[] IDENTIFIER_BYTES = IDENTIFIER.getBytes(StandardCharsets.UTF_8);
    private static final long START_TIMESTAMP = 123456789000L;
    private static final long END_TIMESTAMP = 1235467890L;
    private static final InetSocketAddress SOCKET_ADDRESS = new InetSocketAddress(44994);

    private final StreamingHistogramReceiver receiver = new StreamingHistogramReceiver(this::histogramReceived, 1000);
    private final InFlightHistogramPayload inFlightHistogramPayload = new InFlightHistogramPayload();
    private final List<CapturedData> capturedData = new ArrayList<>();
    private final ByteBuffer payload = ByteBuffer.allocate(1024);
    private final Histogram histogram = new Histogram(1000, 3);

    @Before
    public void before() throws Exception
    {
        VersionCodec.formatTo(Version.ONE, payload);

        histogram.recordValue(37L);
        inFlightHistogramPayload.encode(histogram, START_TIMESTAMP, END_TIMESTAMP, IDENTIFIER_BYTES, payload);
    }

    @Test
    public void shouldAcceptCompletePayload() throws Exception
    {
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.size(), is(1));
        assertCapturedData(this.capturedData.get(0));
    }

    @Test
    public void shouldAcceptPayloadStartingWithIncompleteVersion() throws Exception
    {
        final int endOfPayload = payload.limit();
        payload.limit(VersionCodec.FORMATTED_VERSION_LENGTH / 2);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.isEmpty(), is(true));

        payload.limit(endOfPayload);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.size(), is(1));
        assertCapturedData(this.capturedData.get(0));
    }

    @Test
    public void shouldAcceptPayloadStartingWithIncompleteHeader() throws Exception
    {
        final int endOfPayload = payload.limit();
        payload.limit(VersionCodec.FORMATTED_VERSION_LENGTH + 5);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.isEmpty(), is(true));

        payload.limit(endOfPayload);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.size(), is(1));
        assertCapturedData(this.capturedData.get(0));
    }

    @Test
    public void shouldAcceptPayloadWithIncompleteHistogram() throws Exception
    {
        final int endOfPayload = payload.limit();
        payload.limit(endOfPayload / 2);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.isEmpty(), is(true));

        payload.limit(endOfPayload);
        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.size(), is(1));
        assertCapturedData(this.capturedData.get(0));
    }

    @Test
    public void shouldHandleMultiplePayloads() throws Exception
    {
        payload.position(payload.limit());
        payload.limit(payload.capacity());
        final String otherIdentifier = "other.id";
        final long otherStartTimestamp = START_TIMESTAMP * 2;
        final long otherEndTimestamp = END_TIMESTAMP * 2;
        inFlightHistogramPayload.encode(histogram, otherStartTimestamp, otherEndTimestamp, otherIdentifier.getBytes(StandardCharsets.UTF_8), payload);

        receiver.readFrom(new ByteBufferReadableByteChannel(payload), SOCKET_ADDRESS);

        assertThat(capturedData.size(), is(2));
        assertCapturedData(this.capturedData.get(0));
        final CapturedData capturedData1 = this.capturedData.get(1);
        assertThat(capturedData1.histogram.getTotalCount(), is(1L));
        assertThat(capturedData1.startTimestamp, is(otherStartTimestamp));
        assertThat(capturedData1.endTimestamp, is(otherEndTimestamp));
        assertThat(capturedData1.identifier, is(otherIdentifier));
        assertThat(capturedData1.sender, is(SOCKET_ADDRESS));
    }

    private static void assertCapturedData(final CapturedData capturedData)
    {
        assertThat(capturedData.histogram.getTotalCount(), is(1L));
        assertThat(capturedData.startTimestamp, is(START_TIMESTAMP));
        assertThat(capturedData.endTimestamp, is(END_TIMESTAMP));
        assertThat(capturedData.identifier, is(IDENTIFIER));
        assertThat(capturedData.sender, is(SOCKET_ADDRESS));
    }

    private void histogramReceived(final InetSocketAddress sender, final CharSequence identifier,
                                   final long startTimestamp, final long endTimestamp,
                                   final Histogram histogram)
    {
        capturedData.add(new CapturedData(sender, identifier, startTimestamp, endTimestamp, histogram));
    }

    private static final class CapturedData
    {
        private final InetSocketAddress sender;
        private final CharSequence identifier;
        private final long startTimestamp;
        private final long endTimestamp;
        private final Histogram histogram;

        private CapturedData(
                final InetSocketAddress sender,
                final CharSequence identifier,
                final long startTimestamp,
                final long endTimestamp,
                final Histogram histogram)
        {
            this.sender = sender;
            this.identifier = identifier;
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
            this.histogram = histogram;
        }
    }

    private static class ByteBufferReadableByteChannel implements ReadableByteChannel
    {
        private final ByteBuffer payload;

        ByteBufferReadableByteChannel(final ByteBuffer payload)
        {
            this.payload = payload;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException
        {
            int initial = dst.position();
            dst.put(payload);
            return dst.position() - initial;
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public void close() throws IOException
        {

        }
    }
}