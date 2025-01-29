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

### 3.1. VPC Module Configuration

The VPC module is a core component of the Terraform configuration, responsible for defining and managing the Virtual Private Cloud (VPC) and its associated resources. This module is designed to be highly reusable and configurable, allowing it to be easily adapted for different environments and use cases.

### 3.2 Overview
The VPC module creates and manages the following AWS resources:
- **VPC**: The foundational networking layer.
- **Route Tables**: Define routing rules for network traffic.
- **Route Table Associations**: Associate subnets with route tables.
- **Subnets**: Logical divisions within the VPC.
- **Security Groups**: Act as virtual firewalls to control inbound and outbound traffic.
- **DB Subnet Group**: A collection of subnets used for RDS instances.

### 3.3 Key Features
- **Dynamic Configuration**: Utilizes Terraform's `for_each` and `dynamic` blocks to create resources dynamically based on input variables.
- **Modular Design**: Encapsulates all VPC-related resources within a single module, promoting reusability and maintainability.
- **Flexible Inputs**: Accepts a wide range of input variables to customize the VPC configuration for different environments.

### 3.4 Detailed Configuration

#### 3.4.1 VPC Creation
The VPC is created using the `aws_vpc` resource. Key attributes include:
- **CIDR Block**: Defined by the `cidr_block` input variable.
- **DNS Support**: Enabled to allow DNS resolution within the VPC.
- **DNS Hostnames**: Enabled to assign DNS hostnames to instances.

```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.cidr_block
  enable_dns_support   = true
  enable_dns_hostnames = true
}
```

#### 3.4.2 Route Tables
Route tables are created dynamically using the `aws_route_table` resource. Each route table is configured with tags for better management and identification.

```hcl
resource "aws_route_table" "rtb" {
  for_each = var.route_tables
  vpc_id   = aws_vpc.main.id
  tags     = { for t in each.value.tags : t.key => t.value }
}
```

#### 3.4.3 Route Table Associations
Route table associations are created to link subnets with route tables. The `aws_route_table_association` resource is used for this purpose.

```hcl
data "aws_subnet" "subnet" {
  for_each = {
    for assoc, details in var.route_table_associations : 
    assoc => details.subnet_name
  }
  filter {
    name   = "tag:Name"
    values = [each.value]
  }
}

resource "aws_route_table_association" "rta" {
  for_each       = var.route_table_associations
  route_table_id = aws_route_table.rtb[each.value.route_table_name].id
  subnet_id      = data.aws_subnet.subnet[each.key].id
}
```

#### 3.4.4 Subnets
Subnets are created dynamically using the `aws_subnet` resource. Each subnet is configured with a CIDR block, name, and public IP assignment setting.

```hcl
resource "aws_subnet" "subnets" {
  for_each                = var.subnets
  vpc_id                  = aws_vpc.main.id
  cidr_block              = each.value.cidr_block
  map_public_ip_on_launch = each.value.map_public_ip_on_launch

  tags = {
    Name = each.value.subnet_name
  }
}
```

#### 3.4.5 Security Groups
Security groups are created dynamically using the `aws_security_group` resource. Each security group is configured with ingress and egress rules to control traffic.

```hcl
resource "aws_security_group" "sg" {
  for_each    = var.security_groups
  name        = each.value.name
  description = each.value.description
  vpc_id      = aws_vpc.main.id

  dynamic "ingress" {
    for_each = each.value.ingress_rules
    content {
      from_port        = ingress.value.from_port
      description      = ingress.value.description
      to_port          = ingress.value.to_port
      protocol         = ingress.value.protocol
      cidr_blocks      = ingress.value.cidr_blocks
      security_groups  = [for sg_name in ingress.value.security_groups : data.aws_security_group.sg[sg_name].id]
      self             = ingress.value.self
      ipv6_cidr_blocks = ingress.value.ipv6_cidr_blocks
      prefix_list_ids  = ingress.value.prefix_list_ids
    }
  }

  dynamic "egress" {
    for_each = each.value.egress_rules
    content {
      from_port        = egress.value.from_port
      description      = egress.value.description
      to_port          = egress.value.to_port
      protocol         = egress.value.protocol
      cidr_blocks      = egress.value.cidr_blocks
      security_groups  = [for sg_name in egress.value.security_groups : data.aws_security_group.sg[sg_name].id]
      ipv6_cidr_blocks = egress.value.ipv6_cidr_blocks
      prefix_list_ids  = egress.value.prefix_list_ids
      self             = egress.value.self
    }
  }
}

data "aws_security_group" "sg" {
  for_each = var.security_groups

  filter {
    name   = "group-name"
    values = [each.value.name]
  }
}
```

#### 3.4.6 DB Subnet Group
A DB subnet group is created for RDS instances using the `aws_db_subnet_group` resource. This group includes all subnets created within the VPC.

```hcl
resource "aws_db_subnet_group" "rds" {
  name       = "rds-ec2-db-subnet-group-1"
  description = "Created from the RDS Management Console"
  subnet_ids = [for subnet in data.aws_subnet.subnet : subnet.id]
}
```

### 3.5 Input Variables
The VPC module accepts the following input variables to customize its configuration:

| Variable Name               | Description                                       | Type        |
|-----------------------------|---------------------------------------------------|-------------|
| `cidr_block`                | CIDR block for the VPC                            | `string`    |
| `route_tables`              | Map of route table configurations                 | `map(object)` |
| `route_table_associations`  | Map of route table associations to subnets        | `map(object)` |
| `security_groups`           | Map of security group configurations              | `map(object)` |
| `subnets`                   | Map of subnet configurations                      | `map(object)` |
| `subnet_ids`                | Map of subnet names to their IDs                  | `map(string)` |

### 3.6 Outputs
The VPC module provides the following outputs for use in other modules or configurations:

| Output Name          | Description                            | Type        |
|----------------------|----------------------------------------|-------------|
| `vpc_id`            | The ID of the created VPC              | `string`    |
| `security_group_ids` | Map of security group IDs              | `map(string)` |
| `subnet_ids`        | Map of subnet IDs                      | `map(string)` |

### 3.7 Usage Example
To use the VPC module, include it in your Terraform configuration and provide the necessary input variables:

```hcl
module "vpc" {
  source = "./modules/vpc"

  cidr_block = "10.0.0.0/16"
  route_tables = {
    "rtb_1" = {
      tags = [
        { key = "Name", value = "rtb_1" }
      ]
    }
  }
  route_table_associations = {
    "association_1" = {
      route_table_name = "rtb_1"
      subnet_name      = "subnet_1"
    }
  }
  security_groups = {
    "sg_1" = {
      name        = "sg_1"
      description = "Security Group 1"
      ingress_rules = [
        {
          from_port   = 80
          to_port     = 80
          protocol    = "tcp"
          cidr_blocks = ["0.0.0.0/0"]
          description = "Allow HTTP traffic"
          security_groups = []
          self             = false
          ipv6_cidr_blocks = []
          prefix_list_ids  = []
        }
      ]
      egress_rules = [
        {
          from_port   = 0
          to_port     = 0
          protocol    = "-1"
          cidr_blocks = ["0.0.0.0/0"]
          description = "Allow all outbound traffic"
          security_groups = []
          ipv6_cidr_blocks = []
          prefix_list_ids  = []
          self             = false
        }
      ]
    }
  }
  subnets = {
    "subnet_1" = {
      cidr_block              = "10.0.1.0/24"
      subnet_name             = "subnet_1"
      map_public_ip_on_launch = true
    }
  }
}
```

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
