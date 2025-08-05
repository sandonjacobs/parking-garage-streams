output "cluster_endpoint" {
  description = "The cluster endpoint for the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.endpoint
}

output "reader_endpoint" {
  description = "The reader endpoint for the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.reader_endpoint
}

output "cluster_port" {
  description = "The port for the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.port
}

output "cluster_database_name" {
  description = "The database name for the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.database_name
}

output "cluster_master_username" {
  description = "The master username for the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.master_username
}

output "cluster_arn" {
  description = "The ARN of the Aurora PostgreSQL cluster"
  value       = aws_rds_cluster.aurora_cluster.arn
}

output "cluster_instances" {
  description = "The instance identifiers of the Aurora PostgreSQL cluster"
  value       = [for instance in aws_rds_cluster_instance.aurora_instances : instance.identifier]
}

output "security_group_id" {
  description = "The ID of the security group for the Aurora PostgreSQL cluster"
  value       = aws_security_group.aurora_sg.id
}

output "subnet_group_name" {
  description = "The name of the subnet group for the Aurora PostgreSQL cluster"
  value       = aws_db_subnet_group.aurora_subnet_group.name
}

output "connection_string" {
  description = "PostgreSQL connection string for the Aurora cluster"
  value       = "postgresql://${aws_rds_cluster.aurora_cluster.master_username}:${aws_rds_cluster.aurora_cluster.master_password != null ? "PASSWORD" : "SPECIFY_PASSWORD"}@${aws_rds_cluster.aurora_cluster.endpoint}:${aws_rds_cluster.aurora_cluster.port}/${aws_rds_cluster.aurora_cluster.database_name}"
  sensitive   = true
}