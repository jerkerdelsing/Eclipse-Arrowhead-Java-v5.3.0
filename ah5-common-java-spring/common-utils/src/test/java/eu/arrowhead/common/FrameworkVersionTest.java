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
package eu.arrowhead.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FrameworkVersionTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// systems register this constant as their version, so it must not drift from the released library version
	@Test
	public void testFrameworkVersionMatchesProjectVersion() throws Exception {
		final Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("../pom.xml"));
		final Element project = pom.getDocumentElement();
		String version = null;
		for (var node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
			if ("version".equals(node.getNodeName())) {
				version = node.getTextContent().trim();
			}
		}

		assertEquals(version, Constants.AH_FRAMEWORK_VERSION);
	}
}
