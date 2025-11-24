{{- define "order-service.local.env" }}
- name: USER_SERVICE_URL
  value: "{{ .Values.global.services.userService.url }}"
{{- end }}
