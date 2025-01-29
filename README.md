---

# Terraform Infrastructure Documentation

## 1. Overview
This document provides a detailed overview of the Terraform configuration designed to import and manage existing AWS resources across multiple environments (`test`, `dev`, `prod`). The primary objectives of this configuration are to:
- Maintain consistency across environments.
- Analyze cost differences between imported resources and potential alternative configurations.
- Ensure scalability, reusability, and maintainability through modular design and best practices.

The Terraform configuration follows a **Master Class approach**, leveraging a well-organized directory structure, reusable modules, and environment-specific configurations to achieve a robust and scalable infrastructure-as-code solution.

---

## 2. Directory Structure: A Master Class Approach
The directory structure is designed to promote modularity, reusability, and clear separation of concerns. This approach ensures that the configuration is scalable, maintainable, and easy to extend for future requirements.

```
project_root/
├── environments/
│   ├── backend.tf        # Terraform backend configuration for state management
│   ├── dev.tfvars        # Development environment-specific variables
│   ├── ec2.tf            # EC2 instance configuration
│   ├── ecr.tf            # Elastic Container Registry configuration
│   ├── main.tf           # Main configuration file
│   ├── outputs.tf        # Output definitions for the environment
│   ├── prod.tfvars       # Production environment-specific variables
│   ├── provider.tf       # AWS provider configuration
│   ├── rds.tf            # RDS database configuration
│   ├── s3.tf             # S3 bucket configuration
│   ├── shared.tfvars     # Shared variables used across all environments
│   ├── test.tfvars       # Testing environment-specific variables
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

### Key Features of the Master Class Approach:
1. **Modular Design**: Each AWS resource type (e.g., EC2, RDS, VPC) is encapsulated in a reusable module, promoting code reusability and reducing duplication.
2. **Environment Isolation**: Environment-specific configurations are managed using separate `tfvars` files, ensuring clear separation of concerns.
3. **Shared Configuration**: The `shared.tfvars` file centralizes common variables, reducing redundancy and ensuring consistency across environments.
4. **Scalability**: The structure is designed to accommodate future growth, making it easy to add new environments or resources.
5. **Maintainability**: Clear separation of resources and configurations simplifies troubleshooting and updates.

---

## 3. Imported AWS Resources
All resources in this Terraform configuration are imported from AWS and managed using Terraform. The imported resources include:

### 3.1 VPC (Virtual Private Cloud)
The VPC serves as the foundational networking layer shared across all environments. The following VPC-related resources are imported:
- **DB Subnet Group**: `aws_db_subnet_group.rds`
- **Route Tables**:
  - `RDS-Pvt-rt`
  - `default`
- **Route Table Associations**:
  - `association_1`
  - `association_2`
  - `association_3`
- **Security Groups**:
  - `Amazon-QuickSight-access-new`
  - `Amazon_quicksight_access`
  - `Test-Fargate-Security-Group`
  - `default-1`
  - `ec2-rds-1`
  - `ec2-rds-2`
  - `ec2-rds-3`
  - `ec2-rds-4`
  - `launch-wizard-1` to `launch-wizard-13`
  - `rds-ec2-1` to `rds-ec2-4`
  - `shelly-integrator`
- **Subnets**:
  - `RDS-Pvt-subnet-1` to `RDS-Pvt-subnet-3`
  - `default`, `default-1`, `default-2`
  - `shelly-integrator`

### 3.2 EC2 Instances
Compute resources used for different applications across environments:
- **Test**: EC2 instance `eaiot_test` and RDS instance `postgreaiotcloudtest`.
- **Prod**: EC2 instances `eaiot_prod` and `eaiot_prod_old`, and RDS instance `postgreaiotcloud`.
- **Dev**: EC2 instances `eaiot_dev` and `eaiot_dev_old`, and RDS instance `aiotclouddev`.
- **Shared**: EC2 instances `builder`, `emqx_prod`, and `aiot_node_red`.

### 3.3 ECR (Elastic Container Registry)
Container registry for storing Docker images. All imported ECR repositories are shared across environments:
- `aiotcloud/pg-backup`
- `aiotcloud/photovoltaics`
- `aiotcloud_dev/aiotcloud_api`
- `aiotcloud_dev/aiotcloud_api_base`
- `aiotcloud_dev/backend`
- `aiotcloud_dev/celery_beat`
- `aiotcloud_dev/celery_flower`
- `aiotcloud_dev/celery_worker`
- `aiotcloud_dev/emxq_certbot`
- `aiotcloud_dev/emxq_nginx`
- `aiotcloud_dev/frontend`
- `aiotcloud_dev/nestwork`
- `aiotcloud_dev/nginx`
- `aiotcloud_dev/photovoltaics`
- `aiotcloud_photovoltaics`

### 3.4 RDS (Relational Database Service)
Managed relational database instances:
- **Test**: RDS instance `postgreaiotcloudtest`.
- **Prod**: RDS instance `postgreaiotcloud`.
- **Dev**: RDS instance `aiotclouddev`.

### 3.5 S3 (Simple Storage Service)
Storage buckets used across environments:
- `aiot-backups-bucket`
- `aiot-terraform-state`
- `aiotcloud-db-storage`
- `aiotcloud-photovoltaics`
- `aiotcloud-rds-s31102023`
- `publicaiotcloud`

---

## 4. Workspaces & Environment Configuration
Terraform workspaces are utilized to manage multiple environments (`prod`, `test`, `dev`). Each environment has a corresponding `tfvars` file, while a `shared.tfvars` file contains configurations common to all environments.

### 4.1 Workspace Setup
```sh
terraform workspace list
terraform workspace new dev
terraform workspace select dev
```

### 4.2 Usage of `tfvars` Files
When executing `terraform plan` or `terraform apply`, the following files are used:
- `shared.tfvars`: Contains variables shared across all environments, including VPC configurations, subnets, route tables, security groups, and shared EC2 instances.
- `<workspace>.tfvars`: Contains environment-specific variables (e.g., `dev.tfvars`, `prod.tfvars`).

This approach ensures that shared resources are consistently applied while allowing for environment-specific customizations.

---

## 5. Usage & Deployment

### 5.1 Prerequisites
- Install Terraform (`>=1.3.0`).
- Configure AWS CLI with appropriate credentials:
  ```sh
  aws configure
  ```
- Ensure AWS resources have been imported into Terraform.

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

---

## 6. Variables
| Variable Name               | Description                                       |
|-----------------------------|---------------------------------------------------|
| `aws_region`                | AWS Region to deploy resources                   |
| `vpc_id`                    | The ID of the imported VPC                        |
| `vpc_cidr_block`            | The CIDR block of the imported VPC               |
| `subnets`                   | A map of subnet configurations                    |
| `subnet_ids`                | A map of subnet names to their IDs                |
| `security_groups`           | Security groups assigned to instances             |
| `db_instances`              | Database instance configurations                   |
| `s3_buckets`                | S3 bucket names and descriptions                   |
| `ec2_instances`             | EC2 instance configurations                        |
| `ecr_repositories`          | ECR repositories for container images              |
| `route_tables`              | Route table configurations                         |
| `route_table_associations`  | Route table associations to subnets                |

---

## 7. Outputs
| Output Name          | Description                            |
|----------------------|----------------------------------------|
| `vpc_id`            | The ID of the shared VPC              |
| `ec2_instance_id`   | The ID of the EC2 instance            |
| `rds_instance_id`   | The ID of the RDS database instance   |
| `s3_bucket_id`      | The ID of the S3 bucket               |

---

## 8. Best Practices
- **Modular Design**: Use Terraform modules to encapsulate and reuse resource configurations.
- **Avoid Hardcoding**: Leverage variables and `tfvars` files to parameterize configurations.
- **Remote State Management**: Utilize Terraform remote state to maintain consistency and collaboration across teams.
- **Resource Importing**: Use `terraform import` to bring existing AWS resources under Terraform management without recreating them.
- **Version Control**: Maintain Terraform configurations in a version-controlled repository to track changes and ensure reproducibility.

---

This documentation reflects a **Master Class approach** to Terraform configuration, ensuring scalability, maintainability, and adherence to industry best practices. By following this structure and methodology, the infrastructure is well-positioned for future growth and evolution.

--- 
