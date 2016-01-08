/**
 * Copyright 2014 Jeffrey Damick, All rights reserved.
 */
package com.damick.dropwizard.metrics.cloudwatch;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.EC2MetadataClient;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.util.EC2MetadataUtils;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import io.dropwizard.metrics.BaseReporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * A factory for {@link CloudWatchReporterFactory} instances.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>namespace</td>
 *         <td>(empty)</td>
 *         <td>The namespace for the metric data.</td>
 *     </tr>
 *     <tr>
 *         <td>globalDimensions</td>
 *         <td>(empty)</td>
 *         <td>An array of strings to use as metric dimensions. For example: env=dev</td>
 *     </tr>
 *     <tr>
 *         <td>awsSecretKey</td>
 *         <td>(empty)</td>
 *         <td>The optional AWS Secret key. (If this and awsAccessKeyId not set DefaultAWSCredentialsProviderChain is used)</td>
 *     </tr>
 *     <tr>
 *         <td>awsAccessKeyId</td>
 *         <td>(empty)</td>
 *         <td>The optional AWS Access key. (If this and awsSecretKey not set DefaultAWSCredentialsProviderChain is used)</td>
 *     </tr>
 *     <tr>
 *         <td>awsClientConfiguration</td>
 *         <td>(empty)</td>
 *         <td>The optional <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/ClientConfiguration.html">AWS Client Configuration</a>.</td>
 *     </tr>
 * </table>
 */
@JsonTypeName("cloudwatch")
public class CloudWatchReporterFactory extends BaseReporterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchReporterFactory.class);
    private static final String DEFAULT_REGION = "us-east-1";

    @JsonIgnore
    private AmazonCloudWatchAsync client;

    @NotNull
    private String namespace = "";

    @JsonIgnore
    private String awsSecretKey = null;

    @JsonIgnore
    private String awsAccessKeyId = null;

    @JsonIgnore
    private String awsRegion = DEFAULT_REGION;

    @JsonIgnore
    private String machineDimension;

    @JsonIgnore
    private Boolean ec2MetadataAvailable = null;

    @JsonIgnore
    private List<String> globalDimensions = new ArrayList<>();

    @JsonIgnore
    private CloudWatchClientConfiguration clientConfig = new CloudWatchClientConfiguration();

    @JsonProperty
    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    @JsonProperty
    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    @JsonProperty
    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    @JsonProperty
    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    @JsonProperty
    public String getAwsRegion() {
        return awsRegion;
    }

    @JsonProperty
    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @JsonProperty
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonProperty
    public String getMachineDimension() {
        return machineDimension;
    }

    @JsonProperty
    public void setMachineDimension(String machineDimension) {
        this.machineDimension = machineDimension;
    }

    @JsonProperty
    public List<String> getGlobalDimensions() {
        return globalDimensions;
    }

    @JsonProperty
    public void setGlobalDimensions(List<String> globalDimensions) {
        this.globalDimensions = globalDimensions;
    }

    @JsonProperty
    public void setAwsClientConfiguration(CloudWatchClientConfiguration clientConfig) {
        this.clientConfig = clientConfig;
    }

    @JsonProperty
    public CloudWatchClientConfiguration getAwsClientConfiguration() {
        return clientConfig;
    }

    // for testing..
    @JsonIgnore
    public void setClient(AmazonCloudWatchAsync client) {
        this.client = client;
    }

    @Override
    public ScheduledReporter build(MetricRegistry registry) {
        if (client == null) {
            if (!Strings.isNullOrEmpty(awsAccessKeyId) && !Strings.isNullOrEmpty(awsSecretKey)) {
                client = new AmazonCloudWatchAsyncClient(
                        new BasicAWSCredentials(this.awsAccessKeyId, this.awsSecretKey),
                        clientConfig, Executors.newCachedThreadPool());
            } else {
                client = new AmazonCloudWatchAsyncClient(new DefaultAWSCredentialsProviderChain(), clientConfig);
            }
            Region region = region();
            client.setRegion(region);
            LOGGER.info("CloudWatch reporting configure to send to region: {}", region);
        }

        globalDimensions.add("machine=" + machineId() + "*");

        return new CloudWatchMachineDimensionReporter(registry, this.namespace,
                globalDimensions, getFilter(), client);
    }

    protected String machineId() {
        String machine = machineDimension;
        if (machine == null && isEC2MetadataAvailable()) {
            machine = EC2MetadataUtils.getInstanceId();
        }
        if (Strings.isNullOrEmpty(machine)) {
            machine = "localhost";
        }
        return machine;
    }

    protected Region region() {
        String az = null;
        if (isEC2MetadataAvailable()) {
            az = EC2MetadataUtils.getAvailabilityZone();
        }
        String regionName = awsRegion;
        if (!Strings.isNullOrEmpty(az)) {
            regionName = az.substring(0, az.length() - 1); // strip the AZ letter
        }
        return RegionUtils.getRegion(regionName);
    }

    protected boolean isEC2MetadataAvailable() {
        if (ec2MetadataAvailable == null) {
            EC2MetadataClient client = new EC2MetadataClient();
            try {
                client.readResource("/");
                ec2MetadataAvailable = true;
            } catch (IOException e) {
                LOGGER.error("Not able to connect to EC2 Metadata Service");
                // if we have any exception, we'll assume we're not in ec2..
                ec2MetadataAvailable = false;
            }
        }
        return ec2MetadataAvailable;
    }
}
