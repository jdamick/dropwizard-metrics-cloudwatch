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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.Jackson;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.spi.valuehandling.ValidatedValueUnwrapper;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    public void verifyConfigurable() throws Exception {
        ObjectMapper mapper = Jackson.newObjectMapper();

        // dropwizard 0.9.1 changed the validation wiring a bit..
        Class<ValidatedValueUnwrapper> optValidatorClazz = (Class<ValidatedValueUnwrapper>) Class
                .forName("io.dropwizard.validation.valuehandling.OptionalValidatedValueUnwrapper");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        if (optValidatorClazz != null) {
            validator = Validation.byProvider(HibernateValidator.class).configure()
                    .addValidatedValueHandler(optValidatorClazz.newInstance())
                    .buildValidatorFactory().getValidator();
        }

        ConfigurationFactory<CloudWatchReporterFactory> configFactory =
                new ConfigurationFactory<>(CloudWatchReporterFactory.class,
                        validator, mapper, "dw");
        CloudWatchReporterFactory f = configFactory.build(new File(Resources.getResource("cw.yml").getFile()));

        assertEquals("[env=default]", f.getGlobalDimensions().toString());
        assertEquals("us-east-1", f.getAwsRegion());
        assertEquals("a.b", f.getNamespace());
        assertEquals("XXXXX", f.getAwsSecretKey());
        assertEquals("11111", f.getAwsAccessKeyId());
        assertEquals("p.neustar.biz", f.getAwsClientConfiguration().getProxyHost());
        assertNull(f.getAwsClientConfiguration().getProxyUsername());
    }

    @Test
    public void verifyDefaults() throws Exception {
        CloudWatchReporterFactory factory = new CloudWatchReporterFactory();
        assertFalse(factory.region().getName().isEmpty());
        assertFalse(factory.machineId().isEmpty());

        factory.setAwsRegion("ap-southeast-2");
        factory.setMachineDimension("10.0.0.1");
        assertEquals("ap-southeast-2", factory.region().getName());
        assertEquals("10.0.0.1", factory.machineId());
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
