# Ansible Playbook: Equipment-based Application Deployment

## Overview

This playbook automates the deployment of application packages to target virtual machines based on equipment configurations. It consists of two main plays: target VM determination and package delivery.

## Major Objectives

1. Dynamically identify target VMs based on equipment configurations in a ZIP package
2. Deploy and extract application packages to identified target VMs
3. Maintain deployment logs for tracking and auditing

## Technical Implementation

### Play 1: Target VM Determination

**Purpose**: Analyzes ZIP contents and equipment pools to identify appropriate deployment targets.

#### Key Components:

- **Input Parameters**:
  - `app_zip_path`: Source ZIP file location
  - `inventory_path`: Ansible inventory file
  - `equipment_pools_path`: Equipment pools configuration
  - `extract_path`: Destination path on target VMs

#### Implementation Flow:

1. **Configuration Loading**

   - Loads inventory configuration
   - Loads equipment pools configuration
   - Extracts equipment identifiers from ZIP file using pattern `equipment_\d+\.xml`
2. **Target VM Selection Logic**

   - Initializes empty target VM list
   - Maps equipment from ZIP to equipment pools
   - Identifies VMs associated with matching equipment pools
   - Creates dynamic host group `deploy_targets`

### Play 2: Package Delivery

**Purpose**: Handles the actual deployment of application packages to target VMs.

#### Implementation Flow:

1. **File Transfer**

   - Copies ZIP package to target VMs
   - Creates deployment-specific subdirectories
2. **Package Extraction**

   - Extracts ZIP contents maintaining file permissions (0644)
   - Uses `-o` flag for overwriting existing files
3. **Deployment Logging**

   - Maintains deployment record in `app_deployment.log`
   - Logs deployment timestamp, package name, and target VM

## Dependencies

- Requires `unzip` utility on target systems
- Assumes appropriate file permissions for extract_path
- Requires valid equipment pool configuration

## File Structure Requirements

- Equipment files must follow naming pattern: `equipment_\d+\.xml`
- ZIP file must contain equipment configuration files
- Inventory must define equipment pools for VM groups

## Notes

- Elevated privileges (become: yes) commands are currently commented out
- Uses dynamic inventory grouping for deployment targets
- Implements idempotent operations where possible
