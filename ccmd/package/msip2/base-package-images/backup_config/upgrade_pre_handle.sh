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
## 如果副本数 != 0，则停止
#if [ "$REPLICAS" -ne 0 ]; then
#    echo "副本数不为0，开始停止服务..."
#    kubectl scale deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" --replicas=0
#
#    # 等待彻底停止
#    echo "等待 pod 全部关闭..."
#    kubectl wait --for=delete pod -l app=cloudcmd-im-jingxin -n "$NAMESPACE" --timeout=60s 2>/dev/null
#
#    echo "已停止：$DEPLOYMENT_NAME 副本数=0"
#else
#    echo "已经是停止状态，无需操作"
#fi
echo "upgrade_pre_handle_linkx" >/home/test.txt