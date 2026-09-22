#!/bin/bash
cd "$(dirname "$0")" || exit 1
docker load -i ./*.s
kubectl apply -f ./*.yaml