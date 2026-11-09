/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.security;

import java.security.cert.X509Certificate;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;

// Subject attributes of a certificate, read as the native 5.2.2 implementation read them (DNQ may be DER-encoded).
public final class CertificateNames {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String attribute(final X509Certificate certificate, final String attribute) {
		try {
			for (final Rdn rdn : new LdapName(certificate.getSubjectX500Principal().getName()).getRdns()) {
				if (rdn.getType().equalsIgnoreCase(attribute) || "DNQ".equals(attribute) && "2.5.4.46".equals(rdn.getType())) {
					final Object value = rdn.getValue();
					if (value instanceof byte[] der) {
						return ((ASN1String) ASN1Primitive.fromByteArray(der)).getString();
					}

					return value == null ? "" : value.toString();
				}
			}
		} catch (final Exception ex) {
			// unreadable subject: no attribute
		}

		return "";
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private CertificateNames() {
		throw new UnsupportedOperationException();
	}
}
