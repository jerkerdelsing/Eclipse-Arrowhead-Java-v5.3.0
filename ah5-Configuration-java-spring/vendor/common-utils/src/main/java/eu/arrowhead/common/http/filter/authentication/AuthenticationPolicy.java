/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.common.http.filter.authentication;

public enum AuthenticationPolicy {
	DECLARED, CERTIFICATE, OUTSOURCED, INTERNAL;

	//=================================================================================================
	// members

	public static final String CERTIFICATE_VALUE = "CERTIFICATE"; // right side must be a constant expression
	public static final String OUTSOURCED_VALUE = "OUTSOURCED"; // right side must be a constant expression

	// it is important to keep this lower-case (because @ConditionalOnExpression in CommonBeanConfig class is case sensitive)
	public static final String INTERNAL_VALUE = "internal"; // right side must be a constant expression

}