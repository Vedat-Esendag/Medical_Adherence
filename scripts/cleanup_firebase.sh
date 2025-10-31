#!/bin/bash
#
# Firebase Firestore Cleanup Script
# Medical Adherence App
#
# This script helps clean up test users and orphaned data from Firebase Firestore.
# It uses Firebase CLI to query and delete documents matching test patterns.
#
# Prerequisites:
#   - Firebase CLI installed: npm install -g firebase-tools
#   - Logged into Firebase: firebase login
#   - Project selected: firebase use <project-id>
#
# Usage:
#   ./scripts/cleanup_firebase.sh
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script banner
echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                                                              ║"
echo "║     Firebase Firestore Cleanup Script                       ║"
echo "║     Medical Adherence App                                    ║"
echo "║                                                              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}Error: Firebase CLI is not installed.${NC}"
    echo ""
    echo "Install it with:"
    echo "  npm install -g firebase-tools"
    echo ""
    echo "Then login:"
    echo "  firebase login"
    echo ""
    exit 1
fi

# Check if user is logged in
if ! firebase projects:list &> /dev/null; then
    echo -e "${RED}Error: Not logged into Firebase.${NC}"
    echo ""
    echo "Login with:"
    echo "  firebase login"
    echo ""
    exit 1
fi

# Get current project
CURRENT_PROJECT=$(firebase use | grep "Active Project" | awk '{print $3}')

if [ -z "$CURRENT_PROJECT" ]; then
    echo -e "${RED}Error: No Firebase project selected.${NC}"
    echo ""
    echo "Select a project with:"
    echo "  firebase use <project-id>"
    echo ""
    echo "Available projects:"
    firebase projects:list
    echo ""
    exit 1
fi

echo -e "${GREEN}Current Firebase Project: ${CURRENT_PROJECT}${NC}"
echo ""

# Safety confirmation
echo -e "${YELLOW}⚠️  WARNING: This script will delete data from Firebase Firestore.${NC}"
echo ""
echo "This script will delete:"
echo "  • All user documents matching 'offline_user_*' pattern"
echo "  • Orphaned caregiver links (if confirmed)"
echo ""
echo -e "${YELLOW}Make sure you're using the correct project!${NC}"
echo ""
read -p "Do you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo -e "${YELLOW}Cleanup cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}Starting cleanup...${NC}"
echo ""

# ============================================================================
# CLEANUP FUNCTIONS
# ============================================================================

# Function to delete offline users
cleanup_offline_users() {
    echo -e "${BLUE}Searching for offline users...${NC}"
    
    # Note: Firebase CLI doesn't have built-in query support for pattern matching
    # We need to use the Firebase Admin SDK or REST API for complex queries
    # For now, we'll provide manual instructions
    
    echo ""
    echo -e "${YELLOW}Automated deletion of offline users requires Firebase Admin SDK.${NC}"
    echo ""
    echo "To delete offline users manually:"
    echo ""
    echo "1. Go to Firebase Console: https://console.firebase.google.com/"
    echo "2. Select project: ${CURRENT_PROJECT}"
    echo "3. Navigate to: Firestore Database → users collection"
    echo "4. Delete documents with IDs matching:"
    echo "   - offline_user_local"
    echo "   - offline_user_*"
    echo ""
    echo "Alternatively, use this Node.js script:"
    echo ""
    echo -e "${GREEN}==================== Node.js Cleanup Script ====================${NC}"
    cat << 'EOF'
// cleanup.js - Run with: node cleanup.js
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function deleteOfflineUsers() {
  const usersRef = db.collection('users');
  const snapshot = await usersRef.get();
  
  let deletedCount = 0;
  
  for (const doc of snapshot.docs) {
    if (doc.id.startsWith('offline_user_')) {
      console.log(`Deleting: ${doc.id}`);
      await doc.ref.delete();
      deletedCount++;
    }
  }
  
  console.log(`✅ Deleted ${deletedCount} offline user(s)`);
}

deleteOfflineUsers().catch(console.error);
EOF
    echo -e "${GREEN}================================================================${NC}"
    echo ""
}

# Function to delete all users (DANGER!)
cleanup_all_users() {
    echo -e "${RED}⚠️  DANGER: This will delete ALL users from Firestore!${NC}"
    echo ""
    read -p "Are you ABSOLUTELY SURE? Type 'DELETE ALL' to confirm: " confirm_all
    
    if [ "$confirm_all" != "DELETE ALL" ]; then
        echo -e "${YELLOW}Skipped deleting all users.${NC}"
        return
    fi
    
    echo ""
    echo -e "${RED}Deleting all users collection...${NC}"
    
    # Use Firebase CLI to delete recursively
    firebase firestore:delete users --recursive --yes --project "$CURRENT_PROJECT"
    
    echo -e "${GREEN}✅ All users deleted.${NC}"
}

# Function to clean up caregiver links
cleanup_caregiver_links() {
    echo -e "${BLUE}Caregiver Links Cleanup${NC}"
    echo ""
    echo "Do you want to delete caregiver links?"
    echo ""
    read -p "Delete caregiver links? (yes/no): " confirm_links
    
    if [ "$confirm_links" != "yes" ]; then
        echo -e "${YELLOW}Skipped caregiver links cleanup.${NC}"
        return
    fi
    
    echo ""
    echo -e "${RED}Deleting all caregiver links...${NC}"
    
    firebase firestore:delete caregiver_links --recursive --yes --project "$CURRENT_PROJECT"
    
    echo -e "${GREEN}✅ Caregiver links deleted.${NC}"
}

# ============================================================================
# MAIN MENU
# ============================================================================

show_menu() {
    echo ""
    echo -e "${BLUE}What would you like to clean up?${NC}"
    echo ""
    echo "1) Delete offline users only (offline_user_*)"
    echo "2) Delete ALL users (DANGER!)"
    echo "3) Delete caregiver links"
    echo "4) Delete all data (users + caregiver links)"
    echo "5) Exit"
    echo ""
    read -p "Select an option (1-5): " option
    
    case $option in
        1)
            cleanup_offline_users
            ;;
        2)
            cleanup_all_users
            ;;
        3)
            cleanup_caregiver_links
            ;;
        4)
            cleanup_all_users
            cleanup_caregiver_links
            ;;
        5)
            echo -e "${GREEN}Exiting cleanup script.${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option.${NC}"
            show_menu
            ;;
    esac
}

# Show the menu
show_menu

# ============================================================================
# POST-CLEANUP INSTRUCTIONS
# ============================================================================

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  Cleanup Complete!                                           ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Next steps:"
echo ""
echo "1. On your Android device/emulator:"
echo "   Settings → Apps → Medical Adherence → Clear Data"
echo ""
echo "2. Restart the Medical Adherence app"
echo ""
echo "3. You should see the ProfileSelectionScreen on launch"
echo ""
echo -e "${BLUE}For more information, see: docs/FIREBASE_CLEANUP.md${NC}"
echo ""

exit 0

