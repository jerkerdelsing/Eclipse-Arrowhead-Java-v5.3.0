"""Revocation fixtures for AH-GT-1 peer verification (security finding SOS-4, 2026-11-10). Writes a throw-away PKI
(organisation CA -> cross-certified cloud CA "PeerCloud" -> GateTunnel relay certificate) and CRLs into the directory
given as argument; the Java and Go tests check which CRL sets accept or refuse the relay chain.
Usage: python3 make_revocation.py <output dir>. Test keys only - never used by a cloud."""
import datetime, sys
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
utc = datetime.timezone.utc
start = datetime.datetime(2026, 1, 1, tzinfo=utc); end = datetime.datetime(2036, 1, 1, tzinfo=utc)
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
def crl(issuer, signer, revoked=(), this=start, next=end):
    b = x509.CertificateRevocationListBuilder().issuer_name(issuer).last_update(this).next_update(next)
    for serial in revoked:
        b = b.add_revoked_certificate(x509.RevokedCertificateBuilder().serial_number(serial).revocation_date(this).build())
    return b.sign(signer, hashes.SHA256())
pem = lambda c: c.public_bytes(serialization.Encoding.PEM)

org_key = key(); org_name = name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.COMMON_NAME, "Company.arrowhead.eu"))
org = cert(org_name, org_name, org_key.public_key(), org_key, True)
ca_key = key(); ca_name = name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, "Arrowhead Test Cloud"), (NameOID.COMMON_NAME, "PeerCloud.Company.arrowhead.eu"))
cross = cert(ca_name, org_name, ca_key.public_key(), org_key, True,
             constraints=name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, "PeerCloud")))
relay_key = key()
leaf = cert(name((NameOID.COUNTRY_NAME, "SE"), (NameOID.ORGANIZATION_NAME, "Company"), (NameOID.ORGANIZATIONAL_UNIT_NAME, "PeerCloud"),
                 (NameOID.COMMON_NAME, "GateTunnel.PeerCloud.Company.arrowhead.eu"), (NameOID.DN_QUALIFIER, "sy")), ca_name, relay_key.public_key(), ca_key, False)
(out / "org-ca.crt").write_bytes(pem(org))
(out / "peer-relay-chain.crt").write_bytes(pem(leaf) + pem(cross))
(out / "peer-relay.key").write_bytes(relay_key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))

# the peer cloud CA's CRLs: current and empty; current and revoking the relay certificate (also listing an unrelated
# serial); expired; signed with another key under the same issuer name (must be ignored)
(out / "peer-ca.crl").write_bytes(pem(crl(ca_name, ca_key)))
(out / "peer-ca-revoked.crl").write_bytes(pem(crl(ca_name, ca_key, revoked=(12345, leaf.serial_number))))
(out / "peer-ca-expired.crl").write_bytes(pem(crl(ca_name, ca_key, this=start, next=start + datetime.timedelta(days=1))))
(out / "peer-ca-forged.crl").write_bytes(pem(crl(ca_name, key())))
# the organisation CA's CRLs, DER-encoded like ca.crl of the Java and Go clouds: empty; revoking the cross-certificate
(out / "org-ca.crl").write_bytes(crl(org_name, org_key).public_bytes(serialization.Encoding.DER))
(out / "org-ca-revoked.crl").write_bytes(crl(org_name, org_key, revoked=(cross.serial_number,)).public_bytes(serialization.Encoding.DER))
print("AH-GT-1 revocation fixtures written to", out)
