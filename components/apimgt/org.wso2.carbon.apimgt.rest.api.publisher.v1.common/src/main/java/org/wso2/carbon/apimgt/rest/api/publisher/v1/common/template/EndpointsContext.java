/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.apimgt.rest.api.publisher.v1.common.template;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.dto.EndpointConfigDTO;
import org.wso2.carbon.apimgt.api.dto.EndpointDTO;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIProduct;
import org.wso2.carbon.apimgt.api.model.EndpointSecurity;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.template.APITemplateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * EndpointsContext is responsible for handling API and APIProduct endpoint configurations.
 * It extends {@link ConfigContextDecorator} to inject endpoint details into the VelocityContext.
 */
public class EndpointsContext extends ConfigContextDecorator {

    private API api;
    private APIProduct apiProduct;
    private List<EndpointDTO> endpointDTOList;

    /**
     * Constructs an EndpointsContext instance for an API.
     *
     * @param context         The base configuration context
     * @param api             The API associated with the endpoints
     * @param endpointDTOList The list of endpoint DTOs
     */
    public EndpointsContext(ConfigContext context, API api, List<EndpointDTO> endpointDTOList) {

        super(context);
        this.api = api;
        this.endpointDTOList = endpointDTOList;
    }

    /**
     * Constructs an EndpointsContext instance for an API Product.
     *
     * @param configContext   The base configuration context
     * @param apiProduct      The API product associated with the endpoints
     * @param endpointDTOList The list of endpoint DTOs
     */
    public EndpointsContext(ConfigContext configContext, APIProduct apiProduct,
                            List<EndpointDTO> endpointDTOList) {

        super(configContext);
        this.apiProduct = apiProduct;
        this.endpointDTOList = endpointDTOList;
    }

    /**
     * Validates the configuration context.
     *
     * @throws APITemplateException   If there is an error in the API template processing
     * @throws APIManagementException If there is an API management-related error
     */
    @Override
    public void validate() throws APITemplateException, APIManagementException {

        super.validate();
    }

    /**
     * Populates the Velocity context with endpoint details categorized by environment.
     *
     * @return VelocityContext containing endpoint configurations
     */
    @Override
    public VelocityContext getContext() {

        VelocityContext context = super.getContext();

        if (this.endpointDTOList != null) {
            Map<String, List<SimplifiedEndpointDTO>> groupedEndpoints = simplifyEndpoints(this.endpointDTOList).stream()
                    .collect(Collectors.groupingBy(SimplifiedEndpointDTO::getEnvironment));

            List<SimplifiedEndpointDTO> productionEndpoints =
                    new ArrayList<>(groupedEndpoints.getOrDefault(APIConstants.PRODUCTION, Collections.emptyList()));
            List<SimplifiedEndpointDTO> sandboxEndpoints =
                    new ArrayList<>(groupedEndpoints.getOrDefault(APIConstants.SANDBOX, Collections.emptyList()));

            SimplifiedEndpointDTO defaultProductionEndpoint = Optional.ofNullable(api.getPrimaryProductionEndpointId())
                    .map(id -> findEndpointByUuid(productionEndpoints, id))
                    .orElseGet(() -> !productionEndpoints.isEmpty() ? productionEndpoints.get(0) : null);
            SimplifiedEndpointDTO defaultSandboxEndpoint = Optional.ofNullable(api.getPrimarySandboxEndpointId())
                    .map(id -> findEndpointByUuid(sandboxEndpoints, id))
                    .orElseGet(() -> !sandboxEndpoints.isEmpty() ? sandboxEndpoints.get(0) : null);

            context.put("productionEndpoints", productionEndpoints);
            context.put("sandboxEndpoints", sandboxEndpoints);
            context.put("defaultProductionEndpoint", defaultProductionEndpoint);
            context.put("defaultSandboxEndpoint", defaultSandboxEndpoint);
        } else if (api.getEndpointConfig() != null) {
            EndpointConfigDTO endpointConfigDTO = new Gson().fromJson(api.getEndpointConfig(), EndpointConfigDTO.class);
            if (endpointConfigDTO.getProductionEndpoints() != null) {
                addDefaultEndpoint(context, APIConstants.PRODUCTION, api);
            }

            if (endpointConfigDTO.getSandboxEndpoints() != null) {
                addDefaultEndpoint(context, APIConstants.SANDBOX, api);
            }
        }

        return context;
    }

    /**
     * Adds a default endpoint to the context based on the provided environment.
     *
     * @param context     The context map to store the default endpoint.
     * @param environment The environment type (Production/Sandbox).
     * @param api         The API object containing endpoint configurations.
     */
    private static void addDefaultEndpoint(VelocityContext context, String environment, API api) {

        EndpointDTO defaultEndpoint = new EndpointDTO();
        defaultEndpoint.setEnvironment(environment);
        defaultEndpoint.setEndpointConfig(new Gson().fromJson(api.getEndpointConfig(), EndpointConfigDTO.class));

        String contextKey = environment.equals(APIConstants.PRODUCTION) ? "defaultProductionEndpoint" :
                "defaultSandboxEndpoint";
        context.put(contextKey, new SimplifiedEndpointDTO(defaultEndpoint));
    }

    /**
     * Finds an endpoint by its unique identifier.
     *
     * @param endpointList The list of endpoints to search
     * @param endpointUuid The UUID of the endpoint to find
     * @return The matching {@link EndpointDTO} if found, otherwise null
     */
    public static SimplifiedEndpointDTO findEndpointByUuid(List<SimplifiedEndpointDTO> endpointList,
                                                           String endpointUuid) {

        return endpointList.stream()
                .filter(endpoint -> endpointUuid.equals(endpoint.getEndpointUuid()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Simplifies a list of EndpointDTO objects into a list of SimplifiedEndpointDTO objects.
     *
     * @param endpoints The list of endpoints to simplify
     * @return A list of simplified endpoint DTOs
     */
    public static List<SimplifiedEndpointDTO> simplifyEndpoints(List<EndpointDTO> endpoints) {

        if (endpoints.isEmpty()) {
            return new ArrayList<>();
        }
        return endpoints.stream()
                .map(SimplifiedEndpointDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * A simplified representation of an EndpointDTO, containing key details.
     */
    public static class SimplifiedEndpointDTO {

        private String endpointUuid;
        private boolean endpointSecurityEnabled;
        private String endpointName;
        private String apiKeyIdentifier;
        private String apiKeyValue;
        private String apiKeyIdentifierType;
        private String environment;
        private static final Log log = LogFactory.getLog(SimplifiedEndpointDTO.class);

        /**
         * Constructs a SimplifiedEndpointDTO from an EndpointDTO.
         *
         * @param endpointDTO The endpoint to simplify
         */
        public SimplifiedEndpointDTO(EndpointDTO endpointDTO) {

            if (endpointDTO == null) {
                return;
            }

            this.endpointUuid = endpointDTO.getEndpointUuid();
            this.endpointName = endpointDTO.getEndpointName();
            this.environment = endpointDTO.getEnvironment();

            if (endpointDTO.getEndpointConfig() != null) {
                EndpointConfigDTO.EndpointSecurityConfig securityConfig =
                        endpointDTO.getEndpointConfig().getEndpointSecurity();
                if (securityConfig != null) {
                    this.endpointSecurityEnabled = true;
                    EndpointSecurity endpointSecurity = null;
                    if (APIConstants.PRODUCTION.equals(environment)) {
                        endpointSecurity = securityConfig.getProduction();
                    } else if (APIConstants.SANDBOX.equals(environment)) {
                        endpointSecurity = securityConfig.getSandbox();
                    }
                    if (endpointSecurity != null && endpointSecurity.isEnabled()) {
                        this.apiKeyIdentifier = endpointSecurity.getApiKeyIdentifier();
                        this.apiKeyValue = endpointSecurity.getApiKeyValue();
                        this.apiKeyIdentifierType = endpointSecurity.getApiKeyIdentifierType();
                    }
                } else {
                    this.endpointSecurityEnabled = false;
                }
            }
        }

        public String getEnvironment() {

            return environment;
        }

        public String getEndpointUuid() {

            return endpointUuid;
        }

        public String getEndpointName() {

            return endpointName;
        }

        public String getApiKeyIdentifier() {

            return apiKeyIdentifier;
        }

        public String getApiKeyValue() {

            return apiKeyValue;
        }

        public String getApiKeyIdentifierType() {

            return apiKeyIdentifierType;
        }

        public boolean getEndpointSecurityEnabled() {

            return endpointSecurityEnabled;
        }

        /**
         * Returns a string representation of the simplified endpoint.
         *
         * @return A formatted string containing endpoint details
         */
        @Override
        public String toString() {

            return "SimplifiedEndpointDTO{" +
                    "  endpointUuid='" + endpointUuid + '\'' +
                    ", endpointSecurityEnabled=" + endpointSecurityEnabled +
                    ", endpointName='" + endpointName + '\'' +
                    ", apiKeyIdentifier='" + apiKeyIdentifier + '\'' +
                    ", apiKeyValue='" + apiKeyValue + '\'' +
                    ", apiKeyIdentifierType='" + apiKeyIdentifierType + '\'' +
                    ", environment='" + environment + '\'' +
                    '}';
        }
    }
}
