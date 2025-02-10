def call(String status) {
    script {
        echo "Sending Teams Notification for ${status} pipeline."

        def environmentName = ""
        if (env.DOCKER_CONTEXT == "eaiot_dev") {
            environmentName = "DEV"
        } else if (env.DOCKER_CONTEXT == "eaiot_test") {
            environmentName = "TEST"
        } else if (env.DOCKER_CONTEXT == "eaiot_prod") {
            environmentName = "PROD"
        } else {
            environmentName = "UNKNOWN"
        }

        def repoName = env.GIT_REPO_NAME ?: env.GIT_URL?.tokenize('/')?.last()?.replace('.git', '')

        if (!repoName) {
            def scmVars = checkout scm
            repoName = scmVars?.GIT_URL?.tokenize('/')?.last()?.replace('.git', '') ?: "unknown-repo"
        }

        echo "Repository Name: ${repoName}"

        def repoMapping = [
            "aiotcloud_fe"  : [name: "Frontend", ecr: ["https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/frontend"]],
            "aiotcloud_be"  : [name: "Backend", ecr: [
                "https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/backend",
                "https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/celery_worker",
                "https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/celery_beat"
            ]],
            "aiotcloud_api" : [name: "API", ecr: ["https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/aiotcloud_api",
                "https://191685700468.dkr.ecr.eu-central-1.amazonaws.com/aiotcloud_dev/nestwork"]]
        ]

        def projectInfo = repoMapping[repoName] ?: [name: "Unknown", ecr: []]
        def projectType = projectInfo.name
        def ecrLinks = projectInfo.ecr.join("<br>")

        def backendImage = "${projectInfo.ecr[0]}:${env.TAG}"
        def celeryWorker = "${projectInfo.ecr[1]}:${env.TAG}"
        def celeryBeat   = "${projectInfo.ecr[2]}:${env.TAG}"

        def apiImage = "${projectInfo.ecr[0]}:${env.TAG}"
        def nestworkImage = "${projectInfo.ecr[1]}:${env.TAG}"

        echo "Project Type: ${projectType}"

        def teamsWebhookUrl = 'https://cleverpine.webhook.office.com/webhookb2/f94f102c-2772-4ef6-a62e-e74f06d3844f@2c4c412c-1cb6-4770-9bb4-87b4bfe440c1/IncomingWebhook/7a7a35cde20649fcb5174458c90a3008/818bedfb-a550-4da0-826d-7c40961b9d32/V2LBdAWh8RXsvXahXgJIb-G8lM8P22PSEyAZ_71TE7Dfk1'
        def COMMIT_SHA = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
        def DOCKER_IMAGE_NAME = "${projectInfo}:${env.TAG}"
        def DEPLOYMENT_DATE = new Date().format("dd.MM.yyyy")
        def DOCKER_IMAGE_FULL_NAME = "${projectInfo.ecr[0]}:${env.TAG}"
        def BUILD_DURATION = currentBuild.durationString
        def PIPELINE_URL = env.BUILD_URL
        def BRANCH_NAME = env.BRANCH_NAME
        def BRANCH_URL = "https://bitbucket.org/aiotcloudapp/${repoName}/src/${COMMIT_SHA}/?at=${BRANCH_NAME}"
        def DOCKER_CONTEXT = env.DOCKER_CONTEXT
        def DEPLOYMENT_ENVIRONMENT = environmentName

       def jsonFileMapping = [
    "aiotcloud_fe"  : [success: "fe-success-notification.json", failure: "fe-failure-notification.json"],
    "aiotcloud_be"  : [success: "be-success-notification.json", failure: "be-failure-notification.json"],
    "aiotcloud_api" : [success: "api-success-notification.json", failure: "api-failure-notification.json"]
]

def jsonFiles = jsonFileMapping[repoName] ?: [success: "unknown-success.json", failure: "unknown-failure.json"]
def JSON_FILE = status == "success" ? jsonFiles.success : jsonFiles.failure

// Load the JSON file from the shared library
def jsonContent = libraryResource("teams-notifications/${JSON_FILE}")

// Write the file into the workspace so sed can modify it
writeFile file: JSON_FILE, text: jsonContent

sh """
    echo 'Replacing placeholders in ${JSON_FILE}'
    sed -i 's|projectType|${projectType}|g' ${JSON_FILE}
    sed -i 's|DOCKER_IMAGE_FULL_NAME|${DOCKER_IMAGE_FULL_NAME}|g' ${JSON_FILE}
    sed -i 's|BRANCH_NAME|'"${BRANCH_NAME}"'|g' ${JSON_FILE}
    sed -i 's|DEPLOYMENT_DATE|${DEPLOYMENT_DATE}|g' ${JSON_FILE}
    sed -i 's|DOCKER_CONTEXT|${DOCKER_CONTEXT}|g' ${JSON_FILE}
    sed -i 's|BUILD_DURATION|${BUILD_DURATION}|g' ${JSON_FILE}
    sed -i 's|PIPELINE_URL|${PIPELINE_URL}|g' ${JSON_FILE}
    sed -i 's|BRANCH_URL|${BRANCH_URL}|g' ${JSON_FILE}
    sed -i 's|COMMIT_SHA|${COMMIT_SHA}|g' ${JSON_FILE}
    sed -i 's|DEPLOYMENT_ENVIRONMENT|${DEPLOYMENT_ENVIRONMENT}|g' ${JSON_FILE}
    sed -i 's|backendImage|${backendImage}|g' ${JSON_FILE}
    sed -i 's|celeryWorker|${celeryWorker}|g' ${JSON_FILE}
    sed -i 's|celeryBeat|${celeryBeat}|g' ${JSON_FILE}
    sed -i 's|apiImage|${apiImage}|g' ${JSON_FILE}
    sed -i 's|nestworkImage|${nestworkImage}|g' ${JSON_FILE}
"""

sh "curl -H \"Content-Type: application/json\" -X POST -d @${JSON_FILE} ${teamsWebhookUrl}"

    }
}
