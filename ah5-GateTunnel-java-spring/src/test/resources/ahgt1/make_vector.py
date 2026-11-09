"""Test vector for AH-GT-1 (security finding J5), made with the reference implementation
Arrowhead-test-cloud-java/gatetunnel/src/session_security.py. Writes a throw-away test PKI and one key exchange plus
one sealed request and reply into the directory given as argument; the Java and Go tests check that they verify and
open them. Usage: python3 make_vector.py <reference src dir> <output dir>. Test keys only - never used by a cloud."""
import base64, datetime, json, sys
from pathlib import Path
sys.path.insert(0, sys.argv[1])
import session_security as ss
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, x25519
from cryptography.x509.oid import NameOID

out = Path(sys.argv[2]); out.mkdir(parents=True, exist_ok=True)
start = datetime.datetime(2026, 1, 1, tzinfo=datetime.timezone.utc); end = datetime.datetime(2036, 1, 1, tzinfo=datetime.timezone.utc)
def name(*rdns): return x509.Name([x509.NameAttribute(oid, value) for oid, value in rdns])
def key(): return rsa.generate_private_key(public_exponent=65537, key_size=2048)
def cert(subject, issuer, public, signer, ca, constraints=None):
    b = (x509.CertificateBuilder().subject_name(subject).issuer_name(issuer).public_key(public).serial_number(x509.random_serial_number())
         .not_valid_before(start).not_valid_after(end).add_extension(x509.BasicConstraints(ca=ca, path_length=None), critical=True))
    if constraints is not None:
        b = b.add_extension(x509.NameConstraints(permitted_subtrees=[x509.DirectoryName(constraints)], excluded_subtrees=None), critical=True)
    if ca:
        b = b.add_extension(x509.KeyUsage(False, False, False, False, False, True, True, False, False), critical=True)
    return b.sign(signer, hashes.SHA256())
pem = lambda c: c.public_bytes(serialization.Encoding.PEM).decode()
org_key = key(); org_name = name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.COMMON_NAME, "Company.arrowhead.eu"))
org = cert(org_name, org_name, org_key.public_key(), org_key, True)
(out / "org-ca.crt").write_text(pem(org))
for cloud in ("TestCloud", "GoCloud"):
    ca_key = key(); ca_name = name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, "Arrowhead Test Cloud"), (NameOID.COMMON_NAME, f"{cloud}.Company.arrowhead.eu"))
    cross = cert(ca_name, org_name, ca_key.public_key(), org_key, True,
                 constraints=name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, cloud)))
    relay_key = key()
    leaf = cert(name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, cloud),
                     (NameOID.COMMON_NAME, f"GateTunnel.{cloud}.Company.arrowhead.eu"), (NameOID.DN_QUALIFIER, "sy")), ca_name, relay_key.public_key(), ca_key, False)
    (out / f"{cloud}-relay-chain.crt").write_text(pem(leaf) + pem(cross))
    (out / f"{cloud}-relay.key").write_bytes(relay_key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))

# provider = GoCloud, consumer = TestCloud; the ephemeral private keys are written so the tests can derive the same keys
provider = ss.RelayIdentity(out / "GoCloud-relay.key", out / "GoCloud-relay-chain.crt", out / "org-ca.crt")
consumer = ss.RelayIdentity(out / "TestCloud-relay.key", out / "TestCloud-relay-chain.crt", out / "org-ca.crt")
session = {"sessionId": "vector-session-0001", "consumer": "RandomDataProducer", "consumerCloud": "TestCloud|Company", "provider": "DataManager",
           "serviceDefinition": "dataHistorian", "consumerGateTunnel": "GateTunnel.TestCloud.Company.arrowhead.eu", "providerGateTunnel": "GateTunnel.GoCloud.Company.arrowhead.eu"}
p_priv, session["providerKey"] = ss.new_ephemeral()
c_priv, session["consumerKey"] = ss.new_ephemeral()
raw = lambda k: base64.b64encode(k.private_bytes(serialization.Encoding.Raw, serialization.PrivateFormat.Raw, serialization.NoEncryption())).decode()
provider_exchange = {"providerKey": session["providerKey"], "signature": base64.b64encode(provider.sign(ss.transcript("provider", session))).decode(), "certificateChain": provider.chain_pem}
consumer_exchange = {"consumerKey": session["consumerKey"], "signature": base64.b64encode(consumer.sign(ss.transcript("consumer", session))).decode(), "certificateChain": consumer.chain_pem}
keys = ss.derive_keys(c_priv, session["providerKey"], session["sessionId"])
request = ss.seal(keys["c2p"], session["sessionId"], "c2p", 7, {"method": "POST", "path": "/datamanager/data-historian/put-data", "contentType": "application/json", "body": base64.b64encode(b'{"n":1}').decode()})
reply = ss.seal(keys["p2c"], session["sessionId"], "p2c", 7, {"status": 201, "contentType": "application/json", "body": base64.b64encode(b'{"ok":true}').decode()})
(out / "vector.json").write_text(json.dumps({"session": session, "providerExchange": provider_exchange, "consumerExchange": consumer_exchange,
    "providerEphemeralPrivate": raw(p_priv), "consumerEphemeralPrivate": raw(c_priv),
    "c2pKey": base64.b64encode(keys["c2p"]).decode(), "p2cKey": base64.b64encode(keys["p2c"]).decode(), "request": request, "reply": reply}, indent=1))
print("AH-GT-1 test vector written to", out)
