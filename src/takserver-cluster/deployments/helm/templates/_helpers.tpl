{{/*
Expand the name of the chart.
*/}}
{{- define "takserver.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "takserver.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "takserver.labels" -}}
app: {{ include "takserver.fullname" . }}
app.kubernetes.io/component: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/name: {{ include "takserver.fullname" . }}
helm.sh/chart: {{ .Chart.Name }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "takserver.selectorLabels" -}}
app.kubernetes.io/name: {{ include "takserver.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
