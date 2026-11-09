package eu.arrowhead.localcloudconfigurator.colony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ColonyCryptoTest {

	// Test vectors of colonies v1.9.10 internal/crypto (hash_test.go, idendity_test.go)
	@Test
	void sha3MatchesColonyOS() {
		assertEquals("644bcc7e564373040999aac89e7622f3ca71fba1d972fd94a31c3bfbf24e3938", ColonyCrypto.sha3Hex("hello world"));
	}

	@Test
	void idMatchesColonyOS() {
		assertEquals("4fef2b5a82d134d058c1883c72d6d9caf77cd59ca82d73105017590dea3dcb87",
				ColonyCrypto.id("6d2fb6f546bacfd98c68769e61e0b44a697a30596c018a50e28200aa59b01c0a"));
	}

	@Test
	void signatureRecoversSignerWithLowS() {
		for (int i = 0; i < 25; i++) {
			final String prv = ColonyCrypto.generatePrivateKey();
			final String data = "eyJtc2d0eXBlIjoidGVzdCJ9" + i;
			final String sig = ColonyCrypto.sign(data, prv);
			assertEquals(130, sig.length());
			assertEquals(ColonyCrypto.id(prv), ColonyCrypto.recoverId(data, sig));
			assertNotEquals(ColonyCrypto.id(prv), ColonyCrypto.recoverId(data + "x", sig));
		}
	}

	@Test
	void rejectsBadKeys() {
		assertThrows(IllegalArgumentException.class, () -> ColonyCrypto.id("00"));
		assertThrows(IllegalArgumentException.class, () -> ColonyCrypto.id("0".repeat(64)));
		assertNull(ColonyCrypto.recoverId("x", "00"));
	}
}
