/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.api.http;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import eu.arrowhead.common.Constants;
import eu.arrowhead.onboarding.OnboardingConstants;
import eu.arrowhead.onboarding.api.dto.OnboardingDtos.*;
import eu.arrowhead.onboarding.service.EnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
/**
 * onboardingManagement: the operator approves devices and off-boards them. Identity token bound to the client certificate
 * (outsourced authentication) and management filtering (system operators by default) protect every operation.
 */
@RestController
@RequestMapping(OnboardingConstants.HTTP_API_MANAGEMENT_PATH)
public class OnboardingManagementAPI {
    private final EnrollmentService enrollments;
    public OnboardingManagementAPI(EnrollmentService enrollments) { this.enrollments = enrollments; }
    @Operation(summary="List approved devices, their state and the systems issued for them")
    @GetMapping(value="/enrollments", produces="application/json")
    public EnrollmentList query() { return enrollments.list(); }
    @Operation(summary="Get one device enrollment")
    @GetMapping(value="/enrollments/{deviceName}", produces="application/json")
    public EnrollmentView get(@PathVariable String deviceName) { return enrollments.get(deviceName); }
    @Operation(summary="Approve a device: its bootstrap certificate fingerprint and the systems it may host")
    @PutMapping(value="/enrollments/{deviceName}", consumes="application/json", produces="application/json")
    public EnrollmentView approve(HttpServletRequest http, @PathVariable String deviceName, @RequestBody EnrollmentRequest request) {
        return enrollments.approve(deviceName, request, actor(http));
    }
    @Operation(summary="Off-board a device; its approval stays (state OFFBOARDED) until it is approved again or removed")
    @PostMapping(value="/enrollments/{deviceName}/offboard", produces="application/json")
    public ResponseEntity<OffboardingReport> offboard(HttpServletRequest http, @PathVariable String deviceName) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(enrollments.offboard(deviceName, actor(http)));
    }
    @Operation(summary="Remove a device approval, off-boarding the device first if needed")
    @DeleteMapping(value="/enrollments/{deviceName}", produces="application/json")
    public OffboardingReport remove(HttpServletRequest http, @PathVariable String deviceName) {
        return enrollments.remove(deviceName, actor(http));
    }
    private static String actor(HttpServletRequest http) {
        Object actor = http.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
        return actor == null ? "unknown" : actor.toString();
    }
}
