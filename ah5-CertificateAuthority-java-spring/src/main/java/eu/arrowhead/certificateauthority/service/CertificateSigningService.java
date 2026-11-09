/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.service;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.IPAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

// certificateSigning.sign: issues an end-entity certificate of the local cloud for a CSR.
@Service
public class CertificateSigningService {

	//=================================================================================================
	// members

	private static final String DNS_NAME = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*";
	private static final String SYSTEM_NAME = "[A-Z][A-Za-z0-9]{0,62}";

	private final IssuerCredentials issuer;
	private final IssuedCertificateRepository certificates;
	private final Set<String> reserved;
	private final int certificateValidityDays;
	private final int onboardingValidityHours;
	private final SecureRandom random = new SecureRandom();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public CertificateSigningService(
			final IssuerCredentials issuer,
			final IssuedCertificateRepository certificates,
			@Value(CertificateAuthorityConstants.$RESERVED_NAMES_WD) final String reservedNames,
			@Value(CertificateAuthorityConstants.$CERT_VALIDITY_DAYS_WD) final int certificateValidityDays,
			@Value(CertificateAuthorityConstants.$ONBOARDING_CERT_VALIDITY_HOURS_WD) final int onboardingValidityHours) {
		this.issuer = issuer;
		this.certificates = certificates;
		this.reserved = csv(reservedNames);
		this.certificateValidityDays = certificateValidityDays;
		this.onboardingValidityHours = onboardingValidityHours;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean maySign(final String caller) {
		return CertificateAuthorityConstants.PROFILES.containsKey(caller);
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public Map<String, Object> sign(final Map<String, Object> payload, final String caller, final String origin) {
		if (!maySign(caller)) {
			throw new ForbiddenException("Signing requires a provisioning identity", origin);
		}

		try {
			return issue(payload, caller, origin);
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> issue(final Map<String, Object> payload, final String caller, final String origin) {
		final String encoded = CertificateSupport.text(payload.getOrDefault("encodedCSR", payload.get("csr")));
		if (encoded.isBlank()) {
			throw new IllegalArgumentException("encodedCSR is required");
		}

		final PKCS10CertificationRequest request = CertificateSupport.csr(encoded);
		if (!signatureValid(request)) {
			throw new IllegalArgumentException("CSR signature invalid");
		}

		final RDN[] names = request.getSubject().getRDNs(BCStyle.CN);
		if (names.length != 1 || names[0].size() != 1) {
			throw new IllegalArgumentException("CSR requires exactly one common name");
		}

		final String common = IETFUtils.valueToString(names[0].getFirst().getValue());
		final String suffix = issuer.cloudSuffix();
		final String shortName = common.contains(".") ? common.substring(0, common.indexOf('.')) : "";
		if (!common.equals(shortName + "." + suffix) || !shortName.matches(SYSTEM_NAME)) {
			throw new IllegalArgumentException("CSR common name must belong to the local cloud");
		}

		if (reserved.contains(shortName) && !CertificateAuthorityConstants.SYSOP.equals(caller)) {
			throw new ForbiddenException("Reserved system/operator name", origin);
		}

		if (heldByAnotherIssuer(common, caller)) {
			throw new ForbiddenException("Name has a valid certificate held by another issuer", origin);
		}

		final String profile = profile(payload, request);
		if (!CertificateAuthorityConstants.PROFILE_NAMES.containsKey(profile)) {
			throw new IllegalArgumentException("Invalid profile");
		}

		if (!CertificateAuthorityConstants.PROFILES.getOrDefault(caller, Set.of()).contains(profile)) {
			throw new ForbiddenException("Profile not permitted for caller", origin);
		}

		final X509Certificate root = issuer.root();
		final Instant now = Instant.now();
		final Duration limit = "on".equals(profile) ? Duration.ofHours(onboardingValidityHours) : Duration.ofDays(certificateValidityDays);
		final Instant start = CertificateSupport.instant(payload.get("validAfter"), now.minusSeconds(60));
		Instant end = CertificateSupport.instant(payload.get("validBefore"), start.plus(limit));
		if (end.isAfter(root.getNotAfter().toInstant())) {
			end = root.getNotAfter().toInstant();
		}

		if (!end.isAfter(start) || end.isAfter(start.plus(limit))) {
			throw new IllegalArgumentException("Invalid certificate validity");
		}

		final X509Certificate certificate = build(request, common, profile, start, end, alternativeNames(payload.get("addresses"), request));
		final IssuedCertificate saved = certificates.saveAndFlush(new IssuedCertificate(
				certificate.getSerialNumber().toString(),
				common,
				caller,
				certificate.getNotBefore().toInstant(),
				certificate.getNotAfter().toInstant(),
				null,
				CertificateSupport.pem(certificate)));

		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", saved.getId());
		result.put("certificateChain", List.of(der(certificate), der(root)));
		result.put("commonName", common);
		result.put("profile", CertificateAuthorityConstants.PROFILE_NAMES.get(profile));
		result.put("serialNumber", certificate.getSerialNumber().toString());
		result.put("validAfter", start.toString());
		result.put("validBefore", end.toString());

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean signatureValid(final PKCS10CertificationRequest request) {
		try {
			return request.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(CertificateSupport.PROVIDER).build(request.getSubjectPublicKeyInfo()));
		} catch (final Exception ex) {
			throw new IllegalArgumentException("CSR signature invalid");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// a name that has a valid, unrevoked certificate requested by someone else cannot be taken over
	private boolean heldByAnotherIssuer(final String common, final String caller) {
		final Instant now = Instant.now();
		return certificates.findByCommonNameAndRevokedAtIsNullAndCreatedByNot(common, caller)
				.stream()
				.anyMatch(c -> c.getValidUntil().isAfter(now));
	}

	//-------------------------------------------------------------------------------------------------
	// explicit "profile" field, else the DN qualifier of the CSR subject, else "sy"
	private static String profile(final Map<String, Object> payload, final PKCS10CertificationRequest request) {
		final String profile = CertificateSupport.text(payload.get("profile"));
		if (!profile.isEmpty()) {
			return profile;
		}

		final RDN[] qualifiers = request.getSubject().getRDNs(BCStyle.DN_QUALIFIER);
		return qualifiers.length == 0 ? "sy" : IETFUtils.valueToString(qualifiers[0].getFirst().getValue());
	}

	//-------------------------------------------------------------------------------------------------
	private X509Certificate build(final PKCS10CertificationRequest request, final String common, final String profile, final Instant start, final Instant end,
			final List<GeneralName> alternatives) {
		final X509Certificate root = issuer.root();
		try {
			final X500NameBuilder subject = new X500NameBuilder(BCStyle.INSTANCE);
			final X500Name rootName = new X500Name(root.getSubjectX500Principal().getName());
			for (final var attribute : List.of(BCStyle.C, BCStyle.O)) {
				for (final RDN rdn : rootName.getRDNs(attribute)) {
					subject.addRDN(attribute, IETFUtils.valueToString(rdn.getFirst().getValue()));
				}
			}

			subject.addRDN(BCStyle.OU, "Arrowhead Systems");
			subject.addRDN(BCStyle.CN, common);
			subject.addRDN(BCStyle.DN_QUALIFIER, profile);

			final PublicKey publicKey = new JcaPKCS10CertificationRequest(request).setProvider(CertificateSupport.PROVIDER).getPublicKey();
			final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
					X500Name.getInstance(root.getSubjectX500Principal().getEncoded()),
					new BigInteger(159, random).setBit(158),
					Date.from(start),
					Date.from(end),
					subject.build(),
					publicKey);
			final JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();

			// end-entity only: ca=false is always enforced
			builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
			builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
			builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] { KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth }));
			builder.addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(publicKey));
			builder.addExtension(Extension.authorityKeyIdentifier, false, utils.createAuthorityKeyIdentifier(root.getPublicKey()));
			if (!alternatives.isEmpty()) {
				builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(alternatives.toArray(GeneralName[]::new)));
			}

			return new JcaX509CertificateConverter().setProvider(CertificateSupport.PROVIDER)
					.getCertificate(builder.build(new JcaContentSignerBuilder(issuer.signatureAlgorithm()).setProvider(CertificateSupport.PROVIDER).build(issuer.rootKey())));
		} catch (final IllegalArgumentException ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new InternalServerError("Certificate issuing failed");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// explicit "addresses" (at most 16 IP addresses or DNS names), else the subjectAlternativeName request of the CSR
	static List<GeneralName> alternativeNames(final Object input, final PKCS10CertificationRequest request) {
		final List<Object> addresses = new ArrayList<>();
		if (input == null) {
			addresses.addAll(requestedNames(request));
		} else {
			if (!(input instanceof List<?> list)) {
				throw new IllegalArgumentException("addresses must be a list");
			}

			addresses.addAll(list);
		}

		if (addresses.size() > CertificateAuthorityConstants.MAX_ADDRESSES) {
			throw new IllegalArgumentException("Too many addresses");
		}

		final List<GeneralName> names = new ArrayList<>();
		for (final Object raw : addresses) {
			final String address = CertificateSupport.text(raw);
			if (IPAddress.isValid(address)) {
				names.add(new GeneralName(GeneralName.iPAddress, address));
			} else {
				if (address.length() > 253 || address.isBlank() || !address.matches(DNS_NAME)) {
					throw new IllegalArgumentException("Invalid DNS address");
				}

				names.add(new GeneralName(GeneralName.dNSName, address));
			}
		}

		return names;
	}

	//-------------------------------------------------------------------------------------------------
	private static List<String> requestedNames(final PKCS10CertificationRequest request) {
		final List<String> result = new ArrayList<>();
		final Extensions extensions = request.getRequestedExtensions();
		if (extensions == null) {
			return result;
		}

		final Extension san = extensions.getExtension(Extension.subjectAlternativeName);
		if (san == null) {
			return result;
		}

		try {
			for (final GeneralName name : GeneralNames.getInstance(san.getParsedValue()).getNames()) {
				if (name.getTagNo() == GeneralName.dNSName) {
					result.add(ASN1IA5String.getInstance(name.getName()).getString());
				} else if (name.getTagNo() == GeneralName.iPAddress) {
					result.add(InetAddress.getByAddress(ASN1OctetString.getInstance(name.getName()).getOctets()).getHostAddress());
				}
			}
		} catch (final Exception ex) {
			throw new IllegalArgumentException("Invalid subjectAlternativeName request");
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private static String der(final X509Certificate certificate) {
		try {
			return Base64.getEncoder().encodeToString(certificate.getEncoded());
		} catch (final Exception ex) {
			throw new InternalServerError("Certificate encoding failed");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static Set<String> csv(final String value) {
		final Set<String> result = new TreeSet<>();
		for (final String part : value.split(",")) {
			if (!part.isBlank()) {
				result.add(part.trim());
			}
		}

		return result;
	}
}
