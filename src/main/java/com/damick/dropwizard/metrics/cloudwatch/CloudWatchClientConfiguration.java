package com.damick.dropwizard.metrics.cloudwatch;

import com.amazonaws.ClientConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.security.SecureRandom;

/**
 *
 */
public class CloudWatchClientConfiguration extends ClientConfiguration {
    public CloudWatchClientConfiguration() {
        super();
    }
    public CloudWatchClientConfiguration(ClientConfiguration other) {
        super(other);
    }
    @JsonIgnore
    public void setSecureRandom(SecureRandom secureRandom) {
        super.setSecureRandom(secureRandom);
    }
}
