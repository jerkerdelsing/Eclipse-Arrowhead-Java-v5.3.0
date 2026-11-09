/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.api.http;

import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_BASE_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_CERTIFICATES_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_CERTIFICATE_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_CHECK_CERTIFICATE_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_CHECK_TRUSTED_KEY_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_CRL_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_HEALTH_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_KEYS_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_KEY_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_REVOCATIONS_PATH;
import static eu.arrowhead.certificateauthority.CertificateAuthorityConstants.HTTP_API_OP_SIGN_PATH;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.certificateauthority.security.Caller;
import eu.arrowhead.certificateauthority.service.CertificateAuthorityStatusService;
import eu.arrowhead.certificateauthority.service.CertificateManagementService;
import eu.arrowhead.certificateauthority.service.CertificateSigningService;
import eu.arrowhead.certificateauthority.service.CertificateValidationService;
import eu.arrowhead.certificateauthority.service.CrlService;
import eu.arrowhead.common.Constants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// certificateSigning, certificateValidation and certificateAuthorityManagement. Every path is served with and without the
// "/certificate-authority" base path, as the native 5.2.2 implementation did.
@RestController
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class CertificateAuthorityAPI {

	//=================================================================================================
	// members

	@Autowired
	private CertificateSigningService signingService;

	@Autowired
	private CertificateValidationService validationService;

	@Autowired
	private CertificateManagementService managementService;

	@Autowired
	private CrlService crlService;

	@Autowired
	private CertificateAuthorityStatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_SIGN_PATH, HTTP_API_OP_SIGN_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> sign(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> payload) {
		final String origin = origin(request);
		return json(HttpStatus.CREATED, signingService.sign(body(payload), Caller.of(request, origin), origin));
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_CHECK_CERTIFICATE_PATH, HTTP_API_OP_CHECK_CERTIFICATE_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> checkCertificate(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> payload) {
		return json(HttpStatus.OK, validationService.checkCertificate(body(payload), origin(request)));
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_CHECK_TRUSTED_KEY_PATH, HTTP_API_OP_CHECK_TRUSTED_KEY_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> checkTrustedKey(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> payload) {
		return json(HttpStatus.OK, validationService.checkTrustedKey(body(payload), origin(request)));
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_CRL_PATH, HTTP_API_OP_CRL_PATH })
	public ResponseEntity<byte[]> crl() {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.contentType(MediaType.parseMediaType("application/pkix-crl"))
				.body(crlService.crl());
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_CERTIFICATES_PATH, HTTP_API_OP_CERTIFICATES_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listCertificates(final HttpServletRequest request) {
		final String origin = origin(request);
		return json(HttpStatus.OK, managementService.listCertificates(Caller.of(request, origin), origin));
	}

	//-------------------------------------------------------------------------------------------------
	@DeleteMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_CERTIFICATE_PATH, HTTP_API_OP_CERTIFICATE_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> revokeCertificate(final HttpServletRequest request, @PathVariable final long id) {
		final String origin = origin(request);
		return success(managementService.revokeById(Caller.of(request, origin), id, origin));
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_REVOCATIONS_PATH, HTTP_API_OP_REVOCATIONS_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> revokeCertificateByContent(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> payload) {
		final String origin = origin(request);
		return json(HttpStatus.OK, managementService.revokeByContent(Caller.of(request, origin), body(payload), origin));
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_KEYS_PATH, HTTP_API_OP_KEYS_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listKeys(final HttpServletRequest request) {
		final String origin = origin(request);
		return json(HttpStatus.OK, managementService.listKeys(Caller.of(request, origin), origin));
	}

	//-------------------------------------------------------------------------------------------------
	@PutMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_KEYS_PATH, HTTP_API_OP_KEYS_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> addKey(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> payload) {
		final String origin = origin(request);
		final long id = managementService.addKey(Caller.of(request, origin), body(payload), origin);
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", id);
		result.put("trusted", true);

		return json(HttpStatus.CREATED, result);
	}

	//-------------------------------------------------------------------------------------------------
	@DeleteMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_KEY_PATH, HTTP_API_OP_KEY_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> deleteKey(final HttpServletRequest request, @PathVariable final long id) {
		final String origin = origin(request);
		return success(managementService.deleteKey(Caller.of(request, origin), id, origin));
	}

	//-------------------------------------------------------------------------------------------------
	// liveness on the mutual-TLS port; since 5.3 it needs an identity token like every other path
	@GetMapping(path = { HTTP_API_BASE_PATH + HTTP_API_OP_HEALTH_PATH, HTTP_API_OP_HEALTH_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> health() {
		return json(HttpStatus.OK, statusService.liveness());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	static String origin(final HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}

	//-------------------------------------------------------------------------------------------------
	static Map<String, Object> body(final Map<String, Object> payload) {
		return payload == null ? Map.of() : payload;
	}

	//-------------------------------------------------------------------------------------------------
	static <T> ResponseEntity<T> json(final HttpStatus status, final T body) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(body);
	}

	//-------------------------------------------------------------------------------------------------
	// 200 {"success":true} when something changed, otherwise 404 {"success":false}
	private static ResponseEntity<Map<String, Object>> success(final boolean changed) {
		return json(changed ? HttpStatus.OK : HttpStatus.NOT_FOUND, Map.of("success", changed));
	}
}
