@Path("/api")
@RequiresUser
@ApiDoc
@ApiDocInfo(title = "Fluxzero Home", version = "1.0.0", security = "homeSession",
        components = @ApiDocComponent(path = "securitySchemes.homeSession",
                json = "{\"type\":\"apiKey\",\"in\":\"cookie\",\"name\":\"home_session\"}"),
        serveOpenApi = true, openApiPath = "openapi.json", serveApiReference = true, apiReferencePath = "docs")
package io.fluxzero.home.web;

import io.fluxzero.sdk.tracking.handling.authentication.RequiresUser;
import io.fluxzero.sdk.web.ApiDoc;
import io.fluxzero.sdk.web.ApiDocComponent;
import io.fluxzero.sdk.web.ApiDocInfo;
import io.fluxzero.sdk.web.Path;
