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
package eu.arrowhead.common.log4j2;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.EnvironmentProps;

public final class JDBCConnectionFactoryForLog4J2 {

	//=================================================================================================
	// members

	private static DataSource dataSource;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static Connection getConnection() throws SQLException {
		if (dataSource == null) {
			final HikariConfig config = new HikariConfig();
			config.setJdbcUrl(EnvironmentProps.getProperty(Constants.DATABASE_URL));
			config.setUsername(EnvironmentProps.getProperty(Constants.DATABASE_USER));
			config.setPassword(EnvironmentProps.getProperty(Constants.DATABASE_PASSWORD));
			config.setDriverClassName(EnvironmentProps.getProperty(Constants.DATABASE_DRIVER_CLASS));

			dataSource = new HikariDataSource(config);
		}

		try {
			return dataSource.getConnection();
		} catch (SQLException ex) {
			// this class' purpose to configure logging so in case of exceptions we can't use logging
			System.out.println(ex.getMessage());
			ex.printStackTrace();
			throw ex;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private JDBCConnectionFactoryForLog4J2() {
		throw new UnsupportedOperationException();
	}
}