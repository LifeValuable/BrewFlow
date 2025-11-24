{{- define "common.name" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "common.fullname" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "common.env" -}}
{{- range $key, $value := .Values.env }}
- name: {{ $key }}
  value: {{ $value | quote }}
{{- end }}

- name: SPRING_KAFKA_BOOTSTRAP_SERVERS
  value: "{{ .Values.global.infrastructure.kafka.bootstrapServers }}"

- name: MANAGEMENT_ZIPKIN_TRACING_ENDPOINT
  value: "{{ .Values.global.infrastructure.observability.tempo.endpoint }}"

{{- include "common.env.database" . }}
{{- include "common.env.redis" . }}
{{- end }}

{{- define "common.env.database" -}}
{{- if .Values.database }}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ .Values.global.infrastructure.postgresql.host }}:{{ .Values.global.infrastructure.postgresql.port }}/{{ .Values.database.name }}"
- name: SPRING_DATASOURCE_USERNAME
  value: {{ .Values.global.infrastructure.postgresql.username | quote }}
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.global.secretName | default "brewflow-secrets" }}
      key: postgresPassword
{{- end }}
{{- end }}

{{- define "common.env.redis" -}}
{{- if .Values.redis }}
- name: SPRING_DATA_REDIS_HOST
  value: {{ .Values.global.infrastructure.redis.host }}
- name: SPRING_DATA_REDIS_PORT
  value: "{{ .Values.global.infrastructure.redis.port }}"
{{- end }}
{{- end }}

{{- define "common.env.jwt" -}}
- name: APP_JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ .Values.global.secretName | default "brewflow-secrets" }}
      key: jwtSecret
{{- end -}}

{{- define "common.env.mail" -}}
- name: BREWFLOW_MAIL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.global.secretName | default "brewflow-secrets" }}
      key: mailPassword
{{- end -}}

{{- define "local.env" }}
  {{- if eq .Chart.Name "api-gateway" }}
    {{- include "api-gateway.local.env" . }}
    {{- include "common.env.jwt" . | nindent 0 }}
  {{- else if eq .Chart.Name "notification-service" }}
    {{- include "notification-service.local.env" . }}
    {{- include "common.env.mail" . | nindent 0 }}
  {{- else if eq .Chart.Name "order-service" }}
    {{- include "order-service.local.env" . }}
  {{- else if eq .Chart.Name "user-service" }}
    {{- include "common.env.jwt" . | nindent 0 }}
  {{- end }}
{{- end }}

{{- define "common.probes" -}}
{{- $healthCheck := .Values.healthCheck | default .Values.global.healthCheck }}
{{- if $healthCheck }}
startupProbe:
  httpGet:
    path: {{ $healthCheck.startup.path }}
    port: http
  initialDelaySeconds: {{ $healthCheck.startup.initialDelaySeconds }}
  periodSeconds: {{ $healthCheck.startup.periodSeconds }}
  timeoutSeconds: {{ $healthCheck.startup.timeoutSeconds | default 3 }}
  failureThreshold: {{ $healthCheck.startup.failureThreshold | default 3 }}

livenessProbe:
  httpGet:
    path: {{ $healthCheck.liveness.path }}
    port: http
  initialDelaySeconds: {{ $healthCheck.liveness.initialDelaySeconds }}
  periodSeconds: {{ $healthCheck.liveness.periodSeconds }}
  timeoutSeconds: {{ $healthCheck.liveness.timeoutSeconds | default 5 }}
  failureThreshold: {{ $healthCheck.liveness.failureThreshold | default 3 }}

readinessProbe:
  httpGet:
    path: {{ $healthCheck.readiness.path }}
    port: http
  initialDelaySeconds: {{ $healthCheck.readiness.initialDelaySeconds }}
  periodSeconds: {{ $healthCheck.readiness.periodSeconds }}
  timeoutSeconds: {{ $healthCheck.readiness.timeoutSeconds | default 5 }}
  failureThreshold: {{ $healthCheck.readiness.failureThreshold | default 5 }}
{{- end }}
{{- end }}
