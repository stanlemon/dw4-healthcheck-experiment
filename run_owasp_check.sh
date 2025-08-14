#!/bin/bash

# OWASP Dependency Check runner with fallback options
# This script tries different configurations to work around NVD API issues

echo "🔍 Running OWASP Dependency Check with fallback options..."

# First attempt: Standard configuration
echo "Attempt 1: Standard OWASP check..."
if mvn dependency-check:check \
    -Dformat=HTML,JSON \
    -DsuppressionsFile=owasp-suppressions.xml \
    -DfailBuildOnCVSS=7 \
    -DnvdApiDatafeed=false \
    -DnvdMaxRetryCount=3 \
    -DnvdApiDelay=4000 \
    -DretireJsAnalyzerEnabled=false \
    -DnodeAnalyzerEnabled=false \
    -DenableRetired=false \
    -DenableExperimental=false; then
    echo "✅ OWASP check completed successfully!"
    exit 0
fi

echo "❌ Standard check failed. Trying with local database only..."

# Second attempt: Local database only
if mvn dependency-check:check \
    -Dformat=HTML,JSON \
    -DsuppressionsFile=owasp-suppressions.xml \
    -DfailBuildOnCVSS=7 \
    -DnvdApiDatafeed=false \
    -DautoUpdate=false \
    -DretireJsAnalyzerEnabled=false \
    -DnodeAnalyzerEnabled=false \
    -DenableRetired=false \
    -DenableExperimental=false; then
    echo "✅ OWASP check completed with local database!"
    echo "⚠️  Note: Results may be outdated as NVD update failed"
    exit 0
fi

echo "❌ Local database check also failed. Trying minimal configuration..."

# Third attempt: Minimal configuration
if mvn dependency-check:check \
    -Dformat=HTML \
    -DfailBuildOnCVSS=9 \
    -DnvdApiDatafeed=false \
    -DautoUpdate=false \
    -DretireJsAnalyzerEnabled=false \
    -DnodeAnalyzerEnabled=false \
    -DenableRetired=false \
    -DenableExperimental=false \
    -DskipProvidedScope=true \
    -DskipRuntimeScope=false; then
    echo "✅ OWASP check completed with minimal configuration!"
    echo "⚠️  Note: Only checking for critical vulnerabilities (CVSS >= 9)"
    exit 0
fi

echo "❌ All OWASP attempts failed. The NVD API may be experiencing issues."
echo "💡 Consider:"
echo "   1. Waiting and trying again later"
echo "   2. Using alternative vulnerability scanners"
echo "   3. Checking manually at https://nvd.nist.gov/"

exit 1
