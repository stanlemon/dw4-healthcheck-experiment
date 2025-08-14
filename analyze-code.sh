#!/bin/bash

# SonarCloud Analysis Script
# Upload code analysis to SonarCloud (free for open source)

set -e

echo "🔍 Running SonarCloud Analysis..."

# Check for required environment variables
if [[ -z "$SONAR_TOKEN" ]]; then
    echo "❌ SONAR_TOKEN environment variable is required"
    echo "Get a token from: https://sonarcloud.io/account/security/"
    exit 1
fi

echo "🧪 Running tests and generating coverage reports..."
mvn clean test jacoco:report

echo "📊 Uploading to SonarCloud..."

# Set project key (can be overridden with environment variable)
PROJECT_KEY=${SONAR_PROJECT_KEY:-"stanlemon_dw4-healthcheck-experiment"}

# Set default organization (can be overridden with environment variable)
SONAR_ORGANIZATION=${SONAR_ORGANIZATION:-"stanlemon"}

# For personal accounts, organization might not be needed or could be your GitHub username
if [[ -n "$SONAR_ORGANIZATION" ]]; then
    echo "Using organization: $SONAR_ORGANIZATION"
    echo "Using project key: $PROJECT_KEY"
    mvn sonar:sonar \
        -Dsonar.projectKey="$PROJECT_KEY" \
        -Dsonar.organization="$SONAR_ORGANIZATION" \
        -Dsonar.host.url=https://sonarcloud.io \
        -Dsonar.token="$SONAR_TOKEN"
else
    echo "No organization specified - using personal account"
    echo "Using project key: $PROJECT_KEY"
    mvn sonar:sonar \
        -Dsonar.projectKey="$PROJECT_KEY" \
        -Dsonar.host.url=https://sonarcloud.io \
        -Dsonar.token="$SONAR_TOKEN"
fi

echo "✅ Analysis completed successfully!"
echo "📈 View results at: https://sonarcloud.io/project/overview?id=$PROJECT_KEY"
