package eu.arrowhead.localcloudmanager.api.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// generalManagement get-config (LocalCloudManager has no database, so no get-log); Sysop only (management filter).
@RestController
@RequestMapping(LocalCloudManagerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	@Autowired
	private ConfigService configService;

	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody KeyValuesDTO getConfig(@RequestParam(required = false, name = Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM) final List<String> keys) {
		return configService.getConfig(keys, HttpMethod.GET.name() + " " + LocalCloudManagerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_GET_CONFIG_PATH);
	}
}
