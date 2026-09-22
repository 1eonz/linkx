apiVersion: batch/v1
kind: Job
metadata:
  name:  {{ .metadata.name }}
  namespace: linkx
  labels:
    app: job-db
    app.kubernetes.io/name: job-db
spec:
  template:
    metadata:
      name: {{ .metadata.name }}
      labels:
        app: job-db
        app.kubernetes.io/name: job-db
      annotations:
        safebox/enable: "true"
    spec:
      restartPolicy: Never
      containers:
        - name: {{ .metadata.name }}
          image: {{ (index .spec.template.spec.containers 0).image }}
          imagePullPolicy: Always
          command:
            - java
            - -jar
            - /app/app.jar
            - /app/drop.yaml
          env:
            - name: cloudcmd.service.name
              value: job-db
            - name: SERVER_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
            - name: pod.ip
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: status.podIP
            - name: MYSQL_PASSWORD
              value: safebox:platform/platform-secret#mysql_password
            - name: MYSQL_HOST
              value: cnp-leader.platform.svc:3306
            - name: MYSQL_SCHEMA
              value: "icp_res"
            - name: MYSQL_USER
              value: cnp