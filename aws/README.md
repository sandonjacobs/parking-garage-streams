# AWS RDS Aurora PostgreSQL Terraform Configuration

This directory contains Terraform configuration files to provision an Amazon RDS Aurora PostgreSQL-compatible database cluster.

## Configuration Details

The Terraform configuration creates:

- An Aurora PostgreSQL-compatible database cluster named `sjacobs-demo`
- Dual-stack network type (IPv4 and IPv6 support)
- Public access in the default VPC
- Self-managed password authentication
- Aurora standard configuration with one primary and one replica instance

## Prerequisites

- [Terraform](https://www.terraform.io/downloads.html) (version >= 1.2.0)
- AWS CLI configured with appropriate credentials
- AWS account with permissions to create RDS resources

## Usage

### Initialize Terraform

```bash
cd aws
terraform init
```

### Configure Variables

You can configure the deployment by:

1. Editing the default values in `variables.tf`
2. Creating a `terraform.tfvars` file with your custom values
3. Passing variables via command line

At minimum, you should set a secure password for the database:

```bash
# Create a terraform.tfvars file (DO NOT commit this file to version control)
echo 'db_master_password = "YourSecurePassword123!"' > terraform.tfvars
```

### Plan the Deployment

```bash
terraform plan -out=tfplan
```

### Apply the Configuration

```bash
terraform apply tfplan
```

### Access the Database

After successful deployment, Terraform will output connection information:

```bash
terraform output
```

To get the sensitive connection string:

```bash
terraform output -raw connection_string
```

## Configuration Options

Key variables you can customize:

| Variable | Description | Default |
|----------|-------------|---------|
| `aws_region` | AWS region to deploy resources | `us-west-2` |
| `db_cluster_name` | Name of the Aurora PostgreSQL cluster | `sjacobs-demo` |
| `db_master_username` | Master username for the database | `postgres` |
| `db_master_password` | Master password for the database | `null` (must be provided) |
| `db_instance_class` | Instance class for the database instances | `db.t4g.medium` |
| `db_engine_version` | Aurora PostgreSQL engine version | `15.4` |
| `db_publicly_accessible` | Whether the database should be publicly accessible | `true` |

## Security Considerations

- The default configuration creates a publicly accessible database with a security group that allows access from any IP address. For production use, you should restrict access to specific IP ranges.
- Database credentials should be managed securely. Consider using AWS Secrets Manager for production deployments.
- Enable deletion protection for production databases by setting `db_deletion_protection = true`.

## Integration with Other Modules

This AWS RDS module can be used in conjunction with other modules in the Parking Garage Streams project:

### Stream Processing Applications
- **kafka-streams**: The row-aggregates and zone-statistics modules can store their aggregated results in this PostgreSQL database


### Data Persistence
- Store historical parking statistics and analytics
- Maintain aggregated data for reporting and dashboards
- Provide a persistent storage layer for the stream processing pipeline

### Configuration
To integrate with stream processing applications, configure the database connection using the connection string output:

```bash
# Get the connection string
terraform output -raw connection_string

# Use in application configuration
export DB_CONNECTION_STRING=$(terraform output -raw connection_string)
```

## Clean Up

To destroy all resources created by this Terraform configuration:

```bash
terraform destroy
```