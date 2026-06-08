package uk.gov.hmcts.reform.opal;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public final class LegacyStubContainerConfig {

    private static final int LEGACY_STUB_PORT = 4553;
    private static final Duration PORT_AVAILABILITY_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration PORT_AVAILABILITY_POLL_INTERVAL = Duration.ofMillis(250);
    private static final String LOCAL_LEGACY_GATEWAY_URL = "http://localhost:%d/opal".formatted(LEGACY_STUB_PORT);
    private static final String DEFAULT_LEGACY_STUB_IMAGE = "hmctsprod.azurecr.io/opal/legacy-db-stub:latest";
    private static final String LEGACY_STUB_IMAGE = resolveLegacyStubImage();
    private static final boolean ENABLE_LEGACY_STUB =
        !"false".equalsIgnoreCase(System.getenv().getOrDefault("OPAL_ENABLE_LEGACY_STUB", "true"));
    // Keep a strong reference for the lifetime of the test JVM when we start the container ourselves.
    @SuppressWarnings("unused")
    private static final GenericContainer<?> LEGACY_STUB_CONTAINER;

    static {
        if (ENABLE_LEGACY_STUB) {
            LEGACY_STUB_CONTAINER = createOrReuseLegacyStubContainer();
        } else {
            LEGACY_STUB_CONTAINER = null;
        }
    }

    private LegacyStubContainerConfig() {
    }

    private static String resolveLegacyStubImage() {
        String configuredImage = System.getenv("OPAL_LEGACY_STUB_IMAGE");
        return configuredImage == null || configuredImage.isBlank() ? DEFAULT_LEGACY_STUB_IMAGE : configuredImage;
    }

    private static GenericContainer<?> createOrReuseLegacyStubContainer() {
        if (isPortAvailable(LEGACY_STUB_PORT)) {
            return startLegacyStubContainer();
        }

        if (isLegacyGatewayHealthy()) {
            log.warn(
                "Port {} is already in use; reusing the existing legacy gateway at {}.",
                LEGACY_STUB_PORT, legacyGatewayUrl());
            return null;
        }

        if (waitForPortAvailability()) {
            return startLegacyStubContainer();
        }

        throw new IllegalStateException(
            "Legacy stub port %d is unavailable and no healthy legacy gateway is responding at %s."
                .formatted(LEGACY_STUB_PORT, legacyGatewayUrl()));
    }

    private static GenericContainer<?> startLegacyStubContainer() {
        GenericContainer<?> legacyStubContainer = new GenericContainer<>(DockerImageName.parse(LEGACY_STUB_IMAGE))
            .withExposedPorts(LEGACY_STUB_PORT)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(2));
        legacyStubContainer.setPortBindings(List.of("%d:%d".formatted(LEGACY_STUB_PORT, LEGACY_STUB_PORT)));
        legacyStubContainer.start();
        return legacyStubContainer;
    }

    public static String legacyGatewayUrl() {
        return LOCAL_LEGACY_GATEWAY_URL;
    }

    private static boolean waitForPortAvailability() {
        long deadline = System.nanoTime() + PORT_AVAILABILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isPortAvailable(LEGACY_STUB_PORT)) {
                return true;
            }
            LockSupport.parkNanos(PORT_AVAILABILITY_POLL_INTERVAL.toNanos());
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for legacy stub port availability.",
                    new InterruptedException());
            }
        }
        return false;
    }

    private static boolean isLegacyGatewayHealthy() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(
                "http://localhost:%d/health".formatted(LEGACY_STUB_PORT)).toURL().openConnection();
            connection.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
            connection.setRequestMethod("GET");
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (IOException exception) {
            return false;
        }
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
