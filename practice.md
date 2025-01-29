## Table of Contents
1. [Overview](#overview)
2. [Directory Structure](#directory-structure-a-master-class-approach)
3. [Imported AWS Resources](#imported-aws-resources)
4. [EC2 Resource Configuration](#ec2-resource-configuration)
5. [ECR Module Configuration](#ecr-elastic-container-registry-module-configuration)
6. [Amazon RDS Configuration](#amazon-rds-relational-database-service)
7. [S3 Module Configuration](#s3-simple-storage-service)
8. [Workspaces & Environment Configuration](#workspaces--environment-configuration)
9. [Usage & Deployment](#usage--deployment)
10. [Variables](#variables)
11. [Outputs](#outputs)
12. [Best Practices](#best-practices)


# Terraform Infrastructure Documentation

## 1. Overview
This document provides a detailed overview of the Terraform configuration designed to import and manage existing AWS resources across multiple environments (`test`, `dev`, `prod`). The primary objectives of this configuration are to:
- Maintain consistency across environments.
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


## 4. EC2 Resource Configuration

The **EC2 Instances** are managed using a modular approach with dynamic resources created through `for_each` for specific environments. 
The EC2 instances are defined in the module `modules/ec2/main.tf` and are configured using the `aws_instance` resource type. The configuration supports the creation of dynamic instances as well as three predefined instances 
(`emqx_prod`, `aiot_node_red`, and `builder`) with their own specific resource blocks. Below is a detailed overview of how the EC2 instances are configured and structured.

### 4.1 Dynamic EC2 Instances
The `aws_instance` resources for dynamic EC2 instances are configured using `for_each`. This allows multiple instances to be created based on input variables defined for each environment. The resources created dynamically include the `ec2_instances`, `ec2_builder`, `ec2_aiot_node_red`, and `ec2_emqx_prod` variables. The configuration for these instances includes:

- **AMI**: The Amazon Machine Image (AMI) ID to be used.
- **Instance Type**: The EC2 instance type (e.g., `t3.micro`).
- **Key Name**: The EC2 key pair name for SSH access.
- **Tags**: Tags assigned to the instances.
- **Public IP**: Whether or not to assign a public IP address.
- **EBS Optimization**: Specifies whether the instance is EBS-optimized.
- **Hibernation**: Indicates whether the instance supports hibernation.
- **Tenancy**: The tenancy setting for the instance (e.g., `default`).
- **Capacity Reservation**: Configuration related to capacity reservation.
- **Root Block Device**: Configuration for the root volume attached to the instance, including encryption, size, type, and termination behavior.

The resources for the dynamic EC2 instances are created as follows:

```hcl
resource "aws_instance" "ec2" {
  for_each = var.ec2_instances

  ami                                  = each.value.ami
  instance_type                        = each.value.instance_type
  key_name                             = each.value.key_name
  tags                                 = each.value.tags
  associate_public_ip_address          = each.value.associate_public_ip_address
  ebs_optimized                        = each.value.ebs_optimized
  hibernation                          = each.value.hibernation
  tenancy                              = each.value.tenancy
  placement_partition_number           = each.value.placement_partition_number
  secondary_private_ips                = each.value.secondary_private_ips
  capacity_reservation_specification {
    capacity_reservation_preference = each.value.capacity_reservation_preference
  }
  root_block_device {
    delete_on_termination = each.value.delete_on_termination
    encrypted             = each.value.encrypted
    iops                  = each.value.iops
    throughput            = each.value.throughput
    volume_size           = each.value.volume_size
    volume_type           = each.value.volume_type
  }
}
```

### 4.2 Predefined EC2 Instances
In addition to dynamic instances, three specific EC2 instances are defined with their own resource blocks:

- **`emqx_prod`**: Production instance for the EMQX service.
- **`aiot_node_red`**: Instance for Node-RED used for IoT workflows.
- **`builder`**: A builder instance for various automation tasks.

Each of these instances is defined with a separate resource block to allow independent management and customization:

```hcl
resource "aws_instance" "emqx_prod" {
  for_each = var.ec2_emqx_prod

  ami                                  = each.value.ami
  instance_type                        = each.value.instance_type
  key_name                             = each.value.key_name
  tags                                 = each.value.tags
  associate_public_ip_address          = each.value.associate_public_ip_address
  ebs_optimized                        = each.value.ebs_optimized
  hibernation                          = each.value.hibernation
  tenancy                              = each.value.tenancy
  placement_partition_number           = each.value.placement_partition_number
  secondary_private_ips                = each.value.secondary_private_ips

  capacity_reservation_specification {
    capacity_reservation_preference = each.value.capacity_reservation_preference
  }
 
  root_block_device {
    delete_on_termination = each.value.delete_on_termination
    encrypted             = each.value.encrypted
    iops                  = each.value.iops
    throughput            = each.value.throughput
    volume_size           = each.value.volume_size
    volume_type           = each.value.volume_type
  }
}
```

This pattern is repeated for the other predefined EC2 instances (`aiot_node_red` and `builder`).

### 4.3. Variables

The configuration uses the following variables to define the EC2 instances dynamically:

- **`ec2_instances`**: A map of configurations for general EC2 instances.
- **`ec2_builder`**: A map for the builder instance configuration.
- **`ec2_aiot_node_red`**: A map for the Node-RED instance configuration.
- **`ec2_emqx_prod`**: A map for the EMQX production instance configuration.

Each variable is defined as a map of objects with parameters such as `ami`, `instance_type`, `tags`, and `capacity_reservation_preference`, among others.

```hcl
variable "ec2_instances" {
  description = "A map of EC2 instance configurations"
  type = map(object({
    ami                                  = string
    instance_type                        = string
    key_name                             = string
    tags                                 = map(string)
    associate_public_ip_address          = bool
    ebs_optimized                        = bool
    hibernation                          = bool
    tenancy                              = string
    capacity_reservation_preference      = string
    delete_on_termination                = bool
    encrypted                            = bool
    iops                                 = number
    throughput                           = number
    volume_size                          = number
    volume_type                          = string
    placement_partition_number           = number
    secondary_private_ips                = list(string)
  }))
}
```

### 4.4. Outputs

To ensure that the EC2 instances are easily accessible, the following output is defined to display the instance IDs for all created EC2 instances:

```hcl
output "instance_ids" {
    description = "The ID's of the EC2 Instances"
    value = { for instance_name, instance_data in aws_instance.ec2 : instance_name => instance_data.id }
}
```

### 4.5. Summary

This EC2 configuration enables the creation of both dynamic and predefined EC2 instances, allowing flexibility for different environments while maintaining ease of use through input variables and outputs. The resource blocks are structured to ensure that each EC2 instance type is managed independently, with custom configuration options for each.

---
### 5. ECR (Elastic Container Registry) Module Configuration

The Elastic Container Registry (ECR) module is a key component of the Terraform configuration, responsible for managing container image repositories. This module ensures a consistent and automated approach to provisioning and maintaining ECR repositories across all environments.

### 5.1 Overview

The ECR module creates and manages the following AWS resources:

- **ECR Repositories**: Secure and scalable storage for container images.

### 5.2 Key Features

- **Dynamic Configuration**: Uses Terraform's `for_each` to create repositories dynamically based on input variables.
- **Modular Design**: Encapsulates all ECR-related resources within a single module, ensuring reusability and maintainability.
- **Shared Repositories**: All ECR repositories are available across all environments (test, dev, and prod).

### 5.3 Detailed Configuration

#### 5.3.1 ECR Repository Creation

The `aws_ecr_repository` resource is used to create private ECR repositories dynamically. The repository names are defined through a variable, making it easy to add new repositories without modifying the Terraform configuration.

```hcl
resource "aws_ecr_repository" "my_repositories" {
  for_each = var.ecr_repositories

  name = each.value.name
}
```

#### 5.3.2 Input Variables

The list of repositories is defined in the `ecr_repositories` variable, which is structured as a map of objects. Each object includes the repository name.

```hcl
variable "ecr_repositories" {
  description = "Map of private ECR repositories"
  type = map(object({
    name = string
  }))
}
```

#### 5.3.3 Imported ECR Repositories

The following ECR repositories are imported and used across all environments:

- **aiotcloud/pg-backup**
- **aiotcloud/photovoltaics**
- **aiotcloud_dev/aiotcloud_api**
- **aiotcloud_dev/aiotcloud_api_base**
- **aiotcloud_dev/backend**
- **aiotcloud_dev/celery_beat**
- **aiotcloud_dev/celery_flower**
- **aiotcloud_dev/celery_worker**
- **aiotcloud_dev/emxq_certbot**
- **aiotcloud_dev/emxq_nginx**
- **aiotcloud_dev/frontend**
- **aiotcloud_dev/nestwork**
- **aiotcloud_dev/nginx**
- **aiotcloud_dev/photovoltaics**
- **aiotcloud_photovoltaics**

These repositories provide a centralized location for container images, ensuring availability and consistency across environments.

### 5.4 Input Variables

The ECR module accepts the following input variables to customize repository creation:

| Variable Name       | Description                              | Type             |
|---------------------|------------------------------------------|------------------|
| `ecr_repositories` | Map of ECR repository configurations     | `map(object({ name = string }))` |

### 5.5 Benefits of the ECR Configuration

1. **Centralized Image Management**: Ensures all container images are stored in a single location, making deployments more streamlined.
2. **Scalability**: New repositories can be added dynamically by modifying the input variables.

This approach ensures an efficient and scalable solution for managing container image repositories in AWS using Terraform.

## 6. Amazon RDS (Relational Database Service)

### 6.1 Overview
Amazon Relational Database Service (RDS) is used to manage and scale relational database instances efficiently. This module provisions and maintains RDS instances across different environments, ensuring reliability, security, and high availability.

### 6.2 RDS Instance Configuration by Environment
The following RDS instances are provisioned based on the environment:

- **Test Environment**: RDS instance `postgreaiotcloudtest`
- **Production Environment**: RDS instance `postgreaiotcloud`
- **Development Environment**: RDS instance `aiotclouddev`

### 6.3 Terraform Configuration
#### 6.3.1 Security Group Configuration
The security groups associated with each environment are dynamically retrieved using `aws_security_group` data sources. These security groups control access to the RDS instances.

```hcl
data "aws_security_group" "sg" {
  for_each = toset(lookup({
    "dev"  = ["default", "rds-ec2-2"],
    "test" = ["rds-ec2-3", "ec2-rds-2", "rds-ec2-4"],
    "prod" = ["rds-ec2-1", "launch-wizard-1", "Amazon-QuickSight-access-new", "default"]
  }, terraform.workspace, []))

  filter {
    name   = "group-name"
    values = [each.value]
  }
}
```

#### 6.3.2 Subnet Group Configuration
All RDS instances are provisioned within a predefined DB subnet group to ensure they are deployed within the correct VPC and subnets.

```hcl
data "aws_db_subnet_group" "subnet_group" {
  name = "rds-ec2-db-subnet-group-1"
}
```

#### 6.3.3 RDS Instance Resource
Each RDS instance is dynamically created based on the `db_instances` variable. The instance settings are customized per environment, ensuring scalability and performance optimization.

```hcl
resource "aws_db_instance" "rds" {
  for_each                        = var.db_instances
  instance_class                  = each.value.instance_class
  allocated_storage               = each.value.allocated_storage
  engine                          = each.value.engine
  engine_version                  = each.value.engine_version
  db_subnet_group_name            = data.aws_db_subnet_group.subnet_group.name
  vpc_security_group_ids          = [for sg in data.aws_security_group.sg : sg.id]
  copy_tags_to_snapshot           = each.value.copy_tags_to_snapshot
  storage_encrypted               = each.value.storage_encrypted
  parameter_group_name            = each.value.parameter_group_name
  performance_insights_enabled    = each.value.performance_insights_enabled
  monitoring_interval             = each.value.monitoring_interval
  skip_final_snapshot             = each.value.skip_final_snapshot
  max_allocated_storage           = each.value.max_allocated_storage
}
```

### 6.4 Input Variables

| Variable Name       | Description                                     | Type   |
|---------------------|-------------------------------------------------|--------|
| `db_instances`     | Map defining RDS instances for each environment | `map(object(...))` |
| `security_groups`  | List of security groups for RDS instances       | `map(string)` |

##### `db_instances` Variable Schema:

```hcl
variable "db_instances" {
  type = map(object({
    instance_class                  = string
    allocated_storage               = number
    engine                          = string
    engine_version                  = string
    copy_tags_to_snapshot           = bool
    storage_encrypted               = bool
    parameter_group_name            = string
    performance_insights_enabled    = bool
    monitoring_interval             = number
    skip_final_snapshot             = bool
    max_allocated_storage           = number
  }))
}
```

### 6.5 Outputs

| Output Name          | Description                          | Type       |
|----------------------|--------------------------------------|------------|
| `rds_instance_ids`  | Map of RDS instance IDs              | `map(string)` |
| `security_group_ids` | List of security group IDs for RDS  | `map(string)` |

#### Output Configuration:

```hcl
output "rds_instance_ids" {
  description = "Map of RDS instance IDs"
  value = { for instance_name, instance in aws_db_instance.rds : instance_name => instance.id }
}

output "security_group_ids" {
  value = data.aws_security_group.sg
}
```

### 6.6 Key Features & Benefits

1. **Dynamic & Scalable**: Uses Terraform's `for_each` construct to dynamically provision RDS instances.
5. **Consistent & Maintainable**: Centralized configuration for all environments, reducing duplication and ensuring consistency.

This module simplifies RDS management by providing a reusable, environment-agnostic configuration that supports efficient database provisioning and security compliance.


## 7. S3 (Simple Storage Service)

The **S3 module** defines and manages Amazon S3 buckets used across various environments. These buckets provide object storage for backups, infrastructure state files, database storage, and public assets.

### 7.1 Overview

This module provisions multiple S3 buckets dynamically based on the provided input variables. Each bucket is uniquely defined and managed within the infrastructure, ensuring a scalable and organized storage solution.

### 7.2 S3 Buckets

The following S3 buckets are managed within this module:

- **`aiot-backups-bucket`** – Stores backup files.
- **`aiot-terraform-state`** – Maintains Terraform state files.
- **`aiotcloud-db-storage`** – Used for database storage.
- **`aiotcloud-photovoltaics`** – Stores photovoltaic-related data.
- **`aiotcloud-rds-s31102023`** – Backup storage for RDS.
- **`publicaiotcloud`** – Publicly accessible storage for shared assets.

### 7.3 Configuration Details

#### 7.3.1 S3 Bucket Creation

The S3 buckets are created dynamically using the `aws_s3_bucket` resource:

```hcl
resource "aws_s3_bucket" "buckets" {
  for_each = var.s3_buckets
  bucket   = each.value.bucket_name
}
```

#### 7.3.2 Input Variables

The module uses the following input variables to define S3 buckets:

| Variable Name  | Description                   | Type         |
|---------------|-------------------------------|-------------|
| `s3_buckets`  | Map of S3 bucket configurations | `map(object)` |

Each bucket configuration includes:

```hcl
variable "s3_buckets" {
  description = "Map of S3 bucket names"
  type = map(object({
    description = string
    bucket_name = string
  }))
}
```
---

## 8. Workspaces & Environment Configuration
Terraform workspaces are utilized to manage multiple environments (`prod`, `test`, `dev`). Each environment has a corresponding `tfvars` file, while a `shared.tfvars` file contains configurations common to all environments.

### 8.1 Workspace Setup
```sh
terraform workspace list
terraform workspace new dev
terraform workspace select dev
```

### 8.2 Usage of `tfvars` Files
When executing `terraform plan` or `terraform apply`, the following files are used:
- `shared.tfvars`: Contains variables shared across all environments, including VPC configurations, subnets, route tables, security groups, and shared EC2 instances.
- `<workspace>.tfvars`: Contains environment-specific variables (e.g., `dev.tfvars`, `prod.tfvars`).

This approach ensures that shared resources are consistently applied while allowing for environment-specific customizations.

---

## 9. Usage & Deployment

### 9.1 Prerequisites
- Install Terraform (`>=1.3.0`).
- Configure AWS CLI with appropriate credentials:
  ```sh
  aws configure
  ```
- Ensure AWS resources have been imported into Terraform.

### 9.2 Initializing Terraform
Run the following command inside the `environments` folder:
```sh
terraform init
```

### 9.3 Planning Changes
```sh
terraform plan -var-file="shared.tfvars" -var-file="dev.tfvars"
```

### 9.4 Applying Changes
```sh
terraform apply -var-file="shared.tfvars" -var-file="dev.tfvars"
```

### 9.5 Destroying Resources
```sh
terraform destroy -var-file="shared.tfvars" -var-file="dev.tfvars"
```

---

## 10. Variables
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

## 11. Outputs
| Output Name          | Description                            |
|----------------------|----------------------------------------|
| `vpc_id`            | The ID of the shared VPC              |
| `ec2_instance_id`   | The ID of the EC2 instance            |
| `rds_instance_id`   | The ID of the RDS database instance   |
| `s3_bucket_id`      | The ID of the S3 bucket               |

---

## 12. Best Practices
- **Modular Design**: Use Terraform modules to encapsulate and reuse resource configurations.
- **Avoid Hardcoding**: Leverage variables and `tfvars` files to parameterize configurations.
- **Remote State Management**: Utilize Terraform remote state to maintain consistency and collaboration across teams.
- **Resource Importing**: Use `terraform import` to bring existing AWS resources under Terraform management without recreating them.
- **Version Control**: Maintain Terraform configurations in a version-controlled repository to track changes and ensure reproducibility.

---
