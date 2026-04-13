package com.interview.config;

import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Starts the Pyroscope continuous-profiling agent when
 * {@code pyroscope.agent.enabled=true} (default).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "pyroscope.agent.enabled", havingValue = "true", matchIfMissing = true)
public class PyroscopeConfig {

    @Value("${pyroscope.agent.server-address:http://localhost:4040}")
    private String serverAddress;

    @Value("${pyroscope.agent.application-name:interview-app}")
    private String applicationName;

    @PostConstruct
    public void startPyroscope() {
        PyroscopeAgent.start(
                new Config.Builder()
                        .setApplicationName(applicationName)
                        .setProfilingEvent(EventType.ITIMER)
                        .setProfilingAlloc("512k")
                        .setProfilingLock("10ms")
                        .setFormat(Format.JFR)
                        .setServerAddress(serverAddress)
                        .build()
        );
        log.info("Pyroscope agent started → {}", serverAddress);
    }
}
