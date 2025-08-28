variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-2"
}

variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "db_cluster_name" {
  description = "Name of the Aurora PostgreSQL cluster"
  type        = string
  default     = "parkinggarage-demo"
}

variable "db_master_username" {
  description = "Master username for the RDS Aurora cluster"
  type        = string
  default     = "postgres"
}

variable "db_master_password" {
  description = "Master password for the RDS Aurora cluster"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "Instance class for the RDS Aurora instances"
  type        = string
  default     = "db.t3.medium"
}

variable "db_engine_version" {
  description = "Aurora PostgreSQL engine version"
  type        = string
  default     = "16.6"
}

variable "db_publicly_accessible" {
  description = "Whether the database should be publicly accessible"
  type        = bool
  default     = true
}

variable "db_port" {
  description = "Port for the database connection"
  type        = number
  default     = 5432
}

variable "db_backup_retention_period" {
  description = "Number of days to retain backups"
  type        = number
  default     = 3
}

variable "db_deletion_protection" {
  description = "Enable deletion protection for the database"
  type        = bool
  default     = false
}

variable "other_tags" {
  type = map(string)
  description = "tags for aws resources"
  default = {}
}