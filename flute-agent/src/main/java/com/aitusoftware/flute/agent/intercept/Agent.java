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
package com.aitusoftware.flute.agent.intercept;

import com.aitusoftware.flute.agent.annotation.FluteMetric;
import com.aitusoftware.flute.agent.annotation.MetricNameSubstitution;
import com.aitusoftware.flute.config.HistogramConfig;
import com.aitusoftware.flute.config.PublicationIntervalConfig;
import com.aitusoftware.flute.factory.RecordingTimeTrackerFactory;
import com.aitusoftware.flute.record.TimeTracker;
import com.aitusoftware.flute.send.events.LoggingAggregatorEvents;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.aitusoftware.flute.config.RequiredProperties.requiredProperty;
import static com.aitusoftware.flute.config.SocketAddressParser.fromAddressSpec;

public final class Agent
{
    public static void premain(final String arguments, final Instrumentation instrumentation)
    {
        final Properties properties = new Properties();
        try
        {
            properties.load(loadResource(arguments));
            final RecordingTimeTrackerFactory timeTrackerFactory = createTimeTrackerFactory(properties);
            performInstrumentation(instrumentation, timeTrackerFactory);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream loadResource(final String arguments) throws FileNotFoundException
    {
        return new FileInputStream(arguments);
    }

    static void load(final Properties properties) throws IOException
    {
        final RecordingTimeTrackerFactory timeTrackerFactory = createTimeTrackerFactory(properties);
        performInstrumentation(ByteBuddyAgent.install(), timeTrackerFactory);
    }

    private static RecordingTimeTrackerFactory createTimeTrackerFactory(final Properties properties) throws IOException
    {
        final HistogramConfig histogramConfig = HistogramConfig.fromFluteProperties(properties);
        final SocketAddress recordingAddress = fromAddressSpec(requiredProperty("flute.client.reporting.tcp.address", properties));
        final PublicationIntervalConfig publicationIntervalConfig = PublicationIntervalConfig.fromFluteProperties(properties);
        return new RecordingTimeTrackerFactory().
                publishingTo(recordingAddress).
                publishingEvery(publicationIntervalConfig.getInterval(), publicationIntervalConfig.getUnit()).
                withHistogramConfig(histogramConfig).
                withMultiThreadedAccess(true).
                withSenderEvents(new LoggingAggregatorEvents());
    }

    private static void performInstrumentation(final Instrumentation instrumentation,
                                               final RecordingTimeTrackerFactory timeTrackerFactory)
    {
        final ConcurrentMap<String, TimeTrackerInterceptor> trackerByMethodMap = new ConcurrentHashMap<>();
        AgentBuilder agentBuilder = new AgentBuilder.Default().with(new AgentBuilder.Listener()
        {
            @Override
            public void onTransformation(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module, final DynamicType dynamicType)
            {
            }

            @Override
            public void onIgnored(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module)
            {
            }

            @Override
            public void onError(final String typeName, final ClassLoader classLoader, final JavaModule module, final Throwable throwable)
            {
                System.out.printf("onError: %s%n", typeName);
                throwable.printStackTrace();
            }

            @Override
            public void onComplete(final String typeName, final ClassLoader classLoader, final JavaModule module)
            {
            }
        });

        agentBuilder = instrumentType(agentBuilder, isFluteMetricMethod(),
                ElementMatchers.declaresMethod(isFluteMetricMethod()), timeTrackerFactory,
                trackerByMethodMap);
        final ClassFileTransformer classFileTransformer = agentBuilder.installOn(instrumentation);
    }

    private static final class Instrumenter implements Implementation
    {
        private final RecordingTimeTrackerFactory timeTrackerFactory;
        private final ConcurrentMap<String, TimeTrackerInterceptor> trackerByMethodMap;

        public Instrumenter(
                final RecordingTimeTrackerFactory timeTrackerFactory,
                final ConcurrentMap<String, TimeTrackerInterceptor> trackerByMethodMap)
        {
            this.timeTrackerFactory = timeTrackerFactory;
            this.trackerByMethodMap = trackerByMethodMap;
        }

        @Override
        public ByteCodeAppender appender(final Target implementationTarget)
        {
            return new ByteCodeAppender()
            {
                @Override
                public Size apply(final MethodVisitor methodVisitor, final Context implementationContext,
                                  final MethodDescription instrumentedMethod)
                {
                    if(trackerByMethodMap.containsKey(instrumentedMethod.toGenericString()))
                    {
                        final TimeTrackerInterceptor delegate = trackerByMethodMap.get(instrumentedMethod.toGenericString());
                        return MethodDelegation.to(delegate).
                                appender(implementationTarget).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    return SuperMethodCall.INSTANCE.appender(implementationTarget).
                            apply(methodVisitor, implementationContext, instrumentedMethod);
                }
            };

        }

        @Override
        public InstrumentedType prepare(final InstrumentedType instrumentedType)
        {
            InstrumentedType updated = instrumentedType;
            final MethodList<MethodDescription.InDefinedShape> declaredMethods =
                    instrumentedType.getDeclaredMethods().filter(isFluteMetricMethod());
            for (MethodDescription.InDefinedShape declaredMethod : declaredMethods)
            {
                final TimeTrackerInterceptor timeTrackerInterceptor = trackerByMethodMap.computeIfAbsent(
                        declaredMethod.toGenericString(), k ->
                {
                    final TimeTracker timeTracker = createTimeTracker(timeTrackerFactory, declaredMethod);
                    return new TimeTrackerInterceptor(timeTracker);
                });
                updated = MethodDelegation.to(timeTrackerInterceptor).
                        filter(ElementMatchers.is(declaredMethod)).prepare(updated);
            }
            return updated;
        }
    }

    private static ElementMatcher.Junction<MethodDescription> isFluteMetricMethod()
    {
        return ElementMatchers.declaresAnnotation(ElementMatchers.annotationType(FluteMetric.class));
    }

    private static AgentBuilder instrumentType(final AgentBuilder agentBuilder,
                                               final ElementMatcher.Junction<MethodDescription> methodMatcher,
                                               final ElementMatcher.Junction<TypeDescription> typeMatcher,
                                               final RecordingTimeTrackerFactory timeTrackerFactory,
                                               final ConcurrentMap<String, TimeTrackerInterceptor> trackerByMethodMap)
    {
        return agentBuilder.
                type(typeMatcher).
                transform(new AgentBuilder.Transformer()
        {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription arg1, ClassLoader arg2)
            {
                return builder.
                        method(methodMatcher).
                        intercept(new Instrumenter(timeTrackerFactory, trackerByMethodMap));
            }

        });
    }

    private static TimeTracker createTimeTracker(final RecordingTimeTrackerFactory timeTrackerFactory, final MethodDescription.InDefinedShape declaredMethod)
    {
        String metricName = declaredMethod.getName();
        try
        {
            final FluteMetric fluteMetric = declaredMethod.getDeclaredAnnotations().ofType(FluteMetric.class).load();
            metricName = MetricNameSubstitution.INSTANCE.getMetricName(declaredMethod.getName(), fluteMetric);
        }
        catch (ClassNotFoundException e)
        {
            System.err.printf("Unable to load annotation class: %s", e.getMessage());
        }
        return timeTrackerFactory.withIdentifer(metricName).create();
    }
}