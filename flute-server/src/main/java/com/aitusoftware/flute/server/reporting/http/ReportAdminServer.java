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
package com.aitusoftware.flute.server.reporting.http;

import com.aitusoftware.flute.config.ConnectionFactory;
import com.aitusoftware.flute.config.DatabaseConfig;
import com.aitusoftware.flute.server.config.ServerConfig;
import com.aitusoftware.flute.server.dao.jdbc.MetricIdentifierDao;
import com.aitusoftware.flute.server.http.MetricIdentifierSearchHandler;
import com.aitusoftware.flute.server.reporting.dao.ReportDao;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URL;

import static java.net.InetSocketAddress.createUnresolved;

final class ReportAdminServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportAdminServer.class);
    private static final String WELL_KNOWN_RESOURCE_PATH = "ui/index.html";

    private final ServerConfig serverConfig;
    private final DatabaseConfig metricDatabaseConfig;
    private final DatabaseConfig reportDatabaseConfig;

    ReportAdminServer(
            final ServerConfig serverConfig,
            final DatabaseConfig metricsDatabaseConfig,
            final DatabaseConfig reportDatabaseConfig)
    {
        this.serverConfig = serverConfig;
        this.metricDatabaseConfig = metricsDatabaseConfig;
        this.reportDatabaseConfig = reportDatabaseConfig;
    }

    void run() throws Exception
    {
        final InetSocketAddress socketAddress = createUnresolved("0.0.0.0", serverConfig.getHttpPort());
        final Server server = new Server(socketAddress);
        final ContextHandlerCollection handlers = new ContextHandlerCollection();

        try
        {
            final ConnectionFactory connectionFactory = new ConnectionFactory(metricDatabaseConfig);
            final MetricIdentifierDao metricIdentifierDao = new MetricIdentifierDao(connectionFactory);
            final ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirectoriesListed(false);
            final String fileSystemResourceBase = System.getProperty("flute.resource.base");
            if(fileSystemResourceBase != null)
            {
                LOGGER.info("Setting resource base to file-system location {}", fileSystemResourceBase);
                resourceHandler.setResourceBase(fileSystemResourceBase);
            }
            else
            {
                final URL wellKnownResource = Thread.currentThread().getContextClassLoader().
                        getResource(WELL_KNOWN_RESOURCE_PATH);
                if (wellKnownResource == null)
                {
                    LOGGER.warn("Unable to load UI resources from classpath");
                }
                else
                {
                    final String serverJarFile = wellKnownResource.getFile().split("!")[0];
                    final String jarUrl = String.format("jar:%s!/", serverJarFile);
                    LOGGER.info("Setting resource base to jar file {}", jarUrl);
                    final Resource jarFileResource = Resource.newResource(new URL(jarUrl));
                    resourceHandler.setBaseResource(jarFileResource);
                }
            }
            final ReportDao reportDao = new ReportDao(new ConnectionFactory(reportDatabaseConfig));
            addContextHandler(handlers, "/flute/app/report/create", ModifyReportHandler.createMode(reportDao));
            addContextHandler(handlers, "/flute/app/report/amend", ModifyReportHandler.amendMode(reportDao));
            addContextHandler(handlers, "/flute/app/report/delete", new DeleteReportHandler(reportDao));
            addContextHandler(handlers, "/flute/app/report/get", new GetReportHandler(reportDao));
            addContextHandler(handlers, "/flute/app/report/list", new ListReportsHandler(reportDao));
            addContextHandler(handlers, "/flute/app/report/spec", new ViewReportConfigHandler(reportDao, metricIdentifierDao));
            addContextHandler(handlers, "/flute/app/query/metricSearch", new MetricIdentifierSearchHandler(metricIdentifierDao));

            addContextHandler(handlers, "/flute/resources", resourceHandler);
        }
        catch(final Exception e)
        {
            addContextHandler(handlers, "/flute", new DefaultHandler()
            {
                @Override
                public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException
                {
                    response.setContentType("text/plain; charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    final PrintWriter writer = response.getWriter();
                    writer.append("Failed to configure: ").append(e.getMessage());
                    writer.flush();
                }
            });
        }

        server.setHandler(handlers);
        server.start();
        server.join();
    }

    private void addContextHandler(final ContextHandlerCollection handlers,
                                   final String contextPath,
                                   final Handler childHandler)
    {
        final ContextHandler handler = new ContextHandler();
        handler.setContextPath(contextPath);
        handler.setHandler(childHandler);
        handlers.addHandler(handler);
    }
}
