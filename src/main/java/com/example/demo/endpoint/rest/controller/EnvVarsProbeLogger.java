package com.example.demo.endpoint.rest.controller;

import static com.example.demo.endpoint.rest.controller.EnvVarsProbe.UNRESOLVED;

import com.example.demo.PojaGenerated;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * Reports the same measurements as the HTTP probe, at startup, so that a worker function can be
 * observed through its log group. A worker has no HTTP surface and cannot be reached otherwise.
 */
@PojaGenerated
@Slf4j
@Component
public class EnvVarsProbeLogger {

  private final ConfigurableEnvironment environment;
  private final String upperViaValue;
  private final String dottedViaValue;

  public EnvVarsProbeLogger(
      ConfigurableEnvironment environment,
      @Value("${POJA_PROBE_VALUE:" + UNRESOLVED + "}") String upperViaValue,
      @Value("${poja.probe.value:" + UNRESOLVED + "}") String dottedViaValue) {
    this.environment = environment;
    this.upperViaValue = upperViaValue;
    this.dottedViaValue = dottedViaValue;
  }

  @PostConstruct
  public void report() {
    EnvVarsProbe.report(environment, "POJA_PROBE_VALUE", upperViaValue, dottedViaValue)
        .forEach((key, value) -> log.info("env-vars-probe: {} = {}", key, value));
  }
}
