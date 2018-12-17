DynamoDB with Kotlin
====================

[![Build Status](https://img.shields.io/travis/bringmeister/dynamodb-with-kotlin/master.svg)](https://travis-ci.org/bringmeister/dynamodb-with-kotlin)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/bringmeister/dynamodb-with-kotlin/master/LICENSE)

This is a collection of DynamoDB examples with Kotlin and Spring Boot.
Every example highlights a different aspect of DynamoDB and is located in a single self-containd test.

## Examples

### Auditing

Shows how to implement _createdAt_ and _lastModifiedAt_ timestamps.

**Note:** The _createdAt_ date doesn't work.

### Composite Key

Shows how to implement a composite key made up of two parts.

### DynamoBee

DynamoBee is a migration tool for DynamoDB, similar to as Liquibase or Flyway.
This example shows how to setup and migrate a DynamoDB table with it.

### JSON

Shows how to save a complex object as JSON.

### JSON Migration

Shows an approach how JSON could migrated.

**Note:** This approach is highly opinionated and hand-crafted.

### Queries

Shows how to query entities with indexes.

### Simple

Shows a very simple example and the basic setup.