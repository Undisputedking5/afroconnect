#!/usr/bin/env bash
# End-to-end walkthrough on the CI emulator against local Firebase emulators.
set -uo pipefail
UI="python3 .github/scripts/ui.py"
PKG=com.example.africonnect
mkdir -p shots
STAMP=$(date +%s)
CLIENT="client$STAMP@test.dev"
FREEL="freelancer$STAMP@test.dev"
PASS="secret123"

finish() {
  code=$?
  $UI shot zz_final || true
  adb logcat -d > shots/logcat.txt || true
  echo "---- firebase db log ----"
  grep -E "RepoManager|Connection|WebSocket|ws://|wss://|PersistentConnection" shots/logcat.txt | grep -v BoundBroker | cut -c1-300 | head -25 || true
  echo "---- app log (errors/warnings) ----"
  PID=$(adb shell pidof $PKG | tr -d '\r')
  awk -v p="$PID" '($3==p && ($5=="E" || $5=="W")) || /FATAL|AndroidRuntime/' shots/logcat.txt | grep -viE "EGL|gralloc|OpenGL|HWUI|ashmem|chatty" | tail -60 || true
  if grep -q "FATAL EXCEPTION" shots/logcat.txt; then
    echo "::error::App crashed"; grep -A40 "FATAL EXCEPTION" shots/logcat.txt | head -80
    exit 1
  fi
  exit $code
}
trap finish EXIT
set -e

adb install -r out/AfroConnect-e2e.apk
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION || true
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION || true
adb emu geo fix 36.8219 -1.2921 || true
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity
sleep 6

# ── Onboarding & auth screens ──
$UI wait "Next" 40
$UI shot 01_onboarding
$UI tap "Next"; $UI shot 02_onboarding_2
$UI tap "Skip"
$UI wait "Welcome back"
$UI shot 03_login
$UI tap "Create an account"
$UI wait "Join AfroConnect"
$UI shot 04_register

# ── Client: register & post a job ──
$UI tap "Hire talent"
$UI type "Full name" "Amina Client"
$UI type "Email" "$CLIENT"
$UI type "Password" "$PASS"
$UI type "Confirm password" "$PASS"
$UI tap "Create account"
$UI wait "Your jobs" 40
$UI shot 05_client_dashboard_empty
$UI tap "Post a job"
$UI wait "Publish job"
$UI type "Job title" "Build a delivery app"
$UI type "Description" "We need a Kotlin developer to build an Android delivery app with live tracking."
$UI type "Skills needed" "Kotlin, Firebase"
$UI type "Total budget" "50000"
$UI type "Location" "Westlands Nairobi"
$UI shot 06_post_job_form
$UI tap "Publish job"
$UI wait "Build a delivery app" 30
$UI shot 07_client_dashboard_with_job
$UI tap "Talent"; sleep 2; $UI shot 08_talent
$UI tap "Profile"; sleep 2; $UI shot 09_client_profile
$UI tap "Settings"; $UI wait "Appearance"; $UI shot 10_settings
$UI tap "Log out"; sleep 1; $UI tap "Log out"
$UI wait "Welcome back" 30

# ── Freelancer: register, discover, apply ──
$UI tap "Create an account"
$UI wait "Join AfroConnect"
$UI type "Full name" "Baraka Freelancer"
$UI type "Email" "$FREEL"
$UI type "Password" "$PASS"
$UI type "Confirm password" "$PASS"
$UI tap "Create account"
$UI wait "Browse jobs" 40
sleep 2
$UI shot 11_discover
$UI tap "Build a delivery app"
$UI wait "About this job"
$UI shot 12_job_detail
$UI tap "Apply now"
$UI wait "Submit a proposal"
$UI type "Your bid" "45000"
$UI type "Cover letter" "Hi, I have built three delivery apps with Kotlin and Firebase and can start immediately."
$UI shot 13_apply_sheet
$UI tap "Send proposal"
$UI wait "Withdraw application" 30
$UI shot 14_applied
$UI back
$UI tap "Nearby"; sleep 6; $UI shot 15_map
$UI tap "Messages"; sleep 2; $UI shot 16_messages_empty
$UI tap "Profile"; sleep 2; $UI shot 17_freelancer_profile
$UI tap "Edit profile"; $UI wait "Save profile"; $UI shot 18_edit_profile
$UI type "Professional title" "Android Developer"
$UI type "Skills" "Kotlin, Firebase, Compose"
$UI tap "Save profile"
$UI wait "Android Developer" 20
$UI shot 19_profile_saved
$UI tap "Settings"; $UI wait "Appearance"
$UI tap "Log out"; sleep 1; $UI tap "Log out"
$UI wait "Welcome back" 30

# ── Client: review proposal, hire, chat ──
$UI type "Email" "$CLIENT"
$UI type "Password" "$PASS"
$UI tap "Sign in"
$UI wait "Your jobs" 40
sleep 2
$UI shot 20_client_with_proposal
$UI tap "Notifications"; sleep 2; $UI shot 21_notifications; $UI back
$UI tap "Review"
$UI wait "Proposals"
$UI shot 22_proposals
$UI tap "Hire"; sleep 1; $UI shot 23_hire_dialog; $UI tap "Hire"
$UI wait "hired you" 30
$UI type "Message" "Welcome aboard! Let's start on Monday."
$UI tap "Send"
sleep 3
$UI shot 24_chat
$UI back
$UI tap "Messages"; sleep 2; $UI shot 25_inbox
$UI tap "Profile"; $UI tap "Settings"; $UI wait "Appearance"
$UI tap "Dark"; sleep 1; $UI shot 26_settings_dark
$UI back
$UI tap "Home"; sleep 2; $UI shot 27_dashboard_dark
echo "E2E walkthrough finished"
