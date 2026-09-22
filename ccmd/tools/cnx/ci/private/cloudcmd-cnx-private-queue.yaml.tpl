---
apiVersion: apps/v1
kind: Deployment
metadata:
  namespace: linkx
  name: cloudcmd-cnx-private-queue
  labels:
    app: cloudcmd-cnx-private-queue
spec:
  replicas: 1
  template:
    metadata:
      name: cloudcmd-cnx-private-queue
      labels:
        app: cloudcmd-cnx-private-queue
      annotations:
        vault.security.banzaicloud.io/vault-addr: "http://vault.default:8200"
        vault.security.banzaicloud.io/vault-skip-verify: "true"
        vault.security.banzaicloud.io/vault-role: "default"
        vault.security.banzaicloud.io/vault-path: "kubernetes"
    spec:
      containers:
        - name: cloudcmd-cnx-private-queue
          image: 127.0.0.1:5000/cloudcmd-cnx-private-queue:{{ .Values.version }}
          command: [ 'java' ]
          args:
            - -jar
            - /app/app.jar
          imagePullPolicy: IfNotPresent
          readinessProbe:
            tcpSocket:
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 3
          livenessProbe:
            tcpSocket:
              port: 8080
            initialDelaySeconds: 10
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
              value: cloudcmd-cnx-private-queue
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
      app: cloudcmd-cnx-private-queue
---
apiVersion: v1
kind: Service
metadata:
  namespace: linkx
  name: cloudcmd-cnx-private-queue
spec:
  selector:
    app: cloudcmd-cnx-private-queue
  ports:
    - port: 8080
      name: http
      protocol: TCP
      targetPort: 8080
---

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-component-30021-cnx
  namespace: linkx
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: 3096m
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "900"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "900"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "900"
spec:
  ingressClassName: ingress-30021
  rules:
    - http:
        paths:
          - path: /cnx/private
            pathType: Prefix
            backend:
              service:
                name: cloudcmd-cnx-private-queue
                port:
                  number: 8080