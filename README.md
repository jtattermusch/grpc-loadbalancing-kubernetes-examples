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
kubernetes/docker_build_and_push.sh
```

## Example 1: gRPC Round Robin LB (built-in loadbalancing policy) 

gRPC has a round robin loadbalancer built into its clients.

First we need to deploy a headless service with multiple backends
```
# Deploy the greeter service 
kubectl create -f kubernetes/greeter-server.yaml

# Check that multiple replicas have been started
kubectl get pods
```

Deploy the client
```
# Deploy the greeter service 
kubectl create -f kubernetes/greeter-client-round-robin.yaml
```

Check client's logs and see that gRPC calls are being balanced
```
kubectl logs greeter-client-round-robin
```

TODO: scale replicas up and down and see that things work fine

## Contents

- `envoy-proxy`: A statically configured Envoy proxy (to be deployed as a sidecar)
- `greeter-server`: A simple C# greeter server (based on gRPC Helloworld example)
- `greeter-client`: A simple C# greeter client (based on gRPC Helloworld example)
- `kubernetes`: configuration for running examples on Kubernetes
