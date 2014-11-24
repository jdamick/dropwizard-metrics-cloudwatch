/**
 * Copyright 2014 Jeffrey Damick, All rights reserved.
 */
package com.damick.dropwizard.metrics.cloudwatch;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.blacklocus.metrics.CloudWatchReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import io.dropwizard.metrics.BaseReporterFactory;
import javax.validation.constraints.NotNull;

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
 *         <td>awsSecretKey</td>
 *         <td>(empty)</td>
 *         <td>The optional AWS Secret key. (If this and awsAccessKeyId not set DefaultAWSCredentialsProviderChain is used)</td>
 *     </tr>
 *     <tr>
 *         <td>awsAccessKeyId</td>
 *         <td>(empty)</td>
 *         <td>The optional AWS Access key. (If this and awsSecretKey not set DefaultAWSCredentialsProviderChain is used)</td>
 *     </tr>
 * </table>
 */
@JsonTypeName("cloudwatch")
public class CloudWatchReporterFactory extends BaseReporterFactory {

    @JsonIgnore
    private AmazonCloudWatchAsync client;

    @NotNull
    private String namespace = "";

    @JsonIgnore
    private String awsSecretKey = null;

    @JsonIgnore
    private String awsAccessKeyId = null;


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
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty
    public void setNamespace(String namespace) {
        this.namespace = namespace;
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
                client = new AmazonCloudWatchAsyncClient(new BasicAWSCredentials(this.awsAccessKeyId, this.awsSecretKey));
            } else {
                client = new AmazonCloudWatchAsyncClient(new DefaultAWSCredentialsProviderChain());
            }
        }
        return new CloudWatchReporter(registry, this.namespace, getFilter(), client);
    }
}
