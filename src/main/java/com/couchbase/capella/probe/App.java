/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.capella.probe;

import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.util.Bytes;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

public class App {

  private static class Config {
    public String connectionString;
    public String username;
    public String password;
    public String bucket;
    public boolean captureTraffic;
  }

  public static void main(String[] args) throws IOException {

    Config config = readConfig(args.length == 0 ? null : args[0]);

    System.out.println("*** Connecting to " + config.connectionString + " as user " + config.username);
    Cluster cluster = Cluster.connect(
      config.connectionString,
      ClusterOptions.clusterOptions(config.username, config.password)
        .environment(env -> {
          env.securityConfig()
            .enableTls(true)
            .trustCertificates(capellaCaCertificates());

          // Minimize log output
          env.ioConfig()
            .maxHttpConnections(1)
            .numKvConnections(1);

          if (config.captureTraffic) {
            env.ioConfig()
              .captureTraffic(ServiceType.KV, ServiceType.MANAGER);
          }
        })
    );

    System.out.println("*** Waiting for cluster ready...");
    cluster.waitUntilReady(Duration.ofMinutes(1));

    System.out.println("*** Waiting for bucket '" + config.bucket + "' ready...");
    Bucket bucket = cluster.bucket(config.bucket);
    bucket.waitUntilReady(Duration.ofMinutes(1));


    System.out.println("*** Pinging cluster...");
    System.out.println(cluster.ping());

    System.out.println("*** Disconnecting from cluster...");
    cluster.disconnect();

    System.out.println("*** Done.");
  }

  private static List<X509Certificate> capellaCaCertificates() {
    return SecurityConfig.decodeCertificates(
      singletonList(
        new String(getResourceAsBytes("capella-ca.pem"), UTF_8)
      )
    );
  }

  private static byte[] getResourceAsBytes(String resourceName) {
    try (InputStream is = App.class.getResourceAsStream(resourceName)) {
      if (is == null) {
        throw new RuntimeException("Missing resource: " + resourceName);
      }
      return Bytes.readAllBytes(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Config readConfig(String path) throws IOException {
    String appHome = System.getenv("APP_HOME");

    final File configFile;
    if (path != null) {
      configFile = new File(path);
    } else if (appHome == null) {
      System.err.println("USAGE: capella-probe <path-to-probe-config.json>");
      System.exit(1);
      throw new AssertionError("unreachable code");
    } else {
      configFile = new File(appHome + "/config/probe-config.json");
    }

    System.out.println("Reading probe config from " + configFile.getAbsolutePath());

    try (FileInputStream is = new FileInputStream(configFile)) {
      return new ObjectMapper().readValue(is, Config.class);
    } catch (FileNotFoundException e) {
      System.err.println("Config file not found: " + configFile.getAbsolutePath());
      System.exit(1);
      throw new AssertionError("unreachable code");
    }
  }
}
