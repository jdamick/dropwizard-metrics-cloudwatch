[![Build Status](https://travis-ci.org/jdamick/dropwizard-metrics-cloudwatch.svg?branch=master)](https://travis-ci.org/jdamick/dropwizard-metrics-cloudwatch)  

Tested against dropwizard: 0.7.1 & 0.8.1 & 0.9.1

dropwizard-metrics-cloudwatch
=============================

Dropwizard Metrics plugin for reporting to AWS CloudWatch.



Add Dependency
===


Find the latest release at maven central [http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22dropwizard-metrics-cloudwatch%22](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22dropwizard-metrics-cloudwatch%22)


Builds upon the the great work from [blacklocus/metrics-cloudwatch](https://github.com/blacklocus/metrics-cloudwatch)

Using
=====

Add to your configuration yaml file:

```
metrics:
  reporters:
    - type: cloudwatch
      namespace: some_namespace
      globalDimensions:
        - env=dev  # for example
      awsSecretKey: <optional>
      awsAccessKeyId: <optional>
      awsRegion: <optional: region name to use for cloudwatch reporting>
```

- Namespace must follow the format:
```
 "0-9A-Za-z" plus "."(period), "-" (hyphen), "_" (underscore), "/" (slash), "#" (hash), and ":" (colon)
```
See: [CloudWatch Concepts](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/cloudwatch_concepts.html)

- If neither awsAccessKeyId or awsSecretKey are set, then the
[DefaultAWSCredentialProviderChain](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)
will be used.



### LICENSE

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
