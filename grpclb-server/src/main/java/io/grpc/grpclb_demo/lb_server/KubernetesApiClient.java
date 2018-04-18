package io.grpc.grpclb_demo.lb_server;

import com.google.common.collect.ImmutableList;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1EndpointAddress;
import io.kubernetes.client.models.V1EndpointSubset;
import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.authenticators.GCPAuthenticator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class KubernetesApiClient {
  
  private ApiClient client;
  private CoreV1Api api;
  
  public KubernetesApiClient() throws IOException {
    KubeConfig.registerAuthenticator(new GCPAuthenticator());
    this.client = Config.defaultClient();
    //ApiClient client = Config.fromCluster();
    //Configuration.setDefaultApiClient(client);
    this.api = new CoreV1Api(client);
  }

  public ImmutableList<InetSocketAddress> getEndpointServers(String namespace, String endpointName) throws ApiException {
    V1Endpoints result = api.readNamespacedEndpoints(endpointName, namespace, null, Boolean.TRUE, Boolean.TRUE);
    ImmutableList<InetSocketAddress> serverList = endpointToServerList(result);
    System.out.println(serverList);
    return serverList; 
  }

  private static ImmutableList<InetSocketAddress> endpointToServerList(V1Endpoints endpoints)
  {
    List<InetSocketAddress> servers = new ArrayList<>();
    for (V1EndpointSubset subset : endpoints.getSubsets()) {
      if (subset.getPorts().size() != 1) {
        throw new IllegalArgumentException("More ports defined by endpoints instance: " + subset.getPorts());
      }
      int port = subset.getPorts().get(0).getPort();
      for (V1EndpointAddress address : subset.getAddresses()) {
        servers.add(new InetSocketAddress(parseIP(address.getIp()), port));
      }
    }
    return ImmutableList.copyOf(servers);
  }

  private static InetAddress parseIP(String ip) {
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Malformed IP", e);
    }
  }
}