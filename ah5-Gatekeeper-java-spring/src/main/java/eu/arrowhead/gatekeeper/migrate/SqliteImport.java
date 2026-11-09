/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.migrate;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.parse;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.gatekeeper.service.dto.CloudInput;
import eu.arrowhead.gatekeeper.service.dto.RelayInput;
import eu.arrowhead.gatekeeper.service.validation.GatekeeperValidation;

// Reads the clouds and relays of a native 5.2.2 gatekeeper.db (read-only) and checks every row as a management request would.
public final class SqliteImport {

	//=================================================================================================
	// members

	private final List<CloudInput> clouds = new ArrayList<>();
	private final List<RelayInput> relays = new ArrayList<>();
	private final List<ZonedDateTime> relayCreatedAt = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static SqliteImport read(final Path file) throws SQLException {
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("SQLite file not found: " + file);
		}

		final SqliteImport result = new SqliteImport();
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:file:" + file.toAbsolutePath() + "?mode=ro");
				Statement statement = connection.createStatement()) {
			try (ResultSet rs = statement.executeQuery("SELECT name, operator, gatekeeper_address, gatekeeper_port, metadata FROM clouds ORDER BY operator, name")) {
				while (rs.next()) {
					result.clouds.add(GatekeeperValidation.cloud(m("name", rs.getString("name"),
							"operator", rs.getString("operator"),
							"gatekeeperAddress", rs.getObject("gatekeeper_address"),
							"gatekeeperPort", rs.getObject("gatekeeper_port"),
							"metadata", parse(str(rs.getString("metadata"))))));
				}
			}

			try (ResultSet rs = statement.executeQuery("SELECT name, address, port, type, secure, metadata, created_at FROM relays ORDER BY name")) {
				while (rs.next()) {
					result.relays.add(GatekeeperValidation.relay(m("name", rs.getString("name"),
							"address", rs.getString("address"),
							"port", rs.getObject("port"),
							"type", rs.getString("type"),
							"secure", rs.getInt("secure") != 0,
							"metadata", parse(str(rs.getString("metadata"))))));
					final double createdAt = rs.getDouble("created_at");
					result.relayCreatedAt.add(rs.wasNull() ? null : ZonedDateTime.ofInstant(Instant.ofEpochMilli(Math.round(createdAt * 1000)), ZoneOffset.UTC));
				}
			}
		}

		return result;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public List<CloudInput> getClouds() {
		return clouds;
	}

	//-------------------------------------------------------------------------------------------------
	public List<RelayInput> getRelays() {
		return relays;
	}

	//-------------------------------------------------------------------------------------------------
	public List<ZonedDateTime> getRelayCreatedAt() {
		return relayCreatedAt;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SqliteImport() {
	}
}
