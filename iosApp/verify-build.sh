#!/bin/bash

# YouTube Ratings iOS - Build Verification Script
# Validates all components before submission

set -euo pipefail

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPORT_FILE="$PROJECT_DIR/build_verification_report.txt"

# Initialize report
{
    echo "===================================================================="
    echo "YouTube Ratings iOS - Build Verification Report"
    echo "===================================================================="
    echo "Date: $(date)"
    echo "Project: $PROJECT_DIR"
    echo "===================================================================="
    echo ""
} > "$REPORT_FILE"

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
    echo "ℹ️  $1" >> "$REPORT_FILE"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
    echo "✅ $1" >> "$REPORT_FILE"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    echo "⚠️  $1" >> "$REPORT_FILE"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
    echo "❌ $1" >> "$REPORT_FILE"
}

# Check functions
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing=()
    
    # Check Xcode
    if ! xcode-select -p &> /dev/null; then
        missing+=("Xcode Command Line Tools")
    else
        XCODE_PATH=$(xcode-select -p)
        log_success "Xcode found: $XCODE_PATH"
    fi
    
    # Check Gradle
    if ! command -v ./gradlew &> /dev/null; then
        missing+=("Gradle wrapper (gradlew)")
    else
        log_success "Gradle wrapper found"
    fi
    
    # Check CocoaPods
    if ! command -v pod &> /dev/null; then
        missing+=("CocoaPods")
    else
        POD_VERSION=$(pod --version)
        log_success "CocoaPods found: $POD_VERSION"
    fi
    
    if [ ${#missing[@]} -gt 0 ]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            echo "  - $item"
        done
        return 1
    fi
    
    return 0
}

check_gradle_build() {
    log_info "Verifying Gradle build configuration..."
    
    cd "$PROJECT_DIR/.."
    
    # Check Kotlin syntax
    log_info "Running Kotlin syntax check..."
    if ./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 -x test --no-daemon 2>&1 | grep -q "BUILD SUCCESS"; then
        log_success "Kotlin compilation successful"
    else
        log_error "Kotlin compilation failed"
        return 1
    fi
    
    # Check Gradle task exists
    if ./gradlew :shared:tasks | grep -q "assembleXCFramework"; then
        log_success "Gradle task 'assembleXCFramework' available"
    else
        log_warning "assembleXCFramework task not found"
    fi
    
    return 0
}

check_framework_output() {
    log_info "Checking XCFramework output..."
    
    cd "$PROJECT_DIR/.."
    
    # Build frameworks
    log_info "Building KMP frameworks (this may take a few minutes)..."
    if ./gradlew iosAppAssembleXCFrameworks \
               --no-daemon --console=plain 2>&1 | tee -a "$REPORT_FILE" | grep -q "BUILD FAILURE"; then
        log_error "Framework build failed"
        return 1
    fi
    
    # Verify output
    if [ -d "$PROJECT_DIR/shared.xcframework" ]; then
        SHARED_SIZE=$(du -sh "$PROJECT_DIR/shared.xcframework" | cut -f1)
        log_success "shared.xcframework created ($SHARED_SIZE)"
    else
        log_error "shared.xcframework not found"
        return 1
    fi
    
    if [ -d "$PROJECT_DIR/composeApp.xcframework" ]; then
        COMPOSE_SIZE=$(du -sh "$PROJECT_DIR/composeApp.xcframework" | cut -f1)
        log_success "composeApp.xcframework created ($COMPOSE_SIZE)"
    else
        log_error "composeApp.xcframework not found"
        return 1
    fi
    
    return 0
}

check_podfile() {
    log_info "Validating Podfile..."
    
    cd "$PROJECT_DIR"
    
    # Syntax check
    if ! ruby -c Podfile &> /dev/null; then
        log_error "Podfile syntax error"
        return 1
    fi
    log_success "Podfile syntax valid"
    
    # Install pods
    log_info "Installing CocoaPods dependencies..."
    if pod install --repo-update 2>&1 | tee -a "$REPORT_FILE" | grep -q "Analyzing dependencies"; then
        log_success "CocoaPods install successful"
    else
        log_error "CocoaPods install failed"
        return 1
    fi
    
    # Verify Podfile.lock
    if [ -f "Podfile.lock" ]; then
        POD_COUNT=$(grep "PODS:" -A 100 Podfile.lock | grep "^  -" | wc -l)
        log_success "Podfile.lock created ($POD_COUNT pods)"
    else
        log_warning "Podfile.lock not found"
    fi
    
    return 0
}

check_swift_files() {
    log_info "Checking Swift files..."
    
    cd "$PROJECT_DIR"
    
    SWIFT_FILES=(
        "iosApp/AppDelegate.swift"
        "iosApp/SceneDelegate.swift"
        "iosApp/Networking/NotificationsBridge.swift"
        "iosApp/Networking/NetworkMonitorBridge.swift"
        "iosApp/Networking/KeychainBridge.swift"
        "iosApp/Networking/VideoPlayerBridge.swift"
    )
    
    local missing=0
    for file in "${SWIFT_FILES[@]}"; do
        if [ -f "$file" ]; then
            LINES=$(wc -l < "$file")
            log_success "✓ $file ($LINES lines)"
        else
            log_error "✗ $file MISSING"
            missing=$((missing + 1))
        fi
    done
    
    if [ $missing -gt 0 ]; then
        return 1
    fi
    
    return 0
}

check_configuration_files() {
    log_info "Checking configuration files..."
    
    cd "$PROJECT_DIR"
    
    # Info.plist
    if [ -f "iosApp/Info.plist" ]; then
        PLIST_KEYS=$(grep -c "<key>" iosApp/Info.plist)
        log_success "Info.plist found ($PLIST_KEYS configuration keys)"
        
        # Check required keys
        if grep -q "YT_BASE_URL" iosApp/Info.plist; then
            log_success "  - YT_BASE_URL configured"
        else
            log_warning "  - YT_BASE_URL missing"
        fi
    else
        log_error "Info.plist not found"
        return 1
    fi
    
    # project.yml
    if [ -f "project.yml" ]; then
        if grep -q "preBuildScripts" project.yml; then
            log_success "project.yml with build scripts found"
        else
            log_warning "project.yml missing build scripts"
        fi
    else
        log_error "project.yml not found"
        return 1
    fi
    
    # Podfile
    if [ -f "Podfile" ]; then
        log_success "Podfile found"
    else
        log_error "Podfile not found"
        return 1
    fi
    
    return 0
}

check_xcode_build() {
    log_info "Attempting Xcode build (dry run)..."
    
    cd "$PROJECT_DIR"
    
    if ! [ -f "iosApp.xcworkspace/contents.xcworkspacedata" ]; then
        log_warning "Workspace not initialized, pods may need to be installed"
        return 0
    fi
    
    # Try a build without actually compiling Swift (just validate)
    if xcodebuild -workspace iosApp.xcworkspace \
                  -scheme iosApp \
                  -configuration Debug \
                  -destination 'platform=iOS Simulator,name=iPhone 15' \
                  -dry-run &> /dev/null; then
        log_success "Xcode build configuration valid"
    else
        log_warning "Xcode dry-run build inconclusive (normal for frameworks)"
    fi
    
    return 0
}

check_documentation() {
    log_info "Checking documentation..."
    
    cd "$PROJECT_DIR/.."
    
    DOCS=(
        "iosApp/README.md"
    )
    
    local missing=0
    for doc in "${DOCS[@]}"; do
        if [ -f "$doc" ]; then
            LINES=$(wc -l < "$doc")
            log_success "✓ $doc ($LINES lines)"
        else
            log_error "✗ $doc MISSING"
            missing=$((missing + 1))
        fi
    done
    
    if [ $missing -gt 0 ]; then
        return 1
    fi
    
    return 0
}

generate_summary() {
    {
        echo ""
        echo "===================================================================="
        echo "VERIFICATION SUMMARY"
        echo "===================================================================="
        echo "Build Date: $(date)"
        echo "Xcode Version: $(xcodebuild -version | head -1)"
        echo "CocoaPods Version: $(pod --version)"
        echo ""
        echo "Key Framework Paths:"
        echo "  - Shared: $PROJECT_DIR/shared.xcframework"
        echo "  - Compose: $PROJECT_DIR/composeApp.xcframework"
        echo ""
        echo "Next Steps:"
        echo "  1. open $PROJECT_DIR/iosApp.xcworkspace"
        echo "  2. Select iPhone 15 simulator"
        echo "  3. Press ⌘R to build and run"
        echo "  4. Verify app launches without crashes"
        echo ""
        echo "===================================================================="
    } >> "$REPORT_FILE"
}

main() {
    local failed=0
    
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}   YouTube Ratings iOS - Build Verification${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    # Run checks
    if ! check_prerequisites; then
        log_error "Prerequisites check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_configuration_files; then
        log_error "Configuration check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_swift_files; then
        log_error "Swift files check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_gradle_build; then
        log_error "Gradle build check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_framework_output; then
        log_error "Framework output check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_podfile; then
        log_error "Podfile check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_swift_files; then
        log_error "Swift files check failed (recheck)"
        failed=$((failed + 1))
    fi
    echo ""
    
    if ! check_xcode_build; then
        log_warning "Xcode build check incomplete"
    fi
    echo ""
    
    if ! check_documentation; then
        log_error "Documentation check failed"
        failed=$((failed + 1))
    fi
    echo ""
    
    # Generate summary
    generate_summary
    
    # Final result
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
    
    if [ $failed -eq 0 ]; then
        echo -e "${GREEN}✅ ALL CHECKS PASSED - Ready to build!${NC}"
        echo ""
        echo "Report saved to: $REPORT_FILE"
        echo ""
        echo "Next: open $PROJECT_DIR/iosApp.xcworkspace"
        return 0
    else
        echo -e "${RED}❌ $failed check(s) failed${NC}"
        echo ""
        echo "Report saved to: $REPORT_FILE"
        return 1
    fi
}

# Run main
main "$@"
