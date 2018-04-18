/*
 * Copyright 2015, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.grpclb_demo.lb_server;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import grpc.lb.v1.LoadBalancerGrpc;
import grpc.lb.v1.LoadBalancerOuterClass;
import grpc.lb.v1.LoadBalancerOuterClass.Duration;
import grpc.lb.v1.LoadBalancerOuterClass.InitialLoadBalanceResponse;
import grpc.lb.v1.LoadBalancerOuterClass.LoadBalanceRequest;
import grpc.lb.v1.LoadBalancerOuterClass.LoadBalanceResponse;
import grpc.lb.v1.LoadBalancerOuterClass.ServerList;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Server that manages startup/shutdown of a {@code GrpclbServer} server.
 */
public class GrpclbServer {
  private static final Logger logger = Logger.getLogger(GrpclbServer.class.getName());

  private Server server;

  private void start() throws IOException {
    LoadBalancerImpl loadBalancerImpl = new LoadBalancerImpl();
    KubernetesEndpointWatcher endpointWatcher = new KubernetesEndpointWatcher();
    endpointWatcher.watchEndpoint("default", "greeter-server", loadBalancerImpl);

    /* The port on which the server should run */
    int port = 9000;
    server = ServerBuilder.forPort(port)
        .addService(loadBalancerImpl)
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        GrpclbServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final GrpclbServer server = new GrpclbServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class LoadBalancerImpl extends LoadBalancerGrpc.LoadBalancerImplBase implements ServerListWatcher {
    
    private ImmutableList<InetSocketAddress> serverList = ImmutableList.of();
    private EventBroadcaster<Void> eventBroadcaster = new EventBroadcaster<>(); 
    
    public LoadBalancerImpl() {
    }

    @Override
    public io.grpc.stub.StreamObserver<LoadBalanceRequest> balanceLoad(
        io.grpc.stub.StreamObserver<LoadBalanceResponse> responseObserver) {

      BalanceLoadImpl methodImpl = new BalanceLoadImpl(
          responseObserver,
          () -> getServerList(),
          (obj) -> eventBroadcaster.removeListener(obj));
      eventBroadcaster.addListener(methodImpl);

      return methodImpl;
    }
    
    @Override
    public void onUpdate(ImmutableList<InetSocketAddress> serverList) {
      logger.info("Updating server list: " + serverList);
      setServerList(serverList);
      eventBroadcaster.onUpdate(null);
    }

    @Override
    public void onClose(Exception e) {
      logger.warning("Error in endpoint watcher: " + e);
    }
    
    public synchronized void setServerList(ImmutableList<InetSocketAddress> servers) {
      serverList = servers;
    }
    
    public synchronized ImmutableList<InetSocketAddress> getServerList() {
      return serverList;
    }
  }

  static class BalanceLoadImpl implements StreamObserver<LoadBalanceRequest>, EventListener<Void> {
    
    interface ServerListGetter {
      ImmutableList<InetSocketAddress> get();
    }
    
    interface UnsubscribeAction {
      void unsubscribe(BalanceLoadImpl object);
    }
    
    private boolean initialResponseSent = false;
    private StreamObserver<LoadBalanceResponse> responseObserver;
    private ServerListGetter serverListGetter;
    private UnsubscribeAction unsubscribeAction;
    
    public BalanceLoadImpl(StreamObserver<LoadBalanceResponse> responseObserver, ServerListGetter serverListGetter, UnsubscribeAction unsubscribeAction) {
      this.responseObserver = responseObserver;
      this.serverListGetter = serverListGetter;
      this.unsubscribeAction = unsubscribeAction;
    }

    @Override
    public void onNext(LoadBalanceRequest req) {
      logger.log(Level.INFO, "received LoadBalanceRequest: " + req);
      SendLoadBalanceResponse();
    }

    @Override
    public void onError(Throwable t) {
      logger.log(Level.WARNING, "balanceLoad cancelled");
      unsubscribeAction.unsubscribe(this);
    }

    @Override
    public void onCompleted() {
      responseObserver.onCompleted();
      unsubscribeAction.unsubscribe(this);
    }
    
    public synchronized void SendLoadBalanceResponse()
    {
      LoadBalanceResponse.Builder builder = LoadBalanceResponse.newBuilder();
      if (!initialResponseSent)
      {
        builder.setInitialResponse(InitialLoadBalanceResponse.newBuilder()
            .setClientStatsReportInterval(Duration.newBuilder().setSeconds(10).build())
            .build());
        initialResponseSent = true;
      }

      ServerList.Builder serverListBuilder = ServerList.newBuilder();
      for (InetSocketAddress server : serverListGetter.get()) {
        serverListBuilder.addServers(getServer(server));
      }
      builder.setServerList(serverListBuilder.build());
      LoadBalanceResponse response = builder.build();
      
      logger.log(Level.INFO, "sending LoadBalanceResponse: " + response);
      responseObserver.onNext(response);
    }

    @Override
    public void onUpdate(Void unused) {
      SendLoadBalanceResponse();
    }

    private static LoadBalancerOuterClass.Server getServer(InetSocketAddress server) {
      // TODO: set balancetoken
      return LoadBalancerOuterClass.Server.newBuilder()
          .setIpAddress(ByteString.copyFrom(server.getAddress().getAddress()))
          .setPort(server.getPort())
          .build();
    }
  }
}