# Federating the member clouds

A System of LocalClouds with two or more members is federated through the organisation: one organisation CA cross-signs
each member cloud CA, and one relay (an MQTT broker with mutual TLS) carries the Gatekeeper and GateTunnel traffic between
the clouds. Nothing secret moves between hosts: each cloud CA key stays on its host, the organisation CA key on the
organisation host.

## Roles

| Host | Holds | Runs |
|---|---|---|
| Organisation host (`org/`) | organisation CA key, relay key | the relay (`org/relay.sh`), CRL issuing (`org/crl.sh`) |
| Member host (`clouds/<Cloud>/`) | its cloud CA key, all its system keys and passwords | the local cloud |

The organisation host may be a member host too (as in a single-machine test); keep `org/state/` and the clouds'
`work/` folders apart all the same.

## Steps

1. **Organisation host**: `org/init-org.sh` creates the organisation CA, its CRL and the relay certificate (its SAN is the
   relay host of the design).
2. **Each member host**: `clouds/<Cloud>/install.sh` ends by writing `clouds/<Cloud>/out/<Cloud>-federation-request.tar.gz`
   (cloud CA certificate, a certificate request signed with the cloud CA key, the cloud CRL, the public relay client
   certificates). Re-create it any time with `clouds/<Cloud>/request-federation.sh`.
3. Copy every request to the organisation host (scp, a USB stick: they are public, but check that they arrive unchanged:
   compare `sha256sum` on both sides).
4. **Organisation host**: `org/sign-cloud.sh <request>...` checks each request (member of this SoLC, subject
   `<Cloud>.<Org>.arrowhead.eu`, proof of the CA key, relay subjects as the ACL expects), issues the name-constrained
   cross-certificate, and writes `org/out/<Cloud>-federation.tar.gz` for every signed member. Sign all members before
   handing out the bundles: each bundle holds the CRLs of the members signed so far (`org/sign-cloud.sh --bundles`
   rewrites them).
5. **Organisation host**: `org/relay.sh up` starts the relay on the design's relay port, on all interfaces. Only signed
   members are in its ACL and CRL list (re-run `org/relay.sh up` after signing more members).
6. **Each member host**: `clouds/<Cloud>/import-federation.sh <Cloud>-federation.tar.gz` verifies the bundle against the
   cloud's own CA, installs the relay chains, the organisation CRL and the peer CRLs, and restarts Gatekeeper, GateTunnel
   and the orchestrator with the relay enabled.
7. **Each member host**: `clouds/<Cloud>/intercloud.sh` applies the cloud's part of the plan (`federation-spec.json`):
   preconditions, Gatekeeper neighbours, inter-cloud ConsumerAuthorization grants (exactly the exchanges of the design;
   other exports are revoked), verification, and global service discovery from consumer clouds. Run it on provider clouds
   first, then on consumer clouds (discovery needs the provider's grants).
8. **Each member host**: `clouds/<Cloud>/check.sh`.

## Network

Members connect out to `relay.host:relay.port` of the design. The name must resolve on every member host *and inside its
containers*, and must be in the relay certificate (`org/init-org.sh` puts the design's relay host there; changing it
re-issues the relay certificate). Open only that TCP port on the organisation host, only for the member hosts.

## Changing the SoLC

Edit the design in the console's SoLC view, build a new package (a new revision) and apply it: `install.sh` of an existing
member keeps keys, passwords and data and reconfigures; `intercloud.sh` brings neighbours and grants to the new plan,
revoking what the new plan no longer contains. A member that leaves: `org/crl.sh revoke <Cloud>`, `org/sign-cloud.sh
--bundles` and import the new bundles on the remaining members, `org/relay.sh up`.
