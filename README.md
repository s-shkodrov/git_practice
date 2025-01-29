# Terraform Infrastructure Documentation

## 1. Overview
This document provides a comprehensive overview of the Terraform configuration used to import existing AWS resources and manage infrastructure across multiple environments (`test`, `dev`, `prod`). The primary goal is to maintain consistency across environments while analyzing cost differences between the imported resources and potential alternative configurations.

## 2. Directory Structure
```
project_root/
├── environments/
│   ├── backend.tf        # Terraform backend configuration
│   ├── dev.tfvars        # Development environment variables
│   ├── ec2.tf            # EC2 instance configuration
│   ├── ecr.tf            # Elastic Container Registry configuration
│   ├── main.tf           # Main configuration file
│   ├── outputs.tf        # Output definitions
│   ├── prod.tfvars       # Production environment variables
│   ├── provider.tf       # AWS provider configuration
│   ├── rds.tf            # RDS database configuration
│   ├── s3.tf             # S3 bucket configuration
│   ├── shared.tfvars     # Shared variables used across all environments
│   ├── test.tfvars       # Testing environment variables
│   ├── variables.tf      # Variable definitions
│   └── vpc.tf            # VPC configuration
├── modules/
│   ├── ec2/
│   │   ├── main.tf       # EC2 module configuration
│   │   ├── outputs.tf    # EC2 outputs
│   │   └── variables.tf  # EC2 variables
│   ├── ecr/
│   │   ├── main.tf       # ECR module configuration
│   │   └── variables.tf  # ECR variables
│   ├── rds/
│   │   ├── main.tf       # RDS module configuration
│   │   ├── outputs.tf    # RDS outputs
│   │   └── variables.tf  # RDS variables
│   ├── s3/
│   │   ├── main.tf       # S3 module configuration
│   │   └── variables.tf  # S3 variables
│   └── vpc/
│       ├── main.tf       # VPC module configuration
│       ├── outputs.tf    # VPC outputs
│       └── variables.tf  # VPC variables
```

## 3. Imported AWS Resources
All resources in this Terraform configuration are imported from AWS and managed using Terraform. The imported resources include:
- **VPC**: The base networking layer shared across all environments.
- **EC2 Instances**: Compute resources used for different applications.
- **ECR**: Container registry for storing Docker images.
- **RDS**: Managed relational database instances.
- **S3**: Storage buckets used across environments.

## 4. Workspaces & Environment Configuration
Terraform workspaces are used to manage different environments. Each environment (`prod`, `test`, `dev`) has a corresponding `tfvars` file. Additionally, a `shared.tfvars` file contains common configurations that are applied to all environments.

### 4.1 Workspace Setup
```sh
terraform workspace list
terraform workspace new dev
terraform workspace select dev
```

### 4.2 How tfvars Files Are Used
When running `terraform plan` or `terraform apply`, two `tfvars` files are always used:
- `shared.tfvars`: Contains variables common to all environments.
- `<workspace>.tfvars`: Contains environment-specific variables (e.g., `dev.tfvars`, `prod.tfvars`).

This approach ensures that shared resources do not conflict with environment-specific configurations.

## 5. Usage & Deployment
### 5.1 Prerequisites
- Install Terraform (`>=1.3.0`)
- Configure AWS CLI with appropriate credentials:
  ```sh
  aws configure
  ```
- Ensure that the AWS resources have already been imported into Terraform.

### 5.2 Initializing Terraform
Run the following command inside the `environments` folder:
```sh
terraform init
```

### 5.3 Planning Changes
```sh
terraform plan -var-file="shared.tfvars" -var-file="dev.tfvars"
```

### 5.4 Applying Changes
```sh
terraform apply -var-file="shared.tfvars" -var-file="dev.tfvars"
```

### 5.5 Destroying Resources
```sh
terraform destroy -var-file="shared.tfvars" -var-file="dev.tfvars"
```

## 6. Variables
| Variable Name         | Description                                       |
|---------------------- |------------------------------------------------- |
| `aws_region`         | AWS Region to deploy resources                   |
| `vpc_id`             | The ID of the imported VPC                        |
| `vpc_cidr_block`     | The CIDR block of the imported VPC               |
| `subnets`            | A map of subnet configurations                    |
| `subnet_ids`         | A map of subnet names to their IDs                |
| `security_groups`    | Security groups assigned to instances             |
| `db_instances`       | Database instance configurations                   |
| `s3_buckets`        | S3 bucket names and descriptions                   |
| `ec2_instances`      | EC2 instance configurations                        |
| `ecr_repositories`   | ECR repositories for container images              |
| `route_tables`       | Route table configurations                         |
| `route_table_associations` | Route table associations to subnets        |

## 7. Outputs
| Output Name          | Description                            |
|----------------------|----------------------------------------|
| `vpc_id`            | The ID of the shared VPC              |
| `ec2_instance_id`   | The ID of the EC2 instance            |
| `rds_instance_id`   | The ID of the RDS database instance   |
| `s3_bucket_id`      | The ID of the S3 bucket               |

## 8. Best Practices
- **Use Terraform modules** to ensure code reusability.
- **Avoid hard-coded values** by using variables and tfvars files.
- **Utilize Terraform remote state** to maintain consistency across environments.
- **Leverage `terraform import`** to bring existing AWS resources under Terraform management without recreating them.

---


