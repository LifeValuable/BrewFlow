{{- define "api-gateway.local.env" }}
- name: SERVICES_USER_SERVICE_URL
  value: "{{ .Values.global.services.userService.url }}"
- name: SERVICES_ORDER_SERVICE_URL
  value: "{{ .Values.global.services.orderService.url }}"
- name: SERVICES_PAYMENT_SERVICE_URL
  value: "{{ .Values.global.services.paymentService.url }}"
- name: SERVICES_NOTIFICATION_SERVICE_URL
  value: "{{ .Values.global.services.notificationService.url }}"
{{- end}}
