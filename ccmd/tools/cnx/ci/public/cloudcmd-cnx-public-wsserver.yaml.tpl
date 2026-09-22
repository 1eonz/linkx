---
apiVersion: apps/v1
kind: Deployment
metadata:
  namespace: linkx
  name: cloudcmd-cnx-public-wsserver
  labels:
    app: cloudcmd-cnx-public-wsserver
spec:
  replicas: 1
  template:
    metadata:
      name: cloudcmd-cnx-public-wsserver
      labels:
        app: cloudcmd-cnx-public-wsserver
      annotations:
        vault.security.banzaicloud.io/vault-addr: "http://vault.default:8200"
        vault.security.banzaicloud.io/vault-skip-verify: "true"
        vault.security.banzaicloud.io/vault-role: "default"
        vault.security.banzaicloud.io/vault-path: "kubernetes"
    spec:
      containers:
        - name: cloudcmd-cnx-public-wsserver
          image: 127.0.0.1:5000/cloudcmd-cnx-public-wsserver:{{ .Values.version }}
          command: [ 'java' ]
          args:
            - -jar
            - -Djdk.internal.httpclient.disableHostnameVerification=true
            - /app/app.jar
          imagePullPolicy: IfNotPresent
          readinessProbe:
            tcpSocket:
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 3
          livenessProbe:
            tcpSocket:
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          envFrom:
            - configMapRef:
                name: cloudcmd-profile
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: vault:secret/data/linkx#MYSQL_ROOT_PASSWORD
            - name: REDIS_PASSWORD
              value: vault:secret/data/linkx#REDIS_PASSWORD
            - name: cloudcmd.service.name
              value: cloudcmd-cnx-public-wsserver
            - name: SERVER_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
            - name: pod.ip
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: status.podIP
      restartPolicy: Always
  selector:
    matchLabels:
      app: cloudcmd-cnx-public-wsserver
---
apiVersion: v1
kind: Service
metadata:
  namespace: linkx
  name: cloudcmd-cnx-public-wsserver
spec:
  selector:
    app: cloudcmd-cnx-public-wsserver
  ports:
    - port: 8867
      name: ws
      protocol: TCP
      targetPort: 8867