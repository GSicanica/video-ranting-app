#!/bin/bash

# YouTube Ratings iOS Setup Script
# This script sets up the complete iOS development environment

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname "$PROJECT_DIR")"

echo -e "${YELLOW}🚀 Setting up YouTube Ratings iOS Environment${NC}"

# Check for required tools
check_tools() {
    echo -e "${YELLOW}Checking required tools...${NC}"
    
    local missing_tools=()
    
    if ! command -v xcodebuild &> /dev/null; then
        missing_tools+=("Xcode Command Line Tools")
    fi
    
    if ! command -v pod &> /dev/null; then
        missing_tools+=("CocoaPods")
    fi
    
    if ! command -v brew &> /dev/null; then
        missing_tools+=("Homebrew")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        echo -e "${RED}❌ Missing required tools:${NC}"
        for tool in "${missing_tools[@]}"; do
            echo "  - $tool"
        done
        return 1
    fi
    
    echo -e "${GREEN}✅ All required tools found${NC}"
    return 0
}

# Install Xcode Command Line Tools if needed
install_xcode_tools() {
    if ! command -v xcodebuild &> /dev/null; then
        echo -e "${YELLOW}Installing Xcode Command Line Tools...${NC}"
        xcode-select --install
        read -p "Press Enter after installation is complete..."
    fi
}

# Install CocoaPods if needed
install_cocoapods() {
    if ! command -v pod &> /dev/null; then
        echo -e "${YELLOW}Installing CocoaPods...${NC}"
        sudo gem install cocoapods
    fi
}

# Setup iOS project
setup_ios_project() {
    echo -e "${YELLOW}Setting up iOS project...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Install CocoaPods dependencies
    echo -e "${YELLOW}Installing CocoaPods dependencies...${NC}"
    pod install --repo-update
    
    echo -e "${GREEN}✅ iOS project setup complete${NC}"
}

# Build KMP frameworks
build_kmp_frameworks() {
    echo -e "${YELLOW}Building Kotlin Multiplatform frameworks...${NC}"
    
    cd "$ROOT_DIR"
    
    ./gradlew \
        iosAppAssembleXCFrameworks \
        --no-daemon \
        --console=plain \
        -x test
    
    echo -e "${GREEN}✅ KMP frameworks built successfully${NC}"
}

# Sync frameworks to Xcode
sync_frameworks() {
    echo -e "${YELLOW}Syncing frameworks to Xcode project...${NC}"
    
    cd "$ROOT_DIR"
    
    ./gradlew \
        iosAppSyncXCFrameworks \
        --no-daemon \
        --console=plain
    
    echo -e "${GREEN}✅ Frameworks synced${NC}"
}

# Generate Xcode project
generate_xcode_project() {
    echo -e "${YELLOW}Generating Xcode project with XcodeGen...${NC}"
    
    cd "$PROJECT_DIR"
    
    if command -v xcodegen &> /dev/null; then
        xcodegen generate
        echo -e "${GREEN}✅ Xcode project generated${NC}"
    else
        echo -e "${YELLOW}⚠️  XcodeGen not found, using existing project.yml${NC}"
        echo "You can install XcodeGen with: brew install xcodegen"
    fi
}

# Validate setup
validate_setup() {
    echo -e "${YELLOW}Validating setup...${NC}"
    
    local errors=()
    
    # Check for XCFrameworks
    if [ ! -d "$PROJECT_DIR/shared.xcframework" ]; then
        errors+=("Missing shared.xcframework")
    fi
    
    if [ ! -d "$PROJECT_DIR/composeApp.xcframework" ]; then
        errors+=("Missing composeApp.xcframework")
    fi
    
    # Check for Pods
    if [ ! -d "$PROJECT_DIR/Pods" ]; then
        errors+=("CocoaPods dependencies not installed")
    fi
    
    if [ ${#errors[@]} -gt 0 ]; then
        echo -e "${RED}❌ Validation failed:${NC}"
        for error in "${errors[@]}"; do
            echo "  - $error"
        done
        return 1
    fi
    
    echo -e "${GREEN}✅ All validations passed${NC}"
    return 0
}

# Open Xcode
open_xcode() {
    echo -e "${YELLOW}Opening Xcode project...${NC}"
    cd "$PROJECT_DIR"
    open "iosApp.xcworkspace" || open "iosApp.xcodeproj"
}

# Main execution
main() {
    local skip_xcode=false
    local skip_build=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-xcode)
                skip_xcode=true
                shift
                ;;
            --skip-build)
                skip_build=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Execute setup steps
    check_tools || install_xcode_tools
    install_cocoapods
    
    if [ "$skip_build" != true ]; then
        build_kmp_frameworks
        sync_frameworks
    fi
    
    setup_ios_project
    generate_xcode_project
    validate_setup
    
    if [ "$skip_xcode" != true ]; then
        echo -e "${YELLOW}Ready to open Xcode? (y/n)${NC}"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            open_xcode
        fi
    fi
    
    echo -e "${GREEN}🎉 iOS setup complete!${NC}"
}

show_help() {
    cat << EOF
YouTube Ratings iOS Setup Script

Usage: ./setup-ios.sh [OPTIONS]

Options:
    --skip-xcode    Don't open Xcode at the end
    --skip-build    Skip building KMP frameworks
    --help          Show this help message

Environment Setup:
1. Installs required tools (Xcode, CocoaPods)
2. Builds Kotlin Multiplatform frameworks
3. Syncs frameworks to Xcode
4. Installs CocoaPods dependencies
5. Generates/validates Xcode project
6. Opens Xcode workspace

For manual setup:
    pod install
    ./gradlew iosAppAssembleXCFrameworks
    open iosApp.xcworkspace

EOF
}

# Run main
main "$@"
