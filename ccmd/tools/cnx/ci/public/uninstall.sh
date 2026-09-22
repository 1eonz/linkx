#!/bin/bash
cd "$(dirname "$0")" || exit 1
kubectl delete -f cloudcmd-ingress-30021-ai.yaml
kubectl delete -f cloudcmd-ingress-30021.yaml
kubectl delete -f cloudcmd-cnx-public-wsserver.yaml
kubectl delete -f cloudcmd-redis.yaml
kubectl delete -f cloudcmd-profile.yaml
kubectl delete -f cloudcmd-namespace.yaml
