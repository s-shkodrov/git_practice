# Terraform Infrastructure Documentation

## 1. Overview

This document provides an overview of the Terraform configuration for importing existing AWS resources and managing infrastructure across multiple environments (test, dev, prod). The primary goal is to analyze cost differences between imported resources and alternative configurations.

## 2. Directory Structure

```
project_root/
├── environments/
│   ├── backend.tf
│   ├── dev.tfvars
│   ├── ec2.tf
│   ├── ecr.tf
│   ├── main.tf
│   ├── outputs.tf
│   ├── prod.tfvars
│   ├── provider.tf
│   ├── rds.tf
│   ├── s3.tf
│   ├── shared.tfvars
│   ├── test.tfvars
│   ├── variables.tf
│   └── vpc.tf
├── modules/
│   ├── ec2/
│   │   ├── main.tf
│   │   ├── outputs.tf
│   │   └── variables.tf
│   ├── ecr/
│   │   ├── main.tf
│   │   └── variables.tf
│   ├── rds/
│   │   ├── main.tf
│   │   ├── outputs.tf
│   │   └── variables.tf
│   ├── s3/
│   │   ├── main.tf
│   │   └── variables.tf
│   └── vpc/
│       ├── main.tf
│       ├── outputs.tf
│       └── variables.tf
```

## 3. Infrastructure Components

- **VPC:** Imported existing AWS VPC.
- **EC2:** Virtual machines within the VPC.
- **ECR:** AWS Elastic Container Registry for storing container images.
- **RDS:** Managed database instances.
- **S3:** Storage buckets for various purposes.

## 4. Workspaces & Environments

Terraform workspaces are used to manage separate environments within a shared infrastructure setup:

```sh
terraform workspace list
terraform workspace new dev
terraform workspace select dev
```

Each environment (`prod`, `test`, `dev`) has a corresponding `tfvars` file, while `shared.tfvars` contains common configurations reused across all environments.

## 5. Usage & Deployment

### 5.1 Prerequisites

- Install Terraform (`>=1.3.0`)
- Configure AWS CLI with appropriate credentials:
  ```sh
  aws configure
  ```

### 5.2 Initializing Terraform

Run the following command inside the environment folder:

```sh
terraform init
```

### 5.3 Planning Changes

```sh
terraform plan -var-file="dev.tfvars"
```

### 5.4 Applying Changes

```sh
terraform apply -var-file="dev.tfvars"
```

### 5.5 Destroying Resources

```sh
terraform destroy -var-file="dev.tfvars"
```

## 6. Variables

| Variable Name      | Description                             | Default Value  |
| ------------------ | --------------------------------------- | -------------- |
| `aws_region`       | AWS Region to deploy resources          | `eu-central-1` |
| `vpc_id`           | Imported VPC ID                         | `vpc-xxxxxxx`  |
| `ecs_cluster_name` | Name of the ECS cluster (if applicable) | `ecs-cluster`  |

## 7. Outputs

| Output Name       | Description                         |
| ----------------- | ----------------------------------- |
| `vpc_id`          | The ID of the shared VPC            |
| `ec2_instance_id` | The ID of the EC2 instance          |
| `rds_instance_id` | The ID of the RDS database instance |
| `s3_bucket_id`    | The ID of the S3 bucket             |

## 8. Best Practices

- Use **Terraform modules** to ensure code reusability.
- Avoid hard-coded values by using variables.
- Use **Terraform remote state** for shared environments.
- Implement **IAM roles** with least privilege access.
- Regularly update Terraform providers to maintain security.

---

### Notes

- This documentation will evolve as the infrastructure changes. Always update when modifying Terraform configurations.

---

*Last Updated: [Insert Date]*

dasdasda
