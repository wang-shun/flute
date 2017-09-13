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
package com.aitusoftware.flute.acceptance.framework;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpOps
{
    static <T> T get(final String url, final Class<T> cls)
    {
        final HttpTransport transport = new NetHttpTransport();
        final HttpRequestFactory requestFactory = transport.createRequestFactory();
        try
        {
            final HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(url));
            final HttpResponse response = httpRequest.execute();
            final int responseCode = response.getStatusCode();
            if (responseCode != 200)
            {
                throw new IOException("Received response code: " + responseCode);
            }

            final T result = new Gson().fromJson(new InputStreamReader(response.getContent()), cls);
            disconnect(response);
            return result;
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            shutdown(transport);
        }
    }

    static ApiCallResult post(final String url, final Map<String, Object> postData)
    {
        final HttpTransport transport = new NetHttpTransport();
        try
        {
            final StringWriter buffer = new StringWriter();
            new Gson().toJson(postData, buffer);
            final HttpRequestFactory requestFactory = transport.createRequestFactory();
            final HttpRequest httpRequest = requestFactory.buildPostRequest(new GenericUrl(url),
                    new ByteArrayContent("application/json", buffer.toString().getBytes(StandardCharsets.UTF_8))).
                    setThrowExceptionOnExecuteError(false);

            final HttpResponse response = httpRequest.execute();
            final int responseCode = response.getStatusCode();
            final ApiCallResult apiCallResult = new ApiCallResult(responseCode, response.getStatusMessage());
            disconnect(response);
            return apiCallResult;
        }
        catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            shutdown(transport);
        }
    }

    private static void shutdown(final HttpTransport transport)
    {
        try
        {
            transport.shutdown();
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    private static void disconnect(final HttpResponse response)
    {
        try
        {
            response.disconnect();
        }
        catch (IOException e)
        {
            // ignore
        }
    }
}
