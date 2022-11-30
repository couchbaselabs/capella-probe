# Capella Probe

Connects to a Couchbase Capella cluster using the Couchbase Java SDK.

## Requirements

* Java 8 or later.

## Usage

Edit `config/probe-config.json` to match your database address, credentials, and bucket name.

If desired, set `captureTraffic` to true.

CAUTION: The password is included in the log output when `captureTraffic` is true. 

Run the probe with this command:

```shell
./bin/capella-probe
```

## Troubleshooting

* Remember to add your IP address to the list of allowed IPs.
* For advanced troubleshooting, use [Couchbase SDK Doctor](https://docs.couchbase.com/server/current/sdk/sdk-doctor.html).
