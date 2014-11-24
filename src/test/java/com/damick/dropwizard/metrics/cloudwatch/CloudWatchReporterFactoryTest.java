/**
 * Copyright 2014 Jeffrey Damick, All rights reserved.
 */

package com.damick.dropwizard.metrics.cloudwatch;


import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import java.util.concurrent.Future;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

        Future<Void> mockFuture = mock(Future.class);
        when(mockClient.putMetricDataAsync(any(PutMetricDataRequest.class))).thenReturn(mockFuture);

        factory.setClient(mockClient);
        factory.setAwsAccessKeyId("fakeKey");
        factory.setAwsSecretKey("fakeSecret");
        factory.setNamespace("myspace");
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
