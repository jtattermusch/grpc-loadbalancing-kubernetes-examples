package io.grpc.grpclb_demo.lb_server;

public interface EventListener<T> {
  void onUpdate(T arg);
}