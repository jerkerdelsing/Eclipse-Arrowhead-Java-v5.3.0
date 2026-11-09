"""Reserved and unique certificate names (security finding M1)."""
import unittest

from test_signing import SigningPolicyTest, csr


class NamePolicyTest(SigningPolicyTest):
    def request(self, name, profile, caller):
        return self.authority.sign({"encodedCSR": csr(f"{name}.TestCloud.Company.arrowhead.eu"), "profile": profile}, caller)

    def test_reserved_names_only_for_sysop(self):
        for caller, profile in (("Onboarding", "sy"), ("Onboarding", "de"), ("LocalCloudManager", "sy")):
            for name in ("SysMonitor", "Gatekeeper", "ServiceRegistry", "Sysop"):
                with self.assertRaises(PermissionError, msg=f"{caller} {name}"):
                    self.request(name, profile, caller)
        self.assertEqual("SYSTEM", self.request("Gatekeeper", "sy", "Sysop")["profile"])

    def test_name_held_by_another_caller_is_refused(self):
        self.request("Sensor7", "sy", "LocalCloudManager")
        with self.assertRaises(PermissionError):
            self.request("Sensor7", "sy", "Onboarding")

    def test_same_caller_may_issue_further_profiles_for_its_name(self):
        self.request("Device9", "on", "Onboarding")
        self.assertEqual("DEVICE", self.request("Device9", "de", "Onboarding")["profile"])

    def test_revoked_holder_frees_the_name(self):
        issued = self.request("Sensor8", "sy", "LocalCloudManager")
        self.assertTrue(self.store.revoke(issued["id"]))
        self.assertEqual("SYSTEM", self.request("Sensor8", "sy", "Onboarding")["profile"])


del SigningPolicyTest

if __name__ == "__main__":
    unittest.main()
