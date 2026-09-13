package io.fluxzero.home.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fluxzero.sdk.common.serialization.casting.Upcast;
import org.springframework.stereotype.Component;

/** Reads the original flat descriptions without rewriting stored events or losing unrelated state. */
@Component
public class DetailsUpcaster {
    @Upcast(type = "io.fluxzero.home.model.Home", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Device", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Resident", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Zone", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Scene", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Routine", revision = 0)
    @Upcast(type = "io.fluxzero.home.model.Automation", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.CreateHome", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.AddDevice", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.AddResident", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.DefineZone", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.DefineScene", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.PlanRoutine", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.DefineAutomation", revision = 0)
    ObjectNode describe(ObjectNode value) {
        var result = value.deepCopy();
        if (!result.has("details")) {
            result.putObject("details").set("name", result.remove("name"));
        }
        return result;
    }

    @Upcast(type = "io.fluxzero.home.model.Space", revision = 0)
    @Upcast(type = "io.fluxzero.home.command.AddSpace", revision = 0)
    ObjectNode describeSpace(ObjectNode value) {
        var result = describe(value);
        if (result.has("kind")) {
            ((ObjectNode) result.get("details")).set("kind", result.remove("kind"));
        }
        return result;
    }
}
