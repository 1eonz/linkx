##!/bin/bash
#
## 固定配置（和停止脚本一一对应）
#DEPLOYMENT_NAME="cloudcmd-im-jingxin"
#NAMESPACE="linkx"
#
## 获取当前副本数
#REPLICAS=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null)
#
## 判断是否获取成功
#if [ -z "$REPLICAS" ]; then
#    echo "未找到 deployment: $DEPLOYMENT_NAME -n $NAMESPACE"
#    exit 1
#fi
#
#echo "当前 $DEPLOYMENT_NAME 副本数: $REPLICAS"
#
## 如果副本数 == 0，则启动
#if [ "$REPLICAS" -eq 0 ]; then
#    echo " 当前是停止状态，开始启动..."
#
#    # 启动命令（副本数=1）
#    kubectl scale deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=1
#
#    # 等待 Pod 变成 Running 状态（最多等 60 秒）
#    echo "等待 Pod 启动完成..."
#    kubectl wait --for=condition=ready pod -l app=cloudcmd-im-jingxin -n "$NAMESPACE" --timeout=60s
#
#    echo "启动成功！"
#else
#    echo "已经是运行状态，无需操作"
#fi
echo "upgrade_post_handle_linkx" >/home/test.txt