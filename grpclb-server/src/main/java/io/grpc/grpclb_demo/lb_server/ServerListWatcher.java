package io.grpc.grpclb_demo.lb_server;

import com.google.common.collect.ImmutableList;
import java.net.InetSocketAddress;

public interface ServerListWatcher {
  void onUpdate(ImmutableList<InetSocketAddress> serverList);
  void onClose(Exception e);
}