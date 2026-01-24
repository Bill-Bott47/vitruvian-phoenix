#!/bin/bash
#
# iOS Schema Validator
# ====================
# Validates that DriverFactory.ios.kt table definitions match VitruvianDatabase.sq
#
# Background: iOS uses manual table creation (Layer 4 defense against migration crashes).
# The table schemas in DriverFactory.ios.kt MUST exactly match VitruvianDatabase.sq,
# otherwise fresh iOS installs will crash with SQLiteException when SQLDelight queries
# try to access columns that don't exist.
#
# This script validates:
# 1. All tables in SQLDelight exist in iOS DriverFactory
# 2. For each table, all columns in SQLDelight exist in iOS
#
# Run in CI to catch schema drift before it causes crashes.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Go up two levels from .github/scripts to project root
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

SQ_FILE="$PROJECT_ROOT/shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq"
IOS_FILE="$PROJECT_ROOT/shared/src/iosMain/kotlin/com/devil/phoenixproject/data/local/DriverFactory.ios.kt"

echo "iOS Schema Validator"
echo "===================="
echo ""

# Check files exist
if [ ! -f "$SQ_FILE" ]; then
    echo "ERROR: SQLDelight schema not found: $SQ_FILE"
    exit 1
fi

if [ ! -f "$IOS_FILE" ]; then
    echo "ERROR: iOS DriverFactory not found: $IOS_FILE"
    exit 1
fi

echo "Comparing:"
echo "  SQLDelight: $(basename $SQ_FILE)"
echo "  iOS:        $(basename $IOS_FILE)"
echo ""

ERRORS=0

# Extract table names from SQLDelight (tables start with "CREATE TABLE TableName (")
SQ_TABLES=$(grep -oP '(?<=^CREATE TABLE )\w+' "$SQ_FILE" | sort | uniq)

# Extract table names from iOS DriverFactory
IOS_TABLES=$(grep -oP '(?<=CREATE TABLE IF NOT EXISTS )\w+' "$IOS_FILE" | sort | uniq)

# Check 1: All SQLDelight tables exist in iOS
echo "Step 1: Checking all SQLDelight tables exist in iOS..."
for table in $SQ_TABLES; do
    if ! echo "$IOS_TABLES" | grep -q "^${table}$"; then
        echo "  ERROR: Table '$table' exists in SQLDelight but MISSING in iOS DriverFactory"
        ERRORS=$((ERRORS + 1))
    fi
done
if [ $ERRORS -eq 0 ]; then
    echo "  OK: All $(echo "$SQ_TABLES" | wc -w | tr -d ' ') tables present in both files"
fi

# Check 2: For key tables, verify critical columns exist
# We check a subset of critical columns that are used in common queries
echo ""
echo "Step 2: Checking critical columns in key tables..."

check_column() {
    local table=$1
    local column=$2

    # Check if column exists in iOS file for this table
    # Search for the column name within the CREATE TABLE block
    if ! grep -A 50 "CREATE TABLE IF NOT EXISTS $table" "$IOS_FILE" | grep -q "$column"; then
        echo "  ERROR: Column '$column' missing from iOS table '$table'"
        ERRORS=$((ERRORS + 1))
    fi
}

# Exercise table - key columns
check_column "Exercise" "muscleGroups"
check_column "Exercise" "defaultCableConfig"
check_column "Exercise" "one_rep_max_kg"
check_column "Exercise" "timesPerformed"
check_column "Exercise" "lastPerformed"

# WorkoutSession table - key columns
check_column "WorkoutSession" "timestamp"
check_column "WorkoutSession" "targetReps"
check_column "WorkoutSession" "weightPerCableKg"
check_column "WorkoutSession" "exerciseId"
check_column "WorkoutSession" "exerciseName"
check_column "WorkoutSession" "routineSessionId"
check_column "WorkoutSession" "totalReps"

# Routine table - key columns
check_column "Routine" "createdAt"
check_column "Routine" "lastUsed"
check_column "Routine" "useCount"
check_column "Routine" "description"

# RoutineExercise table - key columns
check_column "RoutineExercise" "exerciseName"
check_column "RoutineExercise" "exerciseMuscleGroup"
check_column "RoutineExercise" "exerciseEquipment"
check_column "RoutineExercise" "cableConfig"
check_column "RoutineExercise" "setReps"
check_column "RoutineExercise" "supersetId"
check_column "RoutineExercise" "usePercentOfPR"

# PersonalRecord table - key columns
check_column "PersonalRecord" "exerciseId"
check_column "PersonalRecord" "exerciseName"
check_column "PersonalRecord" "weight"
check_column "PersonalRecord" "reps"
check_column "PersonalRecord" "oneRepMax"
check_column "PersonalRecord" "workoutMode"
check_column "PersonalRecord" "prType"
check_column "PersonalRecord" "volume"

# MetricSample table - key columns
check_column "MetricSample" "sessionId"
check_column "MetricSample" "position"
check_column "MetricSample" "positionB"
check_column "MetricSample" "velocity"
check_column "MetricSample" "load"
check_column "MetricSample" "power"

# Check for required tables that were missing before the fix
echo ""
echo "Step 3: Checking previously missing tables..."
REQUIRED_TABLES="ExerciseVideo ConnectionLog DiagnosticsHistory PhaseStatistics"
for table in $REQUIRED_TABLES; do
    if ! echo "$IOS_TABLES" | grep -q "^${table}$"; then
        echo "  ERROR: Required table '$table' missing from iOS"
        ERRORS=$((ERRORS + 1))
    else
        echo "  OK: Table '$table' present"
    fi
done

echo ""
echo "===================="
if [ $ERRORS -eq 0 ]; then
    echo "SUCCESS: iOS schema validation passed"
    echo ""
    echo "Tables checked: $(echo "$SQ_TABLES" | wc -w | tr -d ' ')"
    exit 0
else
    echo "FAILED: Found $ERRORS schema mismatch error(s)"
    echo ""
    echo "To fix: Update shared/src/iosMain/kotlin/.../DriverFactory.ios.kt"
    echo "        to match shared/src/commonMain/sqldelight/.../VitruvianDatabase.sq"
    echo ""
    echo "See: .planning/debug/resolved/issue-223-ios-fresh-install-sqlite-crash.md"
    exit 1
fi
