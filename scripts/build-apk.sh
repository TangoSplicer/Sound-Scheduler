#!/bin/bash

# APK Builder Script for Sound Scheduler
# Usage: ./build-apk.sh [debug|release|staging] [version_code] [version_name]

set -e

# Parse arguments
BUILD_TYPE=${1:-debug}
VERSION_CODE=${2:-1}
VERSION_NAME=${3:-"1.0.$VERSION_CODE"}

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}Sound Scheduler APK Builder${NC}"
echo -e "${BLUE}==========================================${NC}"
echo -e "Build Type: ${GREEN}$BUILD_TYPE${NC}"
echo -e "Version Code: ${GREEN}$VERSION_CODE${NC}"
echo -e "Version Name: ${GREEN}$VERSION_NAME${NC}"
echo -e "Timestamp: ${YELLOW}$(date -u +"%Y-%m-%dT%H:%M:%SZ")${NC}"
echo -e "${BLUE}==========================================${NC}"

# Validate build type
case $BUILD_TYPE in
    debug|release|staging)
        ;;
    *)
        echo -e "${RED}Error: Invalid build type '$BUILD_TYPE'${NC}"
        echo -e "${YELLOW}Usage: ./build-apk.sh [debug|release|staging] [version_code] [version_name]${NC}"
        exit 1
        ;;
esac

# Clean previous builds
echo -e "\n${YELLOW}Cleaning previous builds...${NC}"
./gradlew clean

# Run unit tests
echo -e "\n${YELLOW}Running unit tests...${NC}"
./gradlew testDebugUnitTest
echo -e "${GREEN}✓ Unit tests passed${NC}"

# Run lint
echo -e "\n${YELLOW}Running lint checks...${NC}"
./gradlew lintDebug
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
    echo -e "${YELLOW}⚠ Lint found issues (exit code: $LINT_EXIT_CODE)${NC}"
    echo -e "${YELLOW}Check reports at: app/build/reports/lint-results-*.html${NC}"
else
    echo -e "${GREEN}✓ Lint checks passed${NC}"
fi

# Build based on type
echo -e "\n${YELLOW}Building APK...${NC}"
case $BUILD_TYPE in
    debug)
        ./gradlew assembleDevDebug
        APK_PATH="app/build/outputs/apk/dev/debug/SoundScheduler-dev-debug-*.apk"
        ;;
    release)
        ./gradlew assembleProdRelease
        APK_PATH="app/build/outputs/apk/prod/release/SoundScheduler-prod-release-*.apk"
        ;;
    staging)
        ./gradlew assembleStagingRelease
        APK_PATH="app/build/outputs/apk/staging/release/SoundScheduler-staging-release-*.apk"
        ;;
esac

# Find the actual APK file (handle wildcard)
APK_FILE=$(ls $APK_PATH 2>/dev/null | head -n 1)

# Check if build was successful
if [ -f "$APK_FILE" ]; then
    APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
    APK_SIZE_BYTES=$(du -b "$APK_FILE" | cut -f1)
    
    echo -e "\n${GREEN}==========================================${NC}"
    echo -e "${GREEN}Build successful!${NC}"
    echo -e "${GREEN}==========================================${NC}"
    echo -e "APK Path: ${BLUE}$APK_FILE${NC}"
    echo -e "APK Size: ${BLUE}$APK_SIZE${NC}"
    echo -e "Build Type: ${GREEN}$BUILD_TYPE${NC}"
    echo -e "Version Code: ${GREEN}$VERSION_CODE${NC}"
    echo -e "Version Name: ${GREEN}$VERSION_NAME${NC}"
    
    # Validate APK size
    MAX_SIZE_BYTES=52428800  # 50MB
    if [ $APK_SIZE_BYTES -gt $MAX_SIZE_BYTES ]; then
        echo -e "\n${RED}⚠ Warning: APK size exceeds 50MB limit${NC}"
    fi
    
    # Generate build info
    BUILD_INFO_FILE="build-info.json"
    cat > "$BUILD_INFO_FILE" <<EOF
{
  "buildType": "$BUILD_TYPE",
  "versionCode": $VERSION_CODE,
  "versionName": "$VERSION_NAME",
  "buildTime": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "apkPath": "$APK_FILE",
  "apkSize": $APK_SIZE_BYTES,
  "apkSizeHuman": "$APK_SIZE",
  "gitCommit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "gitBranch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
  "gitShortCommit": "$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')",
  "builder": "APK Builder Script",
  "environment": "$BUILD_TYPE"
}
EOF
    
    echo -e "\n${YELLOW}Build info generated: $BUILD_INFO_FILE${NC}"
    echo -e "${GREEN}==========================================${NC}"
    exit 0
else
    echo -e "\n${RED}==========================================${NC}"
    echo -e "${RED}Build failed! APK not found.${NC}"
    echo -e "${RED}Expected path: $APK_PATH${NC}"
    echo -e "${RED}==========================================${NC}"
    exit 1
fi
