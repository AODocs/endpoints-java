[![Build Status](https://api.travis-ci.org/AODocs/endpoints-java.svg?branch=master)](https://travis-ci.org/AODocs/endpoints-java)
[![codecov](https://codecov.io/gh/AODocs/endpoints-java/branch/master/graph/badge.svg)](https://codecov.io/gh/AODocs/endpoints-java)

# Endpoints Java Framework

The Endpoints Java Framework aims to be a simple solution to assist in creation
of RESTful web APIs in Java. This repository provides several artifacts, all
in the `com.aodocs.endpoints` group:

1.  `endpoints-framework`: The core framework, required for all applications
    building Endpoints apps.
2.  `endpoints-framework-all`: Same as above, but with repackaged dependencies.
3.  `endpoints-framework-guice`: An extension for configuring Endpoints using
    Guice.
4.  `endpoints-framework-tools`: Tools for generating discovery documents,
    Swagger documents, and client libraries.

The main documents for consuming Endpoints can be found at
https://cloud.google.com/endpoints/docs/frameworks/java

## Installing to local Maven

To install test versions to Maven for easier dependency management, simply run:

    gradle install
    
## Additions to the original project

These are the most notable additions to
[the original project by Google](https://github.com/cloudendpoints/endpoints-java), currently
inactive:
- Runtime
  - Allow [adding arbitrary data](https://github.com/AODocs/endpoints-java/pull/20) to generic errors
  - [Improve returned errors](https://github.com/AODocs/endpoints-java/pull/30) on malformed JSON
  - Validate request parameters/body through [Java bean validation](https://beanvalidation.org/) provided by [Hibernate validator](https://hibernate.org/validator/)
  - Validate request content-type (must be enable through enableContentTypeValidation servlet parameter)
- Discovery and Swagger
  - [Add description on resources and resource usage as request body](https://github.com/AODocs/endpoints-java/commit/bbb1eff2bb9e7d28fc2ec17599257d0ef610531d)
  - [Support declaring resource properties as required](https://github.com/AODocs/endpoints-java/pull/41)
  - Support pattern, minimum/maximum attributes 
- Swagger
  - Generated spec is [fully compatible](https://github.com/AODocs/endpoints-java/pull/34) with 
[Cloud Endpoints Portal](https://cloud.google.com/endpoints/docs/frameworks/dev-portal-overview) (and is 100% valid Swagger spec)
  - Support [multi-API service](https://github.com/AODocs/endpoints-java/pull/40/commits/1f18d2f64f1538e63a7836a5cd52ff639fc624fd) in Endpoints Management
  - [New options](https://github.com/AODocs/endpoints-java/pull/37) to combine common parameters in same path, extract parameter refs at spec level, add error model description, customize spec title and description
  - [Add description support](https://github.com/AODocs/endpoints-java/pull/40/commits/bbb1eff2bb9e7d28fc2ec17599257d0ef610531d) for resource and resource usage
  - [Configurable naming templates](https://github.com/AODocs/endpoints-java/pull/42) for operationIds and tag names with better defaults
  - Support exclusiveMinimum/exclusiveMaximum, minLength/maxLength and minItems/maxItems attributes
- Dependency updates
  - `com.github.ben-manes.versions` plugin added, Dependecy Updates report can be generated via `./gradlew dependencyUpdates` command

Check 
[closed PRs](https://github.com/AODocs/endpoints-java/pulls?q=is%3Apr+sort%3Aupdated-desc+is%3Aclosed)
for all additions.

## Troubleshooting

* When using validation with java bean validation annotations : in order to have well named parameters instead of _arg0/arg1_ in error messages, your code must be compiled with the `-parameters` options.


## Contributing

Your contributions are welcome. Please follow the [contributor guidelines](/CONTRIBUTING.md).
