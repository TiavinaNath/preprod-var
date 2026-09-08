package com.example.demo.endpoint.rest.controller;

import com.example.demo.PojaGenerated;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Temporary probe shared by the HTTP endpoint and the startup logger, so that a worker — which has
 * no HTTP surface — is measured exactly like the frontal function.
 *
 * <p>Values are never disclosed: each lookup is reported as a length plus a short digest, enough to
 * prove two forms see the same value.
 */
@PojaGenerated
public final class EnvVarsProbe {

  static final String UNRESOLVED = "<unresolved>";

  private EnvVarsProbe() {}

  static Map<String, Object> report(
      ConfigurableEnvironment environment, String name, String upperViaValue, String dottedViaValue) {
    String dotted = name.toLowerCase(Locale.ROOT).replace('_', '.');
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("probedUpper", name);
    report.put("probedDotted", dotted);
    report.put("System.getenv(upper)", describe(System.getenv(name)));
    report.put("System.getenv(dotted)", describe(System.getenv(dotted)));
    report.put("getProperty(upper)", describe(environment.getProperty(name)));
    report.put("getProperty(dotted)", describe(environment.getProperty(dotted)));
    report.put("@Value(${POJA_PROBE_VALUE})", describe(nullIfUnresolved(upperViaValue)));
    report.put("@Value(${poja.probe.value})", describe(nullIfUnresolved(dottedViaValue)));
    report.put("spring.datasource.url", describe(environment.getProperty("spring.datasource.url")));
    report.put("environmentBytes", environmentBytes());
    report.put("environmentEntries", System.getenv().size());
    report.put("maxStackSize", procLimit("Max stack size"));
    report.put("propertySources", propertySourceNames(environment));
    return report;
  }

  /** Size the kernel accounted for at execve: each entry is "KEY=VALUE" plus a terminating NUL. */
  private static long environmentBytes() {
    return System.getenv().entrySet().stream()
        .mapToLong(e -> e.getKey().length() + e.getValue().length() + 2L)
        .sum();
  }

  private static String procLimit(String name) {
    try {
      return Files.readAllLines(Path.of("/proc/self/limits")).stream()
          .filter(line -> line.startsWith(name))
          .findFirst()
          .map(line -> line.substring(name.length()).trim())
          .orElse("not reported");
    } catch (IOException e) {
      return "unreadable: " + e.getMessage();
    }
  }

  private static List<String> propertySourceNames(ConfigurableEnvironment environment) {
    return environment.getPropertySources().stream()
        .map(source -> source.getName() + " :: " + source.getClass().getSimpleName())
        .toList();
  }

  private static String nullIfUnresolved(String value) {
    return UNRESOLVED.equals(value) ? null : value;
  }

  private static String describe(String value) {
    if (value == null) {
      return "NULL";
    }
    return "len=" + value.length() + " sha=" + digestPrefix(value);
  }

  private static String digestPrefix(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (int i = 0; i < 4; i++) {
        hex.append(String.format("%02x", digest[i]));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
