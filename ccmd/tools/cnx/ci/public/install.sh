#!/bin/bash
cd "$(dirname "$0")" || exit 1
docker load -i ./*.s
kubectl apply -f cloudcmd-namespace.yaml
kubectl apply -f cloudcmd-profile.yaml
kubectl apply -f cloudcmd-redis.yaml
kubectl apply -f cloudcmd-cnx-public-wsserver.yaml
kubectl apply -f cloudcmd-ingress-30021.yaml
kubectl apply -f cloudcmd-ingress-30021-ai.yaml

