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

# Based on https://istio.io/docs/setup/kubernetes/quick-start.html

# grant cluster admin permissions to the current user (admin permissions are required to create the necessary RBAC rules for Istio).
# kubectl create clusterrolebinding cluster-admin-binding \
#     --clusterrole=cluster-admin \
#     --user=$(gcloud config get-value core/account)
    
# get istio and add it to PATH
# curl -L https://git.io/getLatestIstio | sh -
# export PATH=$PWD/bin:$PATH

# kubectl apply -f install/kubernetes/istio.yaml

# verify everything's setup
# kubectl get svc -n istio-system
