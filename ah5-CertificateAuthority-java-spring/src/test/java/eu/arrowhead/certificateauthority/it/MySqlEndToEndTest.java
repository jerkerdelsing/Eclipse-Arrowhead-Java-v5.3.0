/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.certificateauthority.CertificateAuthorityMain;
import eu.arrowhead.certificateauthority.TestPki;
import eu.arrowhead.certificateauthority.service.CertificateSupport;
import eu.arrowhead.common.testhelper.StandaloneModeInTests;

// The whole application (filters, management filter, controllers, JPA) against a fresh MySQL schema created by
// deployment/database/init.sql and restricted by least-privilege.sh. Run with -Dca.it.db.url=... -Dca.it.db.password=...;
// skipped otherwise. Development profile without TLS and with declared identities ("Bearer SYSTEM//<name>").
@EnabledIfSystemProperty(named = "ca.it.db.url", matches = ".+")
// the deployment config/ folder of the working directory is not used: only the packaged development sample
@SpringBootTest(classes = { CertificateAuthorityMain.class, MySqlEndToEndTest.Standalone.class }, properties = "spring.config.location=classpath:/application.properties")
@AutoConfigureMockMvc
public class MySqlEndToEndTest {

	//=================================================================================================
	// members

	private static Path crlDirectory;
	private static TestPki pki;

	@Autowired
	private MockMvc mvc;

	private final ObjectMapper json = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) throws Exception {
		pki = new TestPki();
		final Path folder = Files.createTempDirectory("ca-it-");
		crlDirectory = Files.createDirectories(folder.resolve("crl"));
		Files.writeString(folder.resolve("ca.crt"), CertificateSupport.pem(pki.root));
		Files.writeString(folder.resolve("ca.key"), CertificateSupport.pem(pki.rootKeys.getPrivate()));

		registry.add("arrowhead.security.profile", () -> "development");
		registry.add("spring.datasource.url", () -> System.getProperty("ca.it.db.url"));
		registry.add("spring.datasource.password", () -> System.getProperty("ca.it.db.password"));
		registry.add("ca.certificate.file", () -> folder.resolve("ca.crt").toString());
		registry.add("ca.key.file", () -> folder.resolve("ca.key").toString());
		registry.add("crl.dir", () -> crlDirectory.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void signListRevokeAndPublishAgainstMySql() throws Exception {
		// Onboarding signs a system certificate
		final MvcResult signed = mvc.perform(post("/certificate-authority/sign").header("Authorization", "Bearer SYSTEM//Onboarding")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("encodedCSR", TestPki.csr("Pump." + TestPki.SUFFIX, null), "addresses", java.util.List.of("pump")))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.profile").value("SYSTEM"))
				.andReturn();
		final Map<?, ?> certificate = json.readValue(signed.getResponse().getContentAsString(), Map.class);
		final String issued = (String) ((java.util.List<?>) certificate.get("certificateChain")).get(0);

		// a second identity cannot take over the valid name
		mvc.perform(post("/sign").header("Authorization", "Bearer SYSTEM//CertificateAuthority")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("encodedCSR", TestPki.csr("Pump." + TestPki.SUFFIX, null)))))
				.andExpect(status().isForbidden());

		mvc.perform(post("/certificate-authority/checkCertificate").header("Authorization", "Bearer SYSTEM//Onboarding")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("certificate", issued))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("GOOD"))
				.andExpect(jsonPath("$.known").value(true));

		// management: Sysop lists, LocalCloudManager may not
		mvc.perform(get("/certificate-authority/mgmt/certificates").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].common_name").value("Pump." + TestPki.SUFFIX))
				.andExpect(jsonPath("$[0].created_by").value("Onboarding"));
		mvc.perform(get("/certificate-authority/mgmt/certificates").header("Authorization", "Bearer SYSTEM//LocalCloudManager"))
				.andExpect(status().isForbidden());

		// Onboarding revokes its own certificate; the CRL file lists it
		final Number id = (Number) certificate.get("id");
		mvc.perform(delete("/certificate-authority/mgmt/certificates/" + id).header("Authorization", "Bearer SYSTEM//Onboarding"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
		mvc.perform(post("/checkCertificate").header("Authorization", "Bearer SYSTEM//Onboarding")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("certificate", issued))))
				.andExpect(jsonPath("$.status").value("REVOKED"));
		final var crl = (java.security.cert.X509CRL) java.security.cert.CertificateFactory.getInstance("X.509")
				.generateCRL(Files.newInputStream(crlDirectory.resolve("ca.crl")));
		assertTrue(crl.isRevoked(CertificateSupport.certificate(issued)));

		// trusted keys: add, check, delete
		final String key = CertificateSupport.pem(SubjectPublicKeyInfo.getInstance(TestPki.key().getPublic().getEncoded()));
		final MvcResult added = mvc.perform(put("/certificate-authority/mgmt/keys").header("Authorization", "Bearer SYSTEM//Sysop")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("publicKey", key, "description", "it"))))
				.andExpect(status().isCreated())
				.andReturn();
		final Number keyId = (Number) json.readValue(added.getResponse().getContentAsString(), Map.class).get("id");
		mvc.perform(put("/mgmt/keys").header("Authorization", "Bearer SYSTEM//Sysop")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("publicKey", key))))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/certificate-authority/checkTrustedKey").header("Authorization", "Bearer SYSTEM//Onboarding")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("publicKey", key))))
				.andExpect(jsonPath("$.trusted").value(true))
				.andExpect(jsonPath("$.key.description").value("it"));
		mvc.perform(delete("/certificate-authority/mgmt/keys/" + keyId).header("Authorization", "Bearer SYSTEM//Sysop")).andExpect(status().isOk());
		mvc.perform(delete("/certificate-authority/mgmt/keys/" + keyId).header("Authorization", "Bearer SYSTEM//Sysop")).andExpect(status().isNotFound());

		// generalManagement status, get-log, get-config
		mvc.perform(get("/certificate-authority/mgmt/general/status").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.implementation").value("java-spring"))
				.andExpect(jsonPath("$.revokedCertificates").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
		mvc.perform(get("/certificate-authority/mgmt/general/status").header("Authorization", "Bearer SYSTEM//Onboarding"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/certificate-authority/mgmt/general/get-config").param("keys", "crl.dir").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.map['crl.dir']").value(crlDirectory.toString()));

		// the management filter refuses identities outside the whitelist before the CA rules are reached
		final MvcResult refused = mvc.perform(get("/mgmt/keys").header("Authorization", "Bearer SYSTEM//DataManager")).andReturn();
		assertEquals(403, refused.getResponse().getStatus());

		// no identity at all
		mvc.perform(get("/certificate-authority/crl")).andExpect(status().isUnauthorized());
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@TestConfiguration
	static class Standalone {

		//-------------------------------------------------------------------------------------------------
		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}
	}
}
