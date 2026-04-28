resource "kubernetes_namespace" "demo" {
  metadata {
    name = var.namespace
  }
}

resource "helm_release" "dw5_app" {
  name      = "dw5-app"
  namespace = kubernetes_namespace.demo.metadata[0].name
  chart     = "${path.module}/../helm/healthy-app"

  values = [
    file("${path.module}/../helm/healthy-app/values-dw5.yaml")
  ]

  set {
    name  = "replicaCount"
    value = var.replica_count
  }

  depends_on = [kubernetes_namespace.demo]
}

resource "helm_release" "spring4_app" {
  name      = "spring4-app"
  namespace = kubernetes_namespace.demo.metadata[0].name
  chart     = "${path.module}/../helm/healthy-app"

  values = [
    file("${path.module}/../helm/healthy-app/values-spring4.yaml")
  ]

  set {
    name  = "replicaCount"
    value = var.replica_count
  }

  depends_on = [kubernetes_namespace.demo]
}
