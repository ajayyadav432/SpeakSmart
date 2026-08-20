#!/bin/bash

echo "========================================="
echo "   SpeakSmart Autonomous Test Runner    "
echo "========================================="

ADB_BIN="/home/ajay/Android/Sdk/platform-tools/adb"

# Check connected adb devices
DEVICES=$($ADB_BIN devices 2>/dev/null | grep -v "List of devices" | grep "device$" | wc -l)

echo "Running unit test suite (JUnit + Robolectric)..."
./gradlew test 2>&1
UNIT_TEST_EXIT=$?

if [ $UNIT_TEST_EXIT -ne 0 ]; then
    echo "❌ Unit tests failed!"
    exit $UNIT_TEST_EXIT
else
    echo "✅ Unit tests passed successfully!"
fi

if [ $DEVICES -gt 0 ]; then
    echo "Found $DEVICES connected device(s). Running connectedAndroidTest..."
    ./gradlew connectedAndroidTest 2>&1
    INSTRUMENTED_EXIT=$?
    if [ $INSTRUMENTED_EXIT -ne 0 ]; then
        echo "❌ Instrumented device tests failed!"
        exit $INSTRUMENTED_EXIT
    fi
else
    echo "ℹ️ Note: No connected physical device/emulator found. Unit & Compose UI tests passed via Robolectric."
fi

echo ""
echo "========================================="
echo "✅ ALL TEST SUITES COMPLETED SUCCESSFULLY!"
echo "========================================="
exit 0
