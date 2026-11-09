import base64
import os
import tempfile
import unittest
from datetime import timedelta

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

import src.server as server
from test_signing import SigningPolicyTest, csr


class CrlTest(SigningPolicyTest):
    """Revocation register -> CRL (security finding H3)."""

    def setUp(self):
        super().setUp()
        self.saved_crl_dir = server.CRL_DIR
        server.CRL_DIR = self.temp.name

    def tearDown(self):
        server.CRL_DIR = self.saved_crl_dir
        super().tearDown()

    def published(self):
        return x509.load_pem_x509_crl(open(os.path.join(self.temp.name, "ca.crl"), "rb").read())

    def test_revoked_certificate_appears_in_signed_crl(self):
        response = self.authority.sign({"encodedCSR": csr("Sensor1.TestCloud.Company.arrowhead.eu"), "profile": "sy"},
                                       "Onboarding")
        self.authority.publish_crl()
        self.assertEqual(0, len(self.published()))
        self.assertTrue(self.store.revoke(response["id"]))
        self.authority.publish_crl()
        crl = self.published()
        self.assertTrue(crl.is_signature_valid(self.authority.ca_cert.public_key()))
        self.assertEqual(self.authority.ca_cert.subject, crl.issuer)
        self.assertIsNotNone(crl.get_revoked_certificate_by_serial_number(int(response["serialNumber"])))
        self.assertGreater(crl.next_update_utc, server.utcnow() + timedelta(hours=server.CRL_VALIDITY_HOURS - 1))
        self.assertFalse(os.path.exists(os.path.join(self.temp.name, "ca.crl.tmp")))

    def test_certificate_issued_outside_the_register_can_be_revoked_by_content(self):
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        now = server.utcnow()
        cert = (x509.CertificateBuilder()
                .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Gatekeeper.TestCloud.Company.arrowhead.eu")]))
                .issuer_name(self.authority.ca_cert.subject).public_key(key.public_key()).serial_number(4711)
                .not_valid_before(now - timedelta(days=1)).not_valid_after(now + timedelta(days=30))
                .sign(self.authority.ca_key, hashes.SHA256()))
        pem = cert.public_bytes(serialization.Encoding.PEM).decode()
        loaded = self.authority.own_certificate(pem)
        _, changed = self.store.record_revocation(loaded, "revoked-by:Sysop")
        self.assertTrue(changed)
        self.assertFalse(self.store.record_revocation(loaded, "revoked-by:Sysop")[1])
        self.assertEqual("REVOKED", self.authority.check_certificate({"certificate": pem})["status"])
        self.authority.publish_crl()
        self.assertIsNotNone(self.published().get_revoked_certificate_by_serial_number(4711))

    def test_foreign_certificate_is_refused(self):
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "GoCloud.Company.arrowhead.eu")])
        now = server.utcnow()
        foreign = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
                   .serial_number(7).not_valid_before(now).not_valid_after(now + timedelta(days=1)).sign(key, hashes.SHA256()))
        with self.assertRaises(ValueError):
            self.authority.own_certificate(base64.b64encode(foreign.public_bytes(serialization.Encoding.DER)).decode())


del SigningPolicyTest

if __name__ == "__main__":
    unittest.main()
