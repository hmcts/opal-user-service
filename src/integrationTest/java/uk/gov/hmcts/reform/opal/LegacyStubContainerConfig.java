package uk.gov.hmcts.reform.opal;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

@Slf4j
public final class LegacyStubContainerConfig {

    private static final int LEGACY_STUB_PORT = 4553;
    private static final String LOCAL_LEGACY_GATEWAY_URL = "http://localhost:%d/opal".formatted(LEGACY_STUB_PORT);
    private static final String LOCAL_LEGACY_ADMIN_URL = "http://localhost:%d/__admin".formatted(LEGACY_STUB_PORT);
    private static final String DEFAULT_LEGACY_STUB_IMAGE = "hmctsprod.azurecr.io/opal/legacy-db-stub:latest";
    private static final String LEGACY_STUB_IMAGE = resolveLegacyStubImage();

    private static final GenericContainer<?> LEGACY_STUB_CONTAINER;

    static {
        if (isPortAvailable(LEGACY_STUB_PORT)) {
            LEGACY_STUB_CONTAINER = new GenericContainer<>(DockerImageName.parse(LEGACY_STUB_IMAGE))
                .withExposedPorts(LEGACY_STUB_PORT)
                .waitingFor(Wait.forHttp("/health").forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(2));
            LEGACY_STUB_CONTAINER.setPortBindings(List.of("%d:%d".formatted(LEGACY_STUB_PORT, LEGACY_STUB_PORT)));
            LEGACY_STUB_CONTAINER.start();
        } else {
            LEGACY_STUB_CONTAINER = null;
            log.warn(
                "Port {} is already in use; reusing the existing legacy gateway at {}.",
                LEGACY_STUB_PORT,
                legacyGatewayUrl()
            );
        }
    }

    private LegacyStubContainerConfig() {
    }

    private static String resolveLegacyStubImage() {
        String configuredImage = System.getenv("OPAL_LEGACY_STUB_IMAGE");
        return configuredImage == null || configuredImage.isBlank() ? DEFAULT_LEGACY_STUB_IMAGE : configuredImage;
    }

    public static String legacyGatewayUrl() {
        return LOCAL_LEGACY_GATEWAY_URL;
    }

    public static String legacyAdminUrl() {
        return LOCAL_LEGACY_ADMIN_URL;
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
