import base64
import os
import tempfile
import unittest
from datetime import timedelta

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

import src.server as server


def csr(common_name, qualifier=None, extensions=()):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    attributes = [x509.NameAttribute(NameOID.COMMON_NAME, common_name)]
    if qualifier:
        attributes.append(x509.NameAttribute(NameOID.DN_QUALIFIER, qualifier))
    builder = x509.CertificateSigningRequestBuilder().subject_name(x509.Name(attributes))
    for extension, critical in extensions:
        builder = builder.add_extension(extension, critical)
    request = builder.sign(key, hashes.SHA256())
    return base64.b64encode(request.public_bytes(serialization.Encoding.DER)).decode()


class SigningPolicyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        name = x509.Name([x509.NameAttribute(NameOID.COUNTRY_NAME, "SE"), x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Company"),
                          x509.NameAttribute(NameOID.COMMON_NAME, "TestCloud.Company.arrowhead.eu")])
        now = server.utcnow()
        ca = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
              .serial_number(1).not_valid_before(now - timedelta(days=1)).not_valid_after(now + timedelta(days=3650))
              .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True).sign(key, hashes.SHA256()))
        cert_path, key_path = os.path.join(self.temp.name, "ca.crt"), os.path.join(self.temp.name, "ca.key")
        open(cert_path, "wb").write(ca.public_bytes(serialization.Encoding.PEM))
        open(key_path, "wb").write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8,
                                                     serialization.NoEncryption()))
        self.saved = server.CA_CERT_PATH, server.CA_KEY_PATH
        server.CA_CERT_PATH, server.CA_KEY_PATH = cert_path, key_path
        self.store = server.Store(os.path.join(self.temp.name, "ca.db"))
        self.authority = server.CertificateAuthority(self.store)

    def tearDown(self):
        server.CA_CERT_PATH, server.CA_KEY_PATH = self.saved
        self.temp.cleanup()

    def issued(self, response):
        return x509.load_der_x509_certificate(base64.b64decode(response["certificateChain"][0]))

    def test_onboarding_gets_profiled_subject_and_fixed_extensions(self):
        request = csr("Sensor1.TestCloud.Company.arrowhead.eu", extensions=[
            (x509.BasicConstraints(ca=True, path_length=None), True),
            (x509.SubjectAlternativeName([x509.DNSName("sensor1"), x509.UniformResourceIdentifier("urn:x")]), False)])
        response = self.authority.sign({"encodedCSR": request, "profile": "sy"}, "Onboarding")
        cert = self.issued(response)
        self.assertEqual("SYSTEM", response["profile"])
        self.assertEqual("sy", cert.subject.get_attributes_for_oid(NameOID.DN_QUALIFIER)[0].value)
        self.assertFalse(cert.extensions.get_extension_for_class(x509.BasicConstraints).value.ca)
        self.assertEqual([ExtendedKeyUsageOID.SERVER_AUTH, ExtendedKeyUsageOID.CLIENT_AUTH],
                         list(cert.extensions.get_extension_for_class(x509.ExtendedKeyUsage).value))
        names = cert.extensions.get_extension_for_class(x509.SubjectAlternativeName).value
        self.assertEqual(["sensor1"], names.get_values_for_type(x509.DNSName))
        self.assertEqual([], names.get_values_for_type(x509.UniformResourceIdentifier))

    def test_profile_is_limited_per_caller(self):
        request = csr("Mallory.TestCloud.Company.arrowhead.eu", qualifier="op")
        with self.assertRaises(PermissionError):
            self.authority.sign({"encodedCSR": request}, "Onboarding")
        with self.assertRaises(PermissionError):
            self.authority.sign({"encodedCSR": request, "profile": "de"}, "LocalCloudManager")
        self.assertEqual("OPERATOR", self.authority.sign({"encodedCSR": request}, "Sysop")["profile"])

    def test_name_must_belong_to_the_local_cloud(self):
        for name in ("Sensor1.OtherCloud.Company.arrowhead.eu", "sensor1.TestCloud.Company.arrowhead.eu",
                     "TestCloud.Company.arrowhead.eu"):
            with self.assertRaises(ValueError):
                self.authority.sign({"encodedCSR": csr(name), "profile": "sy"}, "Onboarding")

    def test_onboarding_certificates_are_short_lived(self):
        request = csr("Device1.TestCloud.Company.arrowhead.eu")
        cert = self.issued(self.authority.sign({"encodedCSR": request, "profile": "on"}, "Onboarding"))
        self.assertLessEqual(cert.not_valid_after_utc - cert.not_valid_before_utc,
                             timedelta(hours=server.ONBOARDING_VALIDITY_HOURS, minutes=1))
        with self.assertRaises(ValueError):
            self.authority.sign({"encodedCSR": request, "profile": "on",
                                 "validBefore": server.iso(server.utcnow() + timedelta(days=30))}, "Onboarding")

    def test_onboarding_revokes_only_its_own_certificates(self):
        own = self.authority.sign({"encodedCSR": csr("Device1.TestCloud.Company.arrowhead.eu"), "profile": "de"}, "Onboarding")
        other = self.authority.sign({"encodedCSR": csr("Other1.TestCloud.Company.arrowhead.eu"), "profile": "sy"}, "Sysop")
        self.assertFalse(self.store.revoke(other["id"], created_by="Onboarding"))
        self.assertTrue(self.store.revoke(own["id"], created_by="Onboarding"))
        status = self.authority.check_certificate({"certificate": own["certificateChain"][0]})
        self.assertEqual("REVOKED", status["status"])


if __name__ == "__main__":
    unittest.main()
