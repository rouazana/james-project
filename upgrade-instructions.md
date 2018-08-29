# Upgrade instructions

This document covers every changes an Ops needs to be aware of when running James.

## Unreleased

Note: this section is in progress. It will be updated during all the development process until the release.

Changes to apply between 3.1.x and 3.2.x will be reported here.

## JMAPFiltering mailet is required for JMAP capable servers

Date: XXXX

SHA-1: XXXX

JIRA: https://issues.apache.org/jira/browse/JAMES-2529

Required: Yes

Concerned products: Cassandra Guice products

This mailet allow users filtering rules to be applied for incoming emails.

### Upgrade procedure

Add this line before the `LocalDelivery` mailet of your `transport` processor:

```
<mailet match="RecipientIsLocal" class="org.apache.james.jmap.mailet.filter.JMAPFiltering"/>
```

## Cassandra 3.11.3 upgrade

Date: 03/08/2018

SHA-1: de0fa8a3df69f50cbc0684dfb1b911ad497856d7

JIRA: https://issues.apache.org/jira/browse/JAMES-2514

Required: Yes

Concerned products: Cassandra Guice products

James Cassandra Guice now officially uses Cassandra 3.11.3 as a storage backend. While performing the upgrade, the team
did not perform breaking changes. But James Cassandra Guice products are no more tested against Cassandra 2.2.x. Thus we strongly
advise our user to upgrade.

### Upgrade procedure

We will assume you installed that Cassandra had been installed with a debian package. Upgrade procedure stays similar in other cases.


1. Update Cassandra dists in `/etc/apt/sources.list.d/cassandra.list` to match 311x repository

```
deb http://www.apache.org/dist/cassandra/debian 311x main
```


2. Update Cassandra

```
$ apt-get update
$ apt-get install cassandra=3.11.3
```

3. Correct the configuration

Edit /etc/cassandra/cassandra.yaml and ensure to REALLY specify the interface cassandra is listening on (conf PB on OP.LNG)

4.ReStart Cassandra

4.1 Drain data & stop

```
$ nodetool drain
$ nodetool stop
```

4.2 start Cassandra

5. Upgrade SSTable (live update, performance degradation to expect)

```
$ nodetool upgradesstables apache_james
```

