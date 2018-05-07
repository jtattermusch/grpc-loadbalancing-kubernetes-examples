# gRPC Load Balancing on Kubernetes examples

A collection of simple examples showing how to setup load balancing scenarios for gRPC services deployed on Kubernetes.

[Talk Slides: gRPC Loadbalancing on Kubernetes](grpc_loadbalancing_kubernetes_slides.pdf).

[Full talk video: gRPC Loadbalancing on Kubernetes](https://www.youtube.com/watch?v=F2znfxn_5Hg)


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

This example shows how to use gRPC client with its built-in round robin loadbalancer.

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

This example shows how to deploy Envoy proxy as a sidecar (2 containers in a single kubernetes pod).
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

## Example 3: Round Robin LB with dynamically configured Envoy proxy

This example shows how to perform loadbalancing via a dynamically configured Envoy proxy.
The proxy will be deployed as a sidecar and it will obtain the endpoint data from a cluster manager.
Istio pilot will be used as cluster manager. Note while the client needs to be deployed with a sidecar proxy
(which performs the loadbalancing) the destination service can be a regular kubernetes service (with no istio-related configuration).

We first need to install istio into our cluster by following 
https://istio.io/docs/setup/kubernetes/quick-start.html (also see [kubernetes/prepare_istio.sh](kubernetes/prepare_istio.sh)).

We assume greeter-server from previous example is already running.

```
# Deploy greeter client with a sidecar proxy injected by "istioctl kube-inject"
kubectl apply -f <(istioctl kube-inject -f kubernetes/greeter-client-with-envoy-dynamic.yaml)
```

Check that traffic is being load balanced
```
# See running pods and find one that corresponds to greeter-client-with-envoy-dynamic we just deployed.
kubectl get pods

# Inspect the logs an verify things are getting loadbalanced (adjust the pod name first)
kubectl logs greeter-client-with-envoy-dynamic-bb9c86bb5-rgtr9 greeter-client
```

You can try scaling up and down the number of replicas as in previous example.

You can also try applying istio route rules to traffic with destination service "greeter-server" (e.g. `kubectl apply -f kubernetes/fault-injection-rule-example.yaml`).

## Example 4: Load balancing in Istio service mesh

This example shows load balancing in Istio service mesh. Both the client and server will now have their Envoy proxy sidecars.

We assume you've already installed istio in the previous example.

```
# Deploy greeter server and client, both with a sidecar proxy injected by "istioctl kube-inject"
kubectl apply -f <(istioctl kube-inject -f kubernetes/greeter-server-istio.yaml)
kubectl apply -f <(istioctl kube-inject -f kubernetes/greeter-client-istio.yaml)
```

```
# See running pods and find one that corresponds to greeter-client-istio we just deployed.
kubectl get pods

# Inspect the logs an verify things are getting loadbalanced (adjust the pod name first)
kubectl logs greeter-client-istio-75c8ff54d8-wfpcc greeter-client
```

You can try scaling up and down the number of replicas in "greeter-server-istio" deployment.

You can also try applying istio route rules to traffic with destination service "greeter-server-istio".

## Example 5: Client Lookaside LB (with dummy grpclb server implementation)

This example shows how to run a gRPC client with lookaside balancing enabled. We will use a very minimal implementation of the grpclb service. 

**NOTE: The purpose of this example is to demonstrate the concept of a lookaside loadbalancing, not to provide a ready-to-use implementation of grpclb server. The grpclb server provided in this example doesn't do anything else than use kubernetes API watch updates to greeter-server replicas available and notify client about these updates (and you probably want something more elaborate for your application - e.g. to incorporate client stats and server load).**

We assume greeter-server from one of the previous examples is already running.

Deploy the balancer service
```
kubectl create -f kubernetes/greeter-server-balancer.yaml 
```

Deploy the client that will connect to the balancer service first and obtain streaming updates about the list of backends available for serving.
```
kubectl create -f kubernetes/greeter-client-lookaside-lb.yaml
```

Check that traffic is being load balanced
```
# Check logs for "greeter-client-lookaside-lb" pod
kubectl logs greeter-client-lookaside-lb
```

You can try scaling up and down the number of replicas as in previous example.

**Future work:  We plan to add support for Envoy's Universal Data Plane API directly into gRPC clients so that instead of needing to implement your own grpclb server to be able to perform client lookaside LB, you will be able to choose from multiple existing control-plane solutions (e.g. Istio Pilot) that implement the Universal Data Plane API. This effort is currently work in progress.**

## Contents

- `greeter-envoy-static`: A statically configured Envoy proxy (to be deployed together with greeter-client as a sidecar)
- `greeter-server`: A simple C# greeter server (based on gRPC Helloworld example)
- `greeter-client`: A simple C# greeter client (based on gRPC Helloworld example)
- `grpclb-server`: A dummy implementation of the grpclb protocol.
- `kubernetes`: configuration for running examples on Kubernetes
