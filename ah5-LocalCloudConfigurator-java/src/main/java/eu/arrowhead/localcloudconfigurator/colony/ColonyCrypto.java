package eu.arrowhead.localcloudconfigurator.colony;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

/**
 * The ColonyOS identity scheme (colonies v1.9.10, internal/crypto): secp256k1 keys, SHA3-256 hashes, the identity is
 * the SHA3-256 hash of the hex-encoded uncompressed public key, and a signature is r || s || recovery id (65 bytes,
 * low s) over the SHA3-256 hash of the data.
 */
public final class ColonyCrypto {

	private static final X9ECParameters PARAMS = CustomNamedCurves.getByName("secp256k1");
	private static final ECDomainParameters CURVE = new ECDomainParameters(PARAMS.getCurve(), PARAMS.getG(), PARAMS.getN(), PARAMS.getH());
	private static final BigInteger HALF_N = PARAMS.getN().shiftRight(1);
	private static final HexFormat HEX = HexFormat.of();
	private static final SecureRandom RANDOM = new SecureRandom();

	private ColonyCrypto() {
	}

	public static String generatePrivateKey() {
		BigInteger d;
		do {
			d = new BigInteger(256, RANDOM);
		} while (d.signum() <= 0 || d.compareTo(CURVE.getN()) >= 0);
		return HEX.formatHex(fixed(d));
	}

	public static String id(final String prvKeyHex) {
		return sha3Hex(HEX.formatHex(publicKey(privateKey(prvKeyHex))));
	}

	public static String sha3Hex(final String data) {
		return HEX.formatHex(sha3(data.getBytes(StandardCharsets.UTF_8)));
	}

	/** Signature of data (the base64 payload of an RPC message) as ColonyOS expects it, hex-encoded. */
	public static String sign(final String data, final String prvKeyHex) {
		final BigInteger d = privateKey(prvKeyHex);
		final byte[] hash = sha3(data.getBytes(StandardCharsets.UTF_8));
		final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
		signer.init(true, new ECPrivateKeyParameters(d, CURVE));
		final BigInteger[] rs = signer.generateSignature(hash);
		final BigInteger r = rs[0];
		final BigInteger s = rs[1].compareTo(HALF_N) > 0 ? CURVE.getN().subtract(rs[1]) : rs[1];
		final byte[] pub = publicKey(d);
		for (int recId = 0; recId < 4; recId++) {
			final ECPoint q = recover(recId, r, s, hash);
			if (q != null && java.util.Arrays.equals(q.getEncoded(false), pub)) {
				final byte[] sig = new byte[65];
				System.arraycopy(fixed(r), 0, sig, 0, 32);
				System.arraycopy(fixed(s), 0, sig, 32, 32);
				sig[64] = (byte) recId;
				return HEX.formatHex(sig);
			}
		}
		throw new IllegalStateException("no recovery id matches the signing key");
	}

	/** Identity that produced a signature, or null; used by the tests to check signatures the way the server does. */
	public static String recoverId(final String data, final String signatureHex) {
		final byte[] sig = HEX.parseHex(signatureHex);
		if (sig.length != 65) {
			return null;
		}
		final BigInteger r = new BigInteger(1, java.util.Arrays.copyOfRange(sig, 0, 32));
		final BigInteger s = new BigInteger(1, java.util.Arrays.copyOfRange(sig, 32, 64));
		if (s.compareTo(HALF_N) > 0) {
			return null;
		}
		final ECPoint q = recover(sig[64], r, s, sha3(data.getBytes(StandardCharsets.UTF_8)));
		return q == null ? null : sha3Hex(HEX.formatHex(q.getEncoded(false)));
	}

	private static byte[] sha3(final byte[] data) {
		final SHA3Digest d = new SHA3Digest(256);
		d.update(data, 0, data.length);
		final byte[] out = new byte[32];
		d.doFinal(out, 0);
		return out;
	}

	private static BigInteger privateKey(final String hex) {
		final byte[] b = HEX.parseHex(hex.trim());
		if (b.length != 32) {
			throw new IllegalArgumentException("invalid private key length, should be 256 bits");
		}
		final BigInteger d = new BigInteger(1, b);
		if (d.signum() <= 0 || d.compareTo(CURVE.getN()) >= 0) {
			throw new IllegalArgumentException("invalid private key");
		}
		return d;
	}

	private static byte[] publicKey(final BigInteger d) {
		return new FixedPointCombMultiplier().multiply(CURVE.getG(), d).normalize().getEncoded(false);
	}

	private static byte[] fixed(final BigInteger v) {
		final byte[] b = v.toByteArray();
		final byte[] out = new byte[32];
		final int n = Math.min(b.length, 32);
		System.arraycopy(b, b.length - n, out, 32 - n, n);
		return out;
	}

	/** SEC 1 v2, section 4.1.6: public key recovery from (r, s) and the recovery id. */
	private static ECPoint recover(final int recId, final BigInteger r, final BigInteger s, final byte[] hash) {
		final BigInteger n = CURVE.getN();
		final BigInteger x = r.add(BigInteger.valueOf(recId / 2).multiply(n));
		if (x.compareTo(CURVE.getCurve().getField().getCharacteristic()) >= 0) {
			return null;
		}
		final X9IntegerConverter conv = new X9IntegerConverter();
		final byte[] enc = conv.integerToBytes(x, 1 + conv.getByteLength(CURVE.getCurve()));
		enc[0] = (byte) ((recId & 1) == 1 ? 0x03 : 0x02);
		final ECPoint rPoint;
		try {
			rPoint = CURVE.getCurve().decodePoint(enc);
		} catch (final IllegalArgumentException notOnCurve) {
			return null;
		}
		if (!rPoint.multiply(n).isInfinity()) {
			return null;
		}
		final BigInteger e = new BigInteger(1, hash);
		final BigInteger rInv = r.modInverse(n);
		final BigInteger eInvRInv = rInv.multiply(BigInteger.ZERO.subtract(e).mod(n)).mod(n);
		final BigInteger sRInv = rInv.multiply(s).mod(n);
		return ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvRInv, rPoint, sRInv).normalize();
	}
}
