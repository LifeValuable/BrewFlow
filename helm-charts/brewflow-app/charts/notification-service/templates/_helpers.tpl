{{- define "notification-service.local.env" }}
- name: SPRING_MAIL_HOST
  value: "{{ .Values.global.infrastructure.mail.host }}"
- name: SPRING_MAIL_PORT
  value: "{{ .Values.global.infrastructure.mail.port }}"
- name: BREWFLOW_MAIL_USERNAME
  value: "{{ .Values.global.infrastructure.mail.username }}"
{{- end }}
