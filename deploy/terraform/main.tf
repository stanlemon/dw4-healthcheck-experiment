resource "kubernetes_namespace" "demo" {
  metadata {
    name = var.namespace
  }
}

resource "helm_release" "dw4_app" {
  name      = "dw4-app"
  namespace = kubernetes_namespace.demo.metadata[0].name
  chart     = "${path.module}/../helm/healthy-app"

  values = [
    file("${path.module}/../helm/healthy-app/values-dw4.yaml")
  ]

  set {
    name  = "replicaCount"
    value = var.replica_count
  }

  depends_on = [kubernetes_namespace.demo]
}

resource "helm_release" "spring3_app" {
  name      = "spring3-app"
  namespace = kubernetes_namespace.demo.metadata[0].name
  chart     = "${path.module}/../helm/healthy-app"

  values = [
    file("${path.module}/../helm/healthy-app/values-spring3.yaml")
  ]

  set {
    name  = "replicaCount"
    value = var.replica_count
  }

  depends_on = [kubernetes_namespace.demo]
}
