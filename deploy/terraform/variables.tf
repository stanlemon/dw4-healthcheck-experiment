variable "namespace" {
  description = "Kubernetes namespace for the demo"
  type        = string
  default     = "healthy-demo"
}

variable "replica_count" {
  description = "Number of replicas per service"
  type        = number
  default     = 3
}
