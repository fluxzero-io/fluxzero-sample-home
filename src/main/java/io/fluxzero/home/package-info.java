/** A home described in everyday language, independent of device brands. */
@RegisterType
@ApiDocInfo(title = "Fluxzero Home", version = "1.0.0", security = "homeSession",
        components = @ApiDocComponent(path = "securitySchemes.homeSession",
                json = "{\"type\":\"apiKey\",\"in\":\"cookie\",\"name\":\"home_session\"}"),
        serveOpenApi = true, openApiPath = "/api/openapi.json", serveApiReference = true, apiReferencePath = "/api/docs")
package io.fluxzero.home;

import io.fluxzero.common.serialization.RegisterType;
import io.fluxzero.sdk.web.ApiDocComponent;
import io.fluxzero.sdk.web.ApiDocInfo;
