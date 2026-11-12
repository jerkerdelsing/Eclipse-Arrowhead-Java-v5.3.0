/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

// The operator UI and the console session. ConsoleFilter serves these paths on the console connector only.
@Hidden
@RestController
public class ConsoleController {

	//=================================================================================================
	// members

	@Autowired
	private ConsoleAuth auth;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = "/index.html")
	public ResponseEntity<byte[]> index() throws IOException {
		return resource("index.html", MediaType.parseMediaType("text/html; charset=utf-8"));
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = "/app.js")
	public ResponseEntity<byte[]> script() throws IOException {
		return resource("app.js", MediaType.parseMediaType("text/javascript; charset=utf-8"));
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = "/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
	public String health() {
		return "ok\n";
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SoSConfiguratorConstants.HTTP_API_SESSION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> login(final HttpServletRequest request, @RequestBody(required = false) final Map<String, Object> body) {
		final Object password = body == null ? null : body.get("password");
		final ConsoleAuth.Login login = auth.login(request.getRemoteAddr(), password == null ? null : password.toString());
		return switch (login.result()) {
		case OK -> ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie(login.session(), auth.sessionSeconds()).toString())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Json.write(Map.of("session", "open")));
		case WRONG -> error(HttpStatus.UNAUTHORIZED, "wrong password");
		case LOCKED -> error(HttpStatus.TOO_MANY_REQUESTS, "login locked, try again later");
		case DISABLED -> error(HttpStatus.SERVICE_UNAVAILABLE, "the console has no password configured");
		};
	}

	//-------------------------------------------------------------------------------------------------
	@DeleteMapping(path = SoSConfiguratorConstants.HTTP_API_SESSION_PATH)
	public ResponseEntity<Void> logout(final HttpServletRequest request) {
		auth.logout(ConsoleFilter.session(request));
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static ResponseCookie cookie(final String value, final long maxAgeSeconds) {
		return ResponseCookie.from(SoSConfiguratorConstants.CONSOLE_COOKIE, value)
				.httpOnly(true)
				.sameSite("Strict")
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private static ResponseEntity<byte[]> resource(final String name, final MediaType type) throws IOException {
		try (InputStream in = ConsoleController.class.getResourceAsStream("/web/" + name)) {
			if (in == null) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(type).body(in.readAllBytes());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static ResponseEntity<String> error(final HttpStatus status, final String message) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(Json.write(Map.of("error", message)));
	}
}
