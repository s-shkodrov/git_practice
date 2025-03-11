 What Was Required?
Use Ansible to collect deployment metadata (e.g., target VMs, equipment, artifact details).
Attach these details as metadata to Artifactory.
Extend the playbook to log the metadata properly.


Ansible now collects metadata dynamically from:

Extracted equipment files inside the ZIP.
Target VMs based on equipment_pools.yml and inventory.yml.
The Artifactory artifact details (SHA256, repo key, size, etc.).

- name: Attach metadata to Artifactory
  ansible.builtin.command: >
    jf rt sp "{{ app_zip_path }}"
    "orpPipelineTimestamp={{ timestamp }};
    orpPipelineRunId={{ orp_pipeline_run_id }};
    orpPipelineRunLink={{ orp_pipeline_run_link }};
    orpPipelineDeployment={{ orp_pipeline_deployment }};
    artifact_size={{ artifact_size }};
    artifact_sha256={{ artifact_sha256 }};
    equipments={{ equipments_str }};
    target_vms={{ target_vms_str }}"
    --url "{{ bdc_artifactory_url }}"
    --password "{{ personal_token }}"

Playbook logging is extended:

Metadata is logged in ansible-deployment.log.
Deployment details are recorded in GitHub Actions Summary.
🚀 🔹 Conclusion: ✅ Task 1 is DONE! 🎉

=================================================================================
What Was Required?
Use lineinfile instead of relying on stdout_callback to log information.
Avoid unnecessary output and only store relevant details.


- name: Log metadata before attaching to Artifactory
  ansible.builtin.lineinfile:
    path: "/tmp/ansible-deployment.log"
    line: |
      zip_name={{ app_zip_path }}
      deploy_path={{ extract_path }}/{{ app_zip_path | basename | regex_replace('.zip$', '') }}
      target_vms={{ target_vms_str }}
      equipments={{ equipments_str }}
      artifact_size={{ artifact_size }}
      artifact_sha256={{ artifact_sha256 }}
      repo_key={{ artifact_repo_key }}
    create: yes


    ✅ Logging is focused only on necessary data:

ZIP file name, deploy path, target VMs, equipment, artifact details, and repo key.
Avoids unnecessary logs and extra Ansible output.
🚀 🔹 Conclusion: ✅ Task 2 is DONE! 🎉



########################################################################################################################################################################################################


📌 Goal
We need to automate deployments when a new ZIP file is uploaded to JFrog Artifactory by:

Detecting the new artifact in Artifactory (via webhooks).
Triggering a GitHub repository dispatch event (which will start a GitHub Actions workflow).
Running an automated deployment pipeline (fetching, validating, distributing the artifact).
📍 Why Is This Required?
🔹 1. Automate Deployments Instead of Manual Triggers
🔴 Current Problem:

Right now, deployment is triggered manually or by other indirect means.
This slows down the release process and requires human intervention.
✅ Solution:

Use Artifactory webhooks to automatically trigger GitHub Actions when a new artifact is uploaded.

🔹 2. Keep the Deployment Pipeline in Sync with Artifactory
🔴 Current Problem:

If someone uploads a ZIP to Artifactory, GitHub doesn’t know about it.
Developers must manually trigger a workflow or wait for a scheduled job.
✅ Solution:

When a ZIP is uploaded, Artifactory will send a webhook to GitHub.
GitHub repository dispatch will start the deployment immediately.
🔹 3. Ensure Only the Latest Artifacts Are Deployed
🔴 Current Problem:

The pipeline might still be deploying old artifacts even after a new one is uploaded.
✅ Solution:

Webhook ensures only the newest ZIP file is deployed.
The pipeline fetches the latest artifact version directly.
🛠 Step-by-Step Implementation
1️⃣ Set Up a Webhook in JFrog Artifactory
📌 Purpose:

Detect when a new ZIP file is uploaded.
Send a webhook notification to GitHub repository dispatch.
📌 Steps:

Log in to JFrog Artifactory.
Navigate to Administration → Webhooks.
Click "Create Webhook".
Set Trigger Event: "Artifact Uploaded".
Set the Target URL to your GitHub repository dispatch API:
bash
Copy
Edit
https://api.github.com/repos/YOUR_ORG/YOUR_REPO/dispatches
Authentication: Use a GitHub Personal Access Token (PAT) for authentication.
Save the webhook.
📌 Example Webhook Payload Sent by Artifactory:

json
Copy
Edit
{
  "repoKey": "mfi-ei-sw-local",
  "path": "playground_bosch_deliveries/ansible-poc/newApp_1.0.zip",
  "event": "artifact_uploaded",
  "sha256": "abcdef1234567890",
  "size": 320
}
✅ Now, Artifactory will notify GitHub when a ZIP is uploaded.

2️⃣ Configure GitHub to Accept Webhooks (Repository Dispatch)
📌 Purpose:

Make GitHub listen for Artifactory webhook events.
Convert those events into a GitHub Actions workflow trigger.
📌 Steps:

Open your GitHub repository.
Go to Settings → Webhooks & Services → Add Webhook.
Set the Webhook URL as:
bash
Copy
Edit
https://api.github.com/repos/YOUR_ORG/YOUR_REPO/dispatches
Use repository_dispatch as the event type.
📌 GitHub API Request Example (Sent by Artifactory):

bash
Copy
Edit
curl -X POST -H "Accept: application/vnd.github.everest-preview+json" \
-H "Authorization: token YOUR_GITHUB_TOKEN" \
https://api.github.com/repos/YOUR_ORG/YOUR_REPO/dispatches \
-d '{"event_type": "artifactory-upload", "client_payload": {"path": "playground_bosch_deliveries/ansible-poc/newApp_1.0.zip", "sha256": "abcdef1234567890", "size": 320}}'
✅ Now, GitHub will receive events when new artifacts are uploaded.

3️⃣ Create GitHub Actions Workflow to Handle Artifactory Events
📌 Purpose:

Start deployment when GitHub receives a webhook from Artifactory.
📌 Create a GitHub Actions Workflow (.github/workflows/deploy.yml)

yaml
Copy
Edit
name: Deploy on Artifactory Upload

on:
  repository_dispatch:
    types: [artifactory-upload]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v3

      - name: Extract Webhook Payload
        run: |
          echo "Artifact Path: ${{ github.event.client_payload.path }}"
          echo "SHA256: ${{ github.event.client_payload.sha256 }}"
          echo "Size: ${{ github.event.client_payload.size }}"

      - name: Run Deployment Workflow
        run: |
          ansible-playbook deploy.yml \
            --extra-vars "app_zip_path=${{ github.event.client_payload.path }}"
✅ Now, the deployment starts when Artifactory uploads a new ZIP!

4️⃣ Modify Ansible to Use the Webhook Data
📌 Purpose:

Use the artifact path and metadata from GitHub Actions inside Ansible.
📌 Modify deploy.yml (Ansible Playbook)

yaml
Copy
Edit
- name: Deploy New Artifact
  hosts: localhost
  tasks:
    - name: Download Artifact from Artifactory
      get_url:
        url: "https://artifactory.company.com/artifactory/{{ app_zip_path }}"
        dest: "/tmp/{{ app_zip_path | basename }}"
✅ Now, Ansible automatically downloads and deploys the latest artifact!

🔥 Summary
Step	What It Does?	Why It’s Needed?
1️⃣ Set Up Artifactory Webhook	Sends an event when a new ZIP is uploaded	Automates deployment triggers
2️⃣ Configure GitHub to Accept Webhooks	Makes GitHub listen for Artifactory events	Connects Artifactory to CI/CD
3️⃣ Create GitHub Actions Workflow	Starts deployment when an artifact is uploaded	Automates deployment in response to new artifacts
4️⃣ Modify Ansible to Use Webhook Data	Uses artifact metadata from GitHub Actions	Ensures the right artifact is deployed
🚀 Final Benefits
✅ Fully Automated Deployment – No manual triggers!
✅ Always Deploys the Latest Artifact – No risk of outdated versions.
✅ Improved Developer Workflow – Developers just push artifacts and deployment happens automatically.

