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
- **VPC**: The base networking layer shared across all environments. The following VPC-related resources are shared across environments:
  - **DB Subnet Group**: aws_db_subnet_group.rds
  - **Route Tables**:
    - "RDS-Pvt-rt"
    - "default"
  - **Route Table Associations**:
    - "association_1"
    - "association_2"
    - "association_3"
  - **Security Groups**:
    - "Amazon-QuickSight-access-new"
    - "Amazon_quicksight_access"
    - "Test-Fargate-Security-Group"
    - "default-1"
    - "ec2-rds-1"
    - "ec2-rds-2"
    - "ec2-rds-3"
    - "ec2-rds-4"
    - "launch-wizard-1"
    - "launch-wizard-10"
    - "launch-wizard-11"
    - "launch-wizard-12"
    - "launch-wizard-13"
    - "launch-wizard-2"
    - "launch-wizard-3"
    - "launch-wizard-4"
    - "launch-wizard-5"
    - "launch-wizard-6"
    - "launch-wizard-7"
    - "launch-wizard-8"
    - "launch-wizard-9"
    - "rds-ec2-1"
    - "rds-ec2-2"
    - "rds-ec2-3"
    - "rds-ec2-4"
    - "shelly-integrator"
  - **Subnets**:
    - "RDS-Pvt-subnet-1"
    - "RDS-Pvt-subnet-2"
    - "RDS-Pvt-subnet-3"
    - "default"
    - "default-1"
    - "default-2"
    - "shelly-integrator"

- **EC2 Instances**: Compute resources used for different applications.
  - **Test**: EC2 instance called "eaiot_test" and RDS instance called "postgreaiotcloudtest".
  - **Prod**: EC2 instances called "eaiot_prod" and "eaiot_prod_old", and RDS instance called "postgreaiotcloud".
  - **Dev**: EC2 instances called "eaiot_dev" and "eaiot_dev_old", and RDS instance called "aiotclouddev".
  - **Shared**: EC2 instances called "builder","emqx_prod" and "aiot_node_red".
- **ECR**: Container registry for storing Docker images.All imported ECR repositories are used across all environments.
  - "aiotcloud/pg-backup"
  - "aiotcloud/photovoltaics"
  - "aiotcloud_dev/aiotcloud_api"
  - "aiotcloud_dev/aiotcloud_api_base"
  - "aiotcloud_dev/backend"
  - "aiotcloud_dev/celery_beat"
  - "aiotcloud_dev/celery_flower"
  - "aiotcloud_dev/celery_worker"
  - "aiotcloud_dev/emxq_certbot"
  - "aiotcloud_dev/emxq_nginx"
  - "aiotcloud_dev/frontend"
  - "aiotcloud_dev/nestwork"
  - "aiotcloud_dev/nginx"
  - "aiotcloud_dev/photovoltaics"
  - "aiotcloud_photovoltaics"
- **RDS**: Managed relational database instances.
  - **Test**: RDS instance called "postgreaiotcloudtest".
  - **Prod**: RDS instance called "postgreaiotcloud".
  - **Dev**: RDS instance called "aiotclouddev".
- **S3**: Storage buckets used across environments.All imported S3 Buckets are used across all environments. 
  - "aiot-backups-bucket"
  - "aiot-terraform-state"
  - "aiotcloud-db-storage"
  - "aiotcloud-photovoltaics"
  - "aiotcloud-rds-s31102023"
  - "publicaiotcloud"

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
- `shared.tfvars`: Contains variables common to all environments, including the shared VPC configuration, subnets, route tables, route table associations, security groups, and three EC2 instances ("emqx_prod", "ec2_builder", and "aiot_node_red").
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


