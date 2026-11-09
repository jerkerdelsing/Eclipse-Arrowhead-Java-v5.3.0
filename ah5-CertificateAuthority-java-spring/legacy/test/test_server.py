import os
import tempfile
import unittest
from datetime import timedelta

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

from src.server import Store, utcnow


class StoreTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.store = Store(os.path.join(self.temp.name, "ca.db"))

    def tearDown(self):
        self.temp.cleanup()

    def test_trusted_key_lifecycle(self):
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        pem = key.public_key().public_bytes(
            serialization.Encoding.PEM,
            serialization.PublicFormat.SubjectPublicKeyInfo,
        ).decode()
        key_id = self.store.add_key(pem, "integration", utcnow() - timedelta(minutes=1), utcnow() + timedelta(days=1))
        trusted, row = self.store.key_status(pem)
        self.assertTrue(trusted)
        self.assertEqual(key_id, row["id"])
        self.assertTrue(self.store.delete_key(key_id))
        self.assertEqual((False, None), self.store.key_status(pem))


if __name__ == "__main__":
    unittest.main()
