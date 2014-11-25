/**
 * Copyright 2014 Jeffrey Damick, All rights reserved.
 */

package com.damick.dropwizard.metrics.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.blacklocus.metrics.CloudWatchReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class CloudWatchMachineDimensionReporter extends CloudWatchReporter {
    private final List<String> dimensions;


    public CloudWatchMachineDimensionReporter(MetricRegistry registry, String metricNamespace, List<String> dimensions,
                                              MetricFilter metricFilter, AmazonCloudWatchAsync cloudWatch) {

        super(registry, metricNamespace, metricFilter, cloudWatch);
        this.dimensions = dimensions;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        String append = Joiner.on(' ').join(dimensions);
        super.report(transformKeys(gauges, append), transformKeys(counters, append),
                transformKeys(histograms, append), transformKeys(meters, append),
                transformKeys(timers, append));
    }

    protected <T> SortedMap<String, T> transformKeys(SortedMap<String, T> map, String appendKey) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        ImmutableSortedMap.Builder builder = ImmutableSortedMap.naturalOrder();
        if (map.comparator() != null) {
            builder = new ImmutableSortedMap.Builder(map.comparator());
        }

        for (Map.Entry<String, T> entry : map.entrySet()) {
            builder.put(entry.getKey() + " " + appendKey, entry.getValue());
        }

        return builder.build();
    }
}
