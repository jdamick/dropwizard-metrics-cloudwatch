/**
 * Copyright 2014 Jeffrey Damick, All rights reserved.
 */

package com.damick.dropwizard.metrics.cloudwatch;


import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.common.collect.Lists;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import java.util.concurrent.Future;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CloudWatchReporterFactoryTest {
    @Test
    public void isDiscoverable() throws Exception {
        assertThat(new DiscoverableSubtypeResolver().getDiscoveredSubtypes())
                .contains(CloudWatchReporterFactory.class);
    }

    @Test
    public void verifySendingToCloudWatch() throws Exception {
        CloudWatchReporterFactory factory = new CloudWatchReporterFactory();

        MetricRegistry registry = new MetricRegistry();
        Counter counter = registry.counter(MetricRegistry.name(this.getClass(), "test machine=123*"));


        AmazonCloudWatchAsync mockClient = mock(AmazonCloudWatchAsync.class);

        final Future<Void> mockFuture = mock(Future.class);
        when(mockClient.putMetricDataAsync(any(PutMetricDataRequest.class))).thenReturn(mockFuture);
        when(mockClient.putMetricDataAsync(any(PutMetricDataRequest.class))).thenAnswer(new Answer<Future>() {
            @Override
            public Future answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();

                if (args.length > 0 && args[0] != null) {
                    PutMetricDataRequest req = (PutMetricDataRequest) args[0];
                    assertEquals(req.getNamespace(), "myspace");
                    for (MetricDatum datum : req.getMetricData()) {
                        System.out.println(datum.toString());
                        assertTrue(datum.toString().contains("env"));
                    }
                }

                return mockFuture;
            }
        });

        factory.setClient(mockClient);
        factory.setAwsAccessKeyId("fakeKey");
        factory.setAwsSecretKey("fakeSecret");
        factory.setNamespace("myspace");
        factory.setGlobalDimensions(Lists.newArrayList("env=dev"));
        ScheduledReporter reporter = factory.build(registry);

        for (int i = 0; i < 200; i++) {
            counter.inc();
        }
        reporter.report();
        verify(mockClient.putMetricDataAsync(any(PutMetricDataRequest.class)), times(1));
    }

    @Test
    public void verifyDefaultProviderChainIsUsed() throws Exception {
        System.setProperty("aws.accessKeyId", "fake");
        System.setProperty("aws.secretKey", "fake");
        try {
            CloudWatchReporterFactory factory = new CloudWatchReporterFactory();

            MetricRegistry registry = new MetricRegistry();
            Counter counter = registry.counter(MetricRegistry.name(this.getClass(), "test machine=123*"));
            counter.inc();
            factory.build(registry).report();
            // expecting a 403
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretKey");
        }
    }
}
