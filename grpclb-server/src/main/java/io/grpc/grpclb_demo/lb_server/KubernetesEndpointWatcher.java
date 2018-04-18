package io.grpc.grpclb_demo.lb_server;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class KubernetesEndpointWatcher {

  KubernetesClient kubernetesClient = new DefaultKubernetesClient();

  public void watchEndpoint(String namespace, String endpointName, ServerListWatcher watcher) {
    kubernetesClient.endpoints()
        .inNamespace(namespace)
        .withName(endpointName)
        .watch(new Watcher<Endpoints>() {
          @Override
          public void eventReceived(Action action, Endpoints endpoints) {
            switch (action) {
              case ADDED:
              case MODIFIED:
                watcher.onUpdate(endpointToServerList(endpoints));
                break;
              case DELETED:
                watcher.onUpdate(ImmutableList.of());
                break;
              case ERROR:
                // this will end up in onClose()
                watcher.onUpdate(ImmutableList.of());
                break;
            }
          }

          @Override
          public void onClose(KubernetesClientException e) {
            System.out.println("exception " +e );
            watcher.onClose(e);
          }
        });
  }

  private static ImmutableList<InetSocketAddress> endpointToServerList(Endpoints endpoints)
  {
    List<InetSocketAddress> servers = new ArrayList<>();
    for (EndpointSubset subset : endpoints.getSubsets()) {
      if (subset.getPorts().size() != 1) {
        throw new IllegalArgumentException("More ports defined by endpoints instance: " + subset.getPorts());
      }
      int port = subset.getPorts().get(0).getPort();
      for (EndpointAddress address : subset.getAddresses()) {
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