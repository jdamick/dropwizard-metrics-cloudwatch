[![Build Status](https://travis-ci.org/jdamick/dropwizard-metrics-cloudwatch.svg?branch=master)](https://travis-ci.org/jdamick/dropwizard-metrics-cloudwatch)

dropwizard-metrics-cloudwatch
=============================

Dropwizard Metrics plugin for reporting to AWS CloudWatch.



Add Dependency
===

TBD
```
compile 'com.damick:dropwizard-metrics-cloudwatch:xxx'
```


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

