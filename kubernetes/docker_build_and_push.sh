#!/bin/sh
# Copyright 2018 gRPC authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# NOTE: Need to run "gcloud auth configure-docker" before we can push the images to gcr.io

# Build docker images & push them to container registry
docker build -t gcr.io/grpc-loadbalancing-demo2018/greeter_server ../greeter-server && docker push gcr.io/grpc-loadbalancing-demo2018/greeter_server:latest
docker build -t gcr.io/grpc-loadbalancing-demo2018/greeter_client_round_robin ../greeter-client-round-robin && docker push gcr.io/grpc-loadbalancing-demo2018/greeter_client_round_robin:latest
docker build -t gcr.io/grpc-loadbalancing-demo2018/greeter_client_lookaside_lb ../greeter-client-lookaside-lb && docker push gcr.io/grpc-loadbalancing-demo2018/greeter_client_lookaside_lb:latest
docker build -t gcr.io/grpc-loadbalancing-demo2018/greeter_server_balancer ../grpclb-server && docker push gcr.io/grpc-loadbalancing-demo2018/greeter_server_balancer:latest
