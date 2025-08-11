# Get default VPC
data "aws_vpc" "default" {
  default = true
}

# Get default subnets in the default VPC
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Create a subnet group using the default VPC subnets
resource "aws_db_subnet_group" "aurora_subnet_group" {
  name       = "${var.db_cluster_name}-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name = "${var.db_cluster_name}-subnet-group"
  }
}

# Create security group for the Aurora cluster
resource "aws_security_group" "aurora_sg" {
  name        = "${var.db_cluster_name}-sg"
  description = "Security group for Aurora PostgreSQL cluster"
  vpc_id      = data.aws_vpc.default.id

  # Allow PostgreSQL traffic from anywhere (for public access)
  ingress {
    from_port   = var.db_port
    to_port     = var.db_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
    description = "Allow PostgreSQL traffic"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "${var.db_cluster_name}-sg"
  }
}

# Create the Aurora PostgreSQL cluster
resource "aws_rds_cluster" "aurora_cluster" {
  cluster_identifier      = var.db_cluster_name
  engine                  = "aurora-postgresql"
  engine_version          = var.db_engine_version
  engine_mode             = "provisioned"
  database_name           = "parkinggarage"
  master_username         = var.db_master_username
  master_password         = var.db_master_password
  port                    = var.db_port
  backup_retention_period = var.db_backup_retention_period
  preferred_backup_window = "07:00-09:00"
  skip_final_snapshot     = true
  deletion_protection     = var.db_deletion_protection
  db_subnet_group_name    = aws_db_subnet_group.aurora_subnet_group.name
  vpc_security_group_ids  = [aws_security_group.aurora_sg.id]
  
  # Enable dual-stack network type (IPv4 and IPv6)
  network_type            = "IPV4"
  
  # # Enable public access
  # publicly_accessible     = var.db_publicly_accessible

  tags = {
    Name = var.db_cluster_name
  }

  lifecycle {
    ignore_changes = [
      # Ignore changes to master_password if managed externally
      master_password
    ]
  }
}

# Create Aurora PostgreSQL cluster instances
resource "aws_rds_cluster_instance" "aurora_instances" {
  count                = 2  # Primary and one replica for high availability
  identifier           = "${var.db_cluster_name}-instance-${count.index}"
  cluster_identifier   = aws_rds_cluster.aurora_cluster.id
  instance_class       = var.db_instance_class
  engine               = "aurora-postgresql"
  engine_version       = var.db_engine_version
  db_subnet_group_name = aws_db_subnet_group.aurora_subnet_group.name
  
  # Enable public access for each instance
  publicly_accessible  = var.db_publicly_accessible
  

  tags = {
    Name = "${var.db_cluster_name}-instance-${count.index}"
  }
}