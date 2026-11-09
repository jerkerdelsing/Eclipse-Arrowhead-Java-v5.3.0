/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.http.model.HttpInterfaceModel;

class PlantManagementRegistrationTest {
    @Test void registeredOperationsAreValidAndMatchTheControllerPaths() {
        var info = spy(new DynamicServiceOrchestrationSystemInfo());
        doReturn(mock(SSLProperties.class)).when(info).getSslProperties();
        doReturn("serviceorchestration-dynamic").when(info).getDomainAddress();
        doReturn(8441).when(info).getServerPort();
        HttpInterfaceModel model = ReflectionTestUtils.invokeMethod(info, "getHttpServiceInterfaceForPlantManagement");
        assertNotNull(model);
        assertEquals("/serviceorchestration/mgmt/plant-rules", model.basePath());
        assertEquals("POST", model.operations().get("query").method());
        assertEquals("/query", model.operations().get("query").path());
        assertEquals("PUT", model.operations().get("replace").method());
        assertEquals("/replace", model.operations().get("replace").path());
    }
}
