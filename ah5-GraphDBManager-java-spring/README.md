# GraphDBManager 5.2.2

GraphDBManager is a copy of the Arrowhead 5.2.2 Java [DataManager](../ah5-DataManager-java-spring/README.md) (copied 28 October 2026) whose historian is kept in [Ontotext GraphDB Free](https://graphdb.ontotext.com/), an RDF triple store, instead of MySQL. The API, SenML validation, certificate-bound ConsumerAuthorization checks and the sysMonitor publisher are DataManager's; only names and persistence differ.

## Arrowhead names

- System: `GraphDBManager` (certificate `GraphDBManager.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`, profile `sy`)
- Services: `graphDataProxy`, `graphDataHistorian` and `sysMonitor`, version `1.0.0`. DataManager's `dataProxy` and `dataHistorian` are reserved for DataManager in ServiceRegistry (`ReservedServiceDefinitions`), so this copy offers its own definitions; consumers that should use it orchestrate these names.
- Interfaces: `generic_https` with policy `CERT_AUTH` (re-registered at start when SSL and `service.authorization.enabled` are on); `generic_amqps` for `sysMonitor`
- Operations: `list-systems`, `list-services`, `get-data`, `put-data`; `echo`, `power`, `status` for `sysMonitor`
- Port: 8467

| Operation | Method | graphDataProxy path | graphDataHistorian path |
| --- | --- | --- | --- |
| `list-systems` | GET | `/graphdbmanager/data-proxy/systems` | `/graphdbmanager/data-historian/systems` |
| `list-services` | GET | `/graphdbmanager/data-proxy/systems/{systemName}/services` | `/graphdbmanager/data-historian/systems/{systemName}/services` |
| `get-data` | GET | `/graphdbmanager/data-proxy/systems/{systemName}/services/{serviceName}/data` | `/graphdbmanager/data-historian/systems/{systemName}/services/{serviceName}/data` |
| `put-data` | PUT | `/graphdbmanager/data-proxy/systems/{systemName}/services/{serviceName}/data` | `/graphdbmanager/data-historian/systems/{systemName}/services/{serviceName}/data` |

As in DataManager, `graphDataProxy` is an in-memory latest-value cache and `graphDataHistorian` the persistent store; historian queries take `from`, `to`, `count`, repeated `signals` and `signalCount`. `put-data` answers 204. The sysMonitor `echo` is `degraded` while GraphDB cannot be queried.

## Persistence in GraphDB

`GraphDBHistorianStore` (`eu.arrowhead.graphdbmanager.graphdb`) talks to one GraphDB repository over the RDF4J HTTP protocol (`rdf4j-client` 5.1.2). Every SenML pack written with `put-data` becomes, in the named graph `<urn:arrowhead:graphdbmanager:historian>`:

```turtle
@prefix gdm: <https://arrowhead.eu/ns/graphdbmanager#> .
<urn:uuid:…> a gdm:SenMLPack ;
    gdm:systemName "RandomDataProducer" ; gdm:serviceName "randomNumber" ;
    gdm:baseTime 1.79e9 ; gdm:minTime … ; gdm:maxTime … ;   # xsd:double, seconds
    gdm:payload "[…normalized SenML JSON…]" ; gdm:createdAt "…"^^xsd:dateTime ;
    gdm:measurement <urn:uuid:…#m0> .
<urn:uuid:…#m0> a gdm:Measurement ;
    gdm:name "random-number" ; gdm:unit "1" ; gdm:time 1.79e9 ;  # absolute time
    gdm:value 0.42 .                                            # or gdm:stringValue / gdm:booleanValue
```

`get-data` returns the stored `gdm:payload` packs (newest base time first), so its answers equal DataManager's; the `gdm:Measurement` resources exist for SPARQL users of the repository. Query values are bound as RDF literals, never concatenated into SPARQL. There is no relational database, JPA or database log appender: the vendored common library under `vendor/` lost `common/jpa/**`, `LogService`, `LogValidation` and the log4j2 JDBC connection factory (recorded in `vendor/PROVENANCE.json`).

| Property | Default | Meaning |
| --- | --- | --- |
| `graphdb.url` | `http://127.0.0.1:7200` | GraphDB server |
| `graphdb.repository` | `graphdbmanager` | Repository id (must exist) |
| `graphdb.username` / `graphdb.password` | empty | GraphDB user; the deployments use `graphdbmanager` and `${ah.graphdb-password}` from a secret file |

### The GraphDB container

`deployment/graphdb/` builds `ontotext/graphdb:10.8.5` into a container that runs as UID 10001 with a read-only root file system; its data is the volume at `/opt/graphdb/home`. `entrypoint.sh` starts GraphDB and then, on every start (idempotent):

1. creates the repository from `repository-config.ttl` if missing (GraphDB-Free sail, `empty` ruleset: no inference),
2. sets the `admin` password from `GRAPHDB_ADMIN_PASSWORD_FILE`,
3. creates or updates user `graphdbmanager` with `READ_REPO_graphdbmanager` and `WRITE_REPO_graphdbmanager` only, password from `GRAPHDB_USER_PASSWORD_FILE`,
4. turns GraphDB security on and free (anonymous) access off, then marks the container healthy.

Passwords are read only from files and go to curl through a temporary netrc file and stdin, never as arguments or log lines; they may contain only `A-Z a-z 0-9 . _ -`. GraphDB speaks plain HTTP: it has no published port and sits on an internal network joined only by GraphDBManager (the MySQL DataManager used TLS to its database; GraphDB TLS is not configured). A rotated **user** password takes effect on the next GraphDB restart; a rotated **admin** password is refused once security is on (the container stops with "admin secret is not accepted"), so change it in GraphDB first (Workbench or `PUT /rest/security/users/admin` with the old password) and then in the secret.

## Build

Java 21 and Maven 3.9 (no Arrowhead parent POM or installed library: the common sources are vendored). `mvn` need not be on the host:

```bash
docker run --rm -v "$PWD":/src -v "$HOME/.m2":/root/.m2 -w /src maven:3.9.9-eclipse-temurin-21 mvn -B clean verify
```

The first build needs Maven Central for RDF4J; afterwards `-o` works. The result is `target/arrowhead-graphdbmanager-5.2.2.jar`; `scripts/verify.sh` also checks the jar contents. Unit tests cover the historian service (store mocked), SenML validation, the proxy, the health indicator and the operation resolver/authorization filter.

## Deploy in the Java test cloud

`../../Arrowhead/Arrowhead-test-cloud-java` (Compose project `arrowhead-v52-test-cloud`) runs this folder as services `graphdbmanager-graphdb` and `graphdbmanager` (runtime image `graphdbmanager-java/Dockerfile`, which copies the prebuilt jar through the additional context `gdmsrc`). From that folder:

```bash
./scripts/build-java-jars.sh
```

```bash
make up-graphdbmanager
```

```bash
make smoke-graphdbmanager
```

`build-java-jars.sh` builds this jar with the other Java jars (offline). `make up-graphdbmanager` on a running cloud issues the certificate and PKCS#12 stores (`generate-x509.sh`: `graphdbmanager.ext`, store password `AH_STORE_PW_GDM`), provisions the identity (`AH_PW_GDM`, `provision-authentication-identities.sh`), loads the AMQP access list (`amqp/acl`: publish `TestCloud.sysmonitor.GraphDBManager.GraphDBManager.#`), applies the grants (`configure-graphdbmanager-policies.sh`; GraphDBManager is a verifier in `configure-orchestration-dependency-policies.sh` and a sysMonitor provider in `configure-all-sysmonitor-policies.sh`), then builds and starts only the two containers (`--no-deps`, so running dependencies are not rebuilt). A full `make up` does the same steps in its order. The GraphDB secrets are `AH_GRAPHDB_ADMIN_GRAPHDBMANAGER` and `AH_GRAPHDB_PW_GRAPHDBMANAGER` (generated by `rotate-secrets.sh generate` like every other key); the internal network `db-graphdbmanager` has the fixed subnet `GRAPHDBMANAGER_DB_SUBNET` (default `10.252.215.0/24`) because Docker's default address pools are exhausted on the test host.

`smoke-graphdbmanager.sh` acts as SmokeClient and uses only orchestration: for each of `graphDataHistorian` and `graphDataProxy` a DSO pull for `put-data`, `get-data` and `list-systems`, a `PUT` of a SenML pack, a `GET` that must return its value, and `list-systems` naming SmokeClient; then a SPARQL count in GraphDB (as user `graphdbmanager`) that must find the pack's measurement, and an anonymous request that GraphDB must refuse (401). `smoke-test.sh` also checks GraphDBManager (mTLS + identity token), and `smoke-security.sh` probes port 8467 (accepts the cloud probe, refuses the revoked one).

Grants (`configure-graphdbmanager-policies.sh`): both services are readable by LocalCloudManager and SmokeClient; `put-data` is granted to RandomDataProducer and SmokeClient. RandomDataProducer itself still orchestrates `dataHistorian` (DataManager); to feed GraphDBManager a producer orchestrates `graphDataHistorian`.

## Standalone deployment

`compose.yaml` here runs the system and its GraphDB on an existing cloud network without the test-cloud scripts. Copy `.env.example` to `.env`, provision in `security/` the system store `GraphDBManager.p12` (alias `GraphDBManager.<LOCAL_CLOUD>.<ORGANIZATION>.arrowhead.eu`), `truststore.p12`, `store-password`, `identity-password`, `graphdb-password`, `graphdb-admin-password` and `crl/ca.crl`, plus the identity, AMQP ACL entry and ConsumerAuthorization grants in the cloud, then:

```bash
docker compose up -d --build
```

The image runs `mvn -B clean verify` itself and starts with `config/application.properties` (mTLS, outsourced authentication, `CERT_AUTH`, service authorization, GraphDB at `graphdbmanager-graphdb`). `docker compose down -v` erases the GraphDB volume.

## Specifications

[SysD, SysDD and an SD and IDD for each produced service](documentation/v5.2.2/README.md) (`graphDataProxy`, `graphDataHistorian`, `sysMonitor`), written with the official Arrowhead LaTeX templates; the PDFs are beside the sources and `documentation/v5.2.2/build-docs.sh` rebuilds them. They were derived from DataManager's set and edited by hand (not by `documentation-tools`); the SysDD describes the RDF model and the GraphDB deployment.

## Listening port

Default **8467** (`server.port` in the packaged `application.properties`). Override with `server.port` in `config/application.properties`, `SERVER_PORT=8467` or `--server.port=8467`.

See [LISTENING-PORTS.md](../LISTENING-PORTS.md) for precedence and all systems.
