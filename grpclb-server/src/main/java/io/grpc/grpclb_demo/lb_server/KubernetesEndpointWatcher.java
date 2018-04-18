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
import java.util.logging.Logger;

public class KubernetesEndpointWatcher {
  private static final Logger logger = Logger.getLogger(KubernetesEndpointWatcher.class.getName());

  private KubernetesClient kubernetesClient = new DefaultKubernetesClient();

  public void watchEndpoint(String namespace, String endpointName, ServerListWatcher watcher) {
    kubernetesClient.endpoints()
        .inNamespace(namespace)
        .withName(endpointName)
        .watch(new Watcher<Endpoints>() {
          @Override
          public void eventReceived(Action action, Endpoints endpoints) {
            try {
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
            } catch(Exception e) {
              logger.warning("Exception in Endpoints watcher:" + e);
            }
          }

          @Override
          public void onClose(KubernetesClientException e) {
            logger.warning("error " + e);
            watcher.onClose(e);
          }
        });
  }

  private static ImmutableList<InetSocketAddress> endpointToServerList(Endpoints endpoints)
  {
    if (endpoints == null || endpoints.getSubsets() == null) {
      return ImmutableList.of();
    }
    List<InetSocketAddress> servers = new ArrayList<>();
    for (EndpointSubset subset : endpoints.getSubsets()) {
      if (subset.getPorts().size() > 1) {
        logger.warning("More than one port defined by endpoints instance: " + subset.getPorts());
      }
      if (subset.getPorts().size() == 1) { 
        int port = subset.getPorts().get(0).getPort();
        for (EndpointAddress address : subset.getAddresses()) {
          servers.add(new InetSocketAddress(parseIP(address.getIp()), port));
        }
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