output "namespace" {
  value = kubernetes_namespace.demo.metadata[0].name
}

output "dw5_app_release" {
  value = helm_release.dw5_app.name
}

output "spring4_app_release" {
  value = helm_release.spring4_app.name
}
