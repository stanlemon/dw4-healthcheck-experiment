output "namespace" {
  value = kubernetes_namespace.demo.metadata[0].name
}

output "dw4_app_release" {
  value = helm_release.dw4_app.name
}

output "spring3_app_release" {
  value = helm_release.spring3_app.name
}
