# gRPC Load Balancing on Kubernetes examples

A collection of simple examples showing how to setup load balancing scenarios for gRPC services deployed on Kubernetes.

TODO: add slides from talk

## Prework

This examples have been setup on Google Kubernetes Engines, but they should work on any other kubernetes cluster (public or private).

1. Create a demo GKE cluster https://cloud.google.com/kubernetes-engine/docs/how-to/creating-a-container-cluster
   and set up the `gcloud` to make it the default cluster.
2. Make sure you can use the `kubectl` command line tool by following: https://cloud.google.com/kubernetes-engine/docs/quickstart

## Build the docker images

Build the docker images and push them to container registry so that we can later deploy them in
our kubernetes cluster.

```
$ kubernetes/docker_build_and_push.sh
```

## Example 1: Round Robin Loadbalancing with gRPC's built-in loadbalancing policy 

gRPC has a round robin loadbalancer built into its clients.

First we need to deploy a headless service with multiple backends
```
# Deploy the greeter service 
$ kubectl create -f kubernetes/greeter-server.yaml

# Check that multiple replicas have been started
$ kubectl get pods
```

Deploy the client
```
# Deploy the greeter round robin client
$ kubectl create -f kubernetes/greeter-client-round-robin.yaml
```

A little while later, check client's logs and see that gRPC calls are correctly being balanced between backends (even though all the calls have been invoked on the same channel).
```
$ kubectl logs greeter-client-round-robin
Will use round_robin load balancing policy
Creating channel with target greeter-server.default.svc.cluster.local:8000
Greeting: Hello you (Backend IP: 10.0.2.95)
Greeting: Hello you (Backend IP: 10.0.0.74)
Greeting: Hello you (Backend IP: 10.0.1.51)
...
```

Scale down the number of greeter-server's replicas and check that all the request now go to a single replica.
(when a replica does down, kubernetes removes its DNS A record and because there's been a connection failure,
gRPC will re-resolve the service name and get new list of backends).
```
# check results later with "kubectl logs greeter-client-round-robin"
kubectl scale deployment greeter-server --replicas=1
```

Scale up again and watch the client pick up newly added backends.
(note that you need to set GRPC_MAX_CONNECTION_AGE on greeter-server to force periodical DNS re-resolution so that new backends can be picked up).
```
# check results later with "kubectl logs greeter-client-round-robin"
kubectl scale deployment greeter-server --replicas=4
```

## Example 2: Round Robin LB with statically configured Envoy proxy (deployed as sidecar)

Shows how to deploy Envoy proxy as a sidecar (2 containers in a single kubernetes pod).
The Envoy proxy is statically configured to perform round robin loadbalancing
(see [greeter-envoy-static/envoy.yaml](greeter-envoy-static/envoy.yaml)).

We assume greeter-server from previous example is already running.

```
# Deploy the greeter client with envoy proxy as a sidecar
$ kubectl create -f kubernetes/greeter-client-with-envoy-static.yaml
```

Check that traffic is being load balanced
```
# Check logs for greeter-client container from "greeter-client-with-envoy-static" pod
kubectl logs greeter-client-with-envoy-static greeter-client
```

You can try scaling up and down the number of replicas as in previous example.

## Contents

- `greeter-envoy-static`: A statically configured Envoy proxy (to be deployed together with greeter-client as a sidecar)
- `greeter-server`: A simple C# greeter server (based on gRPC Helloworld example)
- `greeter-client`: A simple C# greeter client (based on gRPC Helloworld example)
- `kubernetes`: configuration for running examples on Kubernetes
