/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

public final class SoLocalCloudConfiguratorConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "SoLocalCloudConfigurator";
	public static final String SYSTEM_VERSION = "5.3.0";
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.solocalcloudconfigurator.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.solocalcloudconfigurator.jpa.repository";

	// soLocalCloudConfiguration service
	public static final String SERVICE_DEF_SO_LOCAL_CLOUD_CONFIGURATION = "soLocalCloudConfiguration";
	public static final String SERVICE_VERSION = "1.0.0";
	public static final String SERVICE_OP_CATALOG = "catalog";
	public static final String SERVICE_OP_LIST_LC_DESIGNS = "list-lc-designs";
	public static final String SERVICE_OP_LIST_DESIGNS = "list-designs";
	public static final String SERVICE_OP_GET_DESIGN = "get-design";
	public static final String SERVICE_OP_CREATE_DESIGN = "create-design";
	public static final String SERVICE_OP_UPDATE_DESIGN = "update-design";
	public static final String SERVICE_OP_REMOVE_DESIGN = "remove-design";
	public static final String SERVICE_OP_PLAN = "plan";
	public static final String SERVICE_OP_BUILD_PACKAGE = "build-package";
	public static final String SERVICE_OP_LIST_PACKAGES = "list-packages";
	public static final String SERVICE_OP_GET_PACKAGE = "get-package";
	public static final String SERVICE_OP_REMOVE_PACKAGE = "remove-package";

	public static final String HTTP_API_BASE_PATH = "/api";
	public static final String HTTP_API_OP_CATALOG_PATH = "/catalog";
	public static final String HTTP_API_OP_LC_DESIGNS_PATH = "/lc-designs";
	public static final String HTTP_API_OP_DESIGNS_PATH = "/designs";
	public static final String HTTP_API_OP_DESIGN_PATH = "/designs/{id}";
	public static final String HTTP_API_OP_PLAN_PATH = "/plan";
	public static final String HTTP_API_OP_PACKAGES_PATH = "/packages";
	public static final String HTTP_API_OP_PACKAGE_PATH = "/packages/{id}";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = "/solocalcloudconfigurator/general/mgmt";
	public static final String ID_PATTERN = "[a-f0-9-]{36}";

	// LocalCloudConfigurator, whose stored local-cloud designs a System of LocalClouds is built from
	public static final String LCC_SYSTEM_NAME = "LocalCloudConfigurator";
	public static final String SERVICE_DEF_LOCAL_CLOUD_CONFIGURATION = "localCloudConfiguration";
	public static final String LCC_OP_CATALOG = "catalog";
	public static final String LCC_OP_LIST_CONFIGURATIONS = "list-configurations";
	public static final String LCC_OP_GET_CONFIGURATION = "get-configuration";

	// configuration
	public static final String CATALOG_FILE = "solc.catalog.file";
	public static final String $CATALOG_FILE_WD = "${" + CATALOG_FILE + ":" + SoLocalCloudConfiguratorDefaults.CATALOG_FILE_DEFAULT + "}";
	public static final String API_WHITELIST = "solc.api.whitelist";
	public static final String $API_WHITELIST_WD = "${" + API_WHITELIST + ":" + SoLocalCloudConfiguratorDefaults.API_WHITELIST_DEFAULT + "}";
	public static final String SOURCE_REPOSITORY = "solc.source.repository";
	public static final String $SOURCE_REPOSITORY_WD = "${" + SOURCE_REPOSITORY + ":" + SoLocalCloudConfiguratorDefaults.SOURCE_REPOSITORY_DEFAULT + "}";
	public static final String SOURCE_COMMIT = "solc.source.commit";
	public static final String $SOURCE_COMMIT_WD = "${" + SOURCE_COMMIT + ":}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + SoLocalCloudConfiguratorDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";

	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;
	public static final int MAX_BODY_BYTES = 256 << 10;
	// a package without source or image archives; the host assembler adds those outside this system
	public static final int MAX_PACKAGE_BYTES = 32 << 20;
	public static final int MAX_DESIGNS = 500;
	public static final int MAX_PACKAGES = 200;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SoLocalCloudConfiguratorConstants() {
		throw new UnsupportedOperationException();
	}
}
