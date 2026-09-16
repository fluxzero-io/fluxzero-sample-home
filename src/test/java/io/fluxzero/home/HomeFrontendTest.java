package io.fluxzero.home;

import io.fluxzero.sdk.test.TestFixture;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** CI and fz dev build frontend/dist before this production resource boundary is exercised. */
class HomeFrontendTest {
    @Test void packagedEntryPointReferencesServedAssets() {
        var fixture = TestFixture.create(HomeFrontend.class);
        String html = fixture.whenGet("/").expectWebResult(r -> r.getStatus() == 200)
                .mapWebResultMessage(r -> r.<String>getPayloadAs(String.class)).getResult(String.class);
        assertTrue(html.contains("<div id=\"root\"></div>"));
        var assets = Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(html);
        int count = 0;
        while (assets.find()) {
            fixture.whenGet(assets.group(1)).expectWebResult(r -> r.getStatus() == 200);
            count++;
        }
        assertEquals(2, count, "bundled JavaScript and CSS");
    }
}
