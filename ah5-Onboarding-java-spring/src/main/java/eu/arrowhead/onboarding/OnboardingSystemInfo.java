/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class OnboardingSystemInfo extends SystemInfo {
    @Override public String getSystemName() { return OnboardingConstants.SYSTEM_NAME; }
    @Override public SystemModel getSystemModel() {
        return new SystemModel.Builder().address(getAddress()).version("5.3.0")
                .metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey()).build();
    }
    @Override public List<ServiceModel> getServices() {
        return List.of(
            new ServiceModel.Builder().serviceDefinition(OnboardingConstants.SERVICE_DEF_ONBOARDING).version("1.0.0")
                .serviceInterface(intf("/onboarding")
                    .operation("onboard-with-name", op("POST", "/certificate/name"))
                    .operation("onboard-with-csr", op("POST", "/certificate/csr"))
                    .operation("onboard-device", op("POST", "/device"))
                    .operation("onboard-system", op("POST", "/system"))
                    .operation("offboard-device", op("POST", "/device/offboard")).build()).build(),
            new ServiceModel.Builder().serviceDefinition(OnboardingConstants.SERVICE_DEF_ONBOARDING_MANAGEMENT).version("1.0.0")
                .serviceInterface(intf(OnboardingConstants.HTTP_API_MANAGEMENT_PATH)
                    .operation("query-enrollments", op("GET", "/enrollments"))
                    .operation("get-enrollment", op("GET", "/enrollments/{deviceName}"))
                    .operation("approve-device", op("PUT", "/enrollments/{deviceName}"))
                    .operation("offboard-device", op("POST", "/enrollments/{deviceName}/offboard"))
                    .operation("remove-enrollment", op("DELETE", "/enrollments/{deviceName}")).build()).build(),
            new ServiceModel.Builder().serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT).version("1.0.0")
                .serviceInterface(intf(OnboardingConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
                    .operation(Constants.SERVICE_OP_GET_LOG, op("POST", Constants.HTTP_API_OP_LOGS_PATH))
                    .operation(Constants.SERVICE_OP_GET_CONFIG, op("GET", Constants.HTTP_API_OP_GET_CONFIG_PATH)).build()).build());
    }
    private HttpInterfaceModel.Builder intf(String path) {
        return new HttpInterfaceModel.Builder(Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME, getDomainAddress(), getServerPort()).basePath(path);
    }
    private HttpOperationModel op(String method, String path) {
        return new HttpOperationModel.Builder().method(method).path(path).build();
    }
    @Override protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
        return new PublicConfigurationKeysAndDefaults(Set.of(Constants.SERVER_PORT, Constants.DOMAIN_NAME,
            Constants.AUTHENTICATION_POLICY, Constants.MANAGEMENT_POLICY), OnboardingDefaults.class);
    }
}
