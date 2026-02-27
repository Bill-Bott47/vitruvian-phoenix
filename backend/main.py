"""
Phoenix App — AI Workout API
Runs on pai (192.168.1.189:8505)
Future: deploy to Render/Railway/Fly.io for public launch

⚠️  INTERNAL ONLY — DO NOT EXPOSE TO PUBLIC INTERNET ⚠️
Current auth uses X-User-Id header which is trivially spoofable.
This service must remain behind LAN/VPN until Supabase JWT validation
is implemented in V2. Firewall rule: allow port 8505 from LAN only.

Endpoints:
  GET  /workout/today          → Today's AI-generated workout plan
  POST /workout/complete        → Log a completed workout
  GET  /exercises               → Full exercise library (incl. hybrid)
  GET  /exercises/hybrid        → Non-Vitruvian exercises only
  GET  /user/{user_id}/history  → User's workout history
  GET  /health                  → Health check
"""

from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime, date
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="Phoenix Workout API",
    description="AI-powered workout generation for Vitruvian Trainer+ owners",
    version="0.1.0",
)

# CORS — allow the Android/iOS app + local dev
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Tighten to specific app origins before prod
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Auth (placeholder — swap for Supabase JWT validation) ──────────────────

def get_user_id(x_user_id: Optional[str] = Header(None)) -> str:
    """
    MVP auth: client sends X-User-Id header.
    V2: validate Supabase JWT and extract user ID from claims.
    """
    if not x_user_id:
        raise HTTPException(status_code=401, detail="X-User-Id header required")
    return x_user_id


# ── Models ─────────────────────────────────────────────────────────────────

class ExerciseType:
    VITRUVIAN  = "VITRUVIAN"
    DUMBBELL   = "DUMBBELL"
    BARBELL    = "BARBELL"
    BODYWEIGHT = "BODYWEIGHT"
    TRX        = "TRX"
    MACHINE    = "MACHINE"
    REST_TIMER = "REST_TIMER"


class PlannedSet(BaseModel):
    set_number: int
    set_type: str = "STANDARD"           # STANDARD | WARMUP | DROPSET | AMRAP
    target_reps: Optional[int] = None
    target_weight_kg: Optional[float] = None
    target_rpe: Optional[int] = None
    rest_seconds: int = 60


class PlannedExercise(BaseModel):
    order_index: int
    exercise_id: str
    exercise_name: str
    exercise_type: str = ExerciseType.VITRUVIAN
    muscle_group: str
    sets: List[PlannedSet]
    rest_seconds: int = 60
    coaching_note: Optional[str] = None  # Form cue / technique tip
    cable_config: Optional[str] = "DOUBLE"
    is_travel_substitute: bool = False   # True = AI-generated travel sub


class WorkoutPlan(BaseModel):
    plan_id: str
    user_id: str
    generated_at: datetime
    workout_name: str
    estimated_duration_minutes: int
    exercises: List[PlannedExercise]
    ai_notes: Optional[str] = None       # AI coach commentary


class CompletedSetLog(BaseModel):
    exercise_id: str
    set_number: int
    actual_reps: int
    actual_weight_kg: float
    logged_rpe: Optional[int] = None
    is_pr: bool = False


class WorkoutCompletionLog(BaseModel):
    plan_id: Optional[str] = None        # None = free workout (no plan)
    user_id: str
    started_at: datetime
    completed_at: datetime
    sets: List[CompletedSetLog]
    notes: Optional[str] = None


class ExerciseDefinition(BaseModel):
    id: str
    name: str
    exercise_type: str
    muscle_group: str
    equipment: str
    description: Optional[str] = None
    coaching_note: Optional[str] = None
    default_cable_config: Optional[str] = None  # VITRUVIAN only


# ── Exercise Library (seed data — will move to DB) ─────────────────────────

HYBRID_EXERCISE_LIBRARY: List[ExerciseDefinition] = [
    # ── Dumbbell ──────────────────────────────────────────────────────────
    ExerciseDefinition(
        id="db-chest-press", name="Dumbbell Chest Press",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="CHEST", equipment="DUMBBELL",
        coaching_note="Neutral spine, scapulae retracted. Lower until elbows at 90°."
    ),
    ExerciseDefinition(
        id="db-row", name="Dumbbell Row",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="BACK", equipment="DUMBBELL",
        coaching_note="Brace core. Drive elbow to hip, not shoulder."
    ),
    ExerciseDefinition(
        id="db-shoulder-press", name="Dumbbell Shoulder Press",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="SHOULDERS", equipment="DUMBBELL",
        coaching_note="Do not hyperextend lumbar. Press vertical, not forward."
    ),
    ExerciseDefinition(
        id="db-rdl", name="Romanian Deadlift (DB)",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="HAMSTRINGS", equipment="DUMBBELL",
        coaching_note="Hip hinge, not squat. Maintain neutral spine throughout."
    ),
    ExerciseDefinition(
        id="db-lateral-raise", name="Lateral Raise",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="SHOULDERS", equipment="DUMBBELL",
        coaching_note="Lead with elbows, not wrists. Stop at shoulder height."
    ),
    ExerciseDefinition(
        id="db-bicep-curl", name="Dumbbell Bicep Curl",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="BICEPS", equipment="DUMBBELL",
        coaching_note="Supinate at the top. Control the eccentric."
    ),
    ExerciseDefinition(
        id="db-tricep-kickback", name="Tricep Kickback",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="TRICEPS", equipment="DUMBBELL",
        coaching_note="Upper arm parallel to floor. Full extension at top."
    ),
    ExerciseDefinition(
        id="db-goblet-squat", name="Goblet Squat",
        exercise_type=ExerciseType.DUMBBELL, muscle_group="QUADS", equipment="DUMBBELL",
        coaching_note="Elbows inside knees at bottom. Drive through heels."
    ),

    # ── Bodyweight ─────────────────────────────────────────────────────────
    ExerciseDefinition(
        id="bw-pushup", name="Push-Up",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="CHEST", equipment="BODYWEIGHT",
        coaching_note="Rigid plank from head to heel. Elbows 45° from torso."
    ),
    ExerciseDefinition(
        id="bw-pullup", name="Pull-Up",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="BACK", equipment="BODYWEIGHT",
        coaching_note="Dead hang start. Drive elbows down to lats, not shoulders."
    ),
    ExerciseDefinition(
        id="bw-dip", name="Dip",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="TRICEPS", equipment="BODYWEIGHT",
        coaching_note="Slight forward lean for chest emphasis. Don't flare elbows."
    ),
    ExerciseDefinition(
        id="bw-plank", name="Plank",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="CORE", equipment="BODYWEIGHT",
        coaching_note="Neutral spine. Squeeze glutes and abs. Don't hold breath."
    ),
    ExerciseDefinition(
        id="bw-squat", name="Bodyweight Squat",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="QUADS", equipment="BODYWEIGHT",
        coaching_note="Feet shoulder-width. Knees track toes. Full depth if mobility allows."
    ),
    ExerciseDefinition(
        id="bw-lunge", name="Reverse Lunge",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="QUADS", equipment="BODYWEIGHT",
        coaching_note="Step back, not forward. Rear knee hovers 1\" above floor."
    ),
    ExerciseDefinition(
        id="bw-glute-bridge", name="Glute Bridge",
        exercise_type=ExerciseType.BODYWEIGHT, muscle_group="GLUTES", equipment="BODYWEIGHT",
        coaching_note="Drive through heels. Full hip extension at top. Pause 1 second."
    ),

    # ── TRX ────────────────────────────────────────────────────────────────
    ExerciseDefinition(
        id="trx-row", name="TRX Row",
        exercise_type=ExerciseType.TRX, muscle_group="BACK", equipment="TRX",
        coaching_note="Body angle controls difficulty. Retract scapulae before pulling."
    ),
    ExerciseDefinition(
        id="trx-chest-press", name="TRX Chest Press",
        exercise_type=ExerciseType.TRX, muscle_group="CHEST", equipment="TRX",
        coaching_note="Lean forward for more load. Keep rigid plank throughout."
    ),
    ExerciseDefinition(
        id="trx-bicep-curl", name="TRX Bicep Curl",
        exercise_type=ExerciseType.TRX, muscle_group="BICEPS", equipment="TRX",
        coaching_note="Elbows fixed, walk feet forward for more load."
    ),
    ExerciseDefinition(
        id="trx-squat", name="TRX Squat",
        exercise_type=ExerciseType.TRX, muscle_group="QUADS", equipment="TRX",
        coaching_note="Hold handles for counterbalance. Allows deeper squat."
    ),
    ExerciseDefinition(
        id="trx-plank", name="TRX Plank",
        exercise_type=ExerciseType.TRX, muscle_group="CORE", equipment="TRX",
        coaching_note="Feet in straps. Harder than floor plank — high core demand."
    ),

    # ── Rest Timer (template) ───────────────────────────────────────────────
    ExerciseDefinition(
        id="rest-60", name="Rest (60 sec)",
        exercise_type=ExerciseType.REST_TIMER, muscle_group="RECOVERY", equipment="NONE",
        coaching_note="Active recovery. Breathe. Shake out the pump."
    ),
    ExerciseDefinition(
        id="rest-90", name="Rest (90 sec)",
        exercise_type=ExerciseType.REST_TIMER, muscle_group="RECOVERY", equipment="NONE",
        coaching_note="Longer recovery for heavy compound sets."
    ),
    ExerciseDefinition(
        id="rest-120", name="Rest (2 min)",
        exercise_type=ExerciseType.REST_TIMER, muscle_group="RECOVERY", equipment="NONE",
        coaching_note="Full recovery. Used after max-effort sets."
    ),
]


# ── AI Workout Generation (MVP: rule-based progressive overload) ───────────

def generate_workout_plan(user_id: str, user_history: list = None) -> WorkoutPlan:
    """
    MVP: Returns a template upper push day.
    V2: Query user's progression history + AI model to auto-progress weights/reps.
    V3: Adapt to available equipment (home vs gym vs travel hotel).
    """
    from uuid import uuid4

    plan_id = f"plan_{uuid4().hex[:8]}"

    exercises = [
        PlannedExercise(
            order_index=0,
            exercise_id="cable-chest-press",   # Vitruvian exercise
            exercise_name="Cable Chest Press",
            exercise_type=ExerciseType.VITRUVIAN,
            muscle_group="CHEST",
            cable_config="DOUBLE",
            coaching_note="Set cable at chest height. Drive elbows together at lockout.",
            sets=[
                PlannedSet(set_number=1, set_type="WARMUP", target_reps=15, target_weight_kg=10),
                PlannedSet(set_number=2, set_type="STANDARD", target_reps=10, target_weight_kg=20),
                PlannedSet(set_number=3, set_type="STANDARD", target_reps=10, target_weight_kg=20),
                PlannedSet(set_number=4, set_type="STANDARD", target_reps=8, target_weight_kg=22.5),
            ]
        ),
        PlannedExercise(
            order_index=1,
            exercise_id="rest-90",
            exercise_name="Rest",
            exercise_type=ExerciseType.REST_TIMER,
            muscle_group="RECOVERY",
            sets=[PlannedSet(set_number=1, set_type="REST", rest_seconds=90)],
            rest_seconds=90
        ),
        PlannedExercise(
            order_index=2,
            exercise_id="cable-shoulder-press",
            exercise_name="Cable Shoulder Press",
            exercise_type=ExerciseType.VITRUVIAN,
            muscle_group="SHOULDERS",
            cable_config="DOUBLE",
            coaching_note="Press vertical. Brace core to protect lumbar.",
            sets=[
                PlannedSet(set_number=1, set_type="STANDARD", target_reps=10, target_weight_kg=15),
                PlannedSet(set_number=2, set_type="STANDARD", target_reps=10, target_weight_kg=15),
                PlannedSet(set_number=3, set_type="STANDARD", target_reps=8, target_weight_kg=17.5),
            ]
        ),
        PlannedExercise(
            order_index=3,
            exercise_id="rest-60",
            exercise_name="Rest",
            exercise_type=ExerciseType.REST_TIMER,
            muscle_group="RECOVERY",
            sets=[PlannedSet(set_number=1, set_type="REST", rest_seconds=60)],
            rest_seconds=60
        ),
        PlannedExercise(
            order_index=4,
            exercise_id="bw-pushup",
            exercise_name="Push-Up",
            exercise_type=ExerciseType.BODYWEIGHT,
            muscle_group="CHEST",
            coaching_note="Finisher — go to near failure. Control the negative.",
            sets=[
                PlannedSet(set_number=1, set_type="AMRAP", target_reps=None),
                PlannedSet(set_number=2, set_type="AMRAP", target_reps=None),
            ]
        ),
    ]

    return WorkoutPlan(
        plan_id=plan_id,
        user_id=user_id,
        generated_at=datetime.utcnow(),
        workout_name="Upper Push — Day 1",
        estimated_duration_minutes=35,
        exercises=exercises,
        ai_notes="Progressive overload target: +2.5kg on Cable Chest Press if all 4 sets completed at RPE <8."
    )


# ── Routes ─────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "service": "phoenix-workout-api", "version": "0.1.0"}


@app.get("/workout/today", response_model=WorkoutPlan)
def get_todays_workout(user_id: str = Depends(get_user_id)):
    """
    Returns AI-generated workout plan for today.
    MVP: template-based. V2: progressive overload from history. V3: full AI.
    """
    plan = generate_workout_plan(user_id=user_id)
    return plan


@app.post("/workout/complete")
def log_workout_completion(
    log: WorkoutCompletionLog,
    user_id: str = Depends(get_user_id)
):
    """
    Receives completed workout data from the app.
    Stores for progressive overload calculation and PRs.
    MVP: just acknowledges. V2: write to DB + update progression.
    """
    duration_minutes = (log.completed_at - log.started_at).seconds // 60
    total_sets = len(log.sets)
    prs = [s for s in log.sets if s.is_pr]

    return {
        "status": "logged",
        "user_id": user_id,
        "plan_id": log.plan_id,
        "sets_logged": total_sets,
        "duration_minutes": duration_minutes,
        "new_prs": len(prs),
        "message": f"Great work. {total_sets} sets logged. {'🔥 ' + str(len(prs)) + ' new PRs!' if prs else 'Keep pushing.'}"
    }


@app.get("/exercises", response_model=List[ExerciseDefinition])
def get_exercise_library():
    """
    Returns the full hybrid exercise library (Vitruvian + all alternatives).
    MVP: in-memory seed data. V2: pull from Supabase DB.
    """
    return HYBRID_EXERCISE_LIBRARY


@app.get("/exercises/hybrid", response_model=List[ExerciseDefinition])
def get_hybrid_exercises(exercise_type: Optional[str] = None):
    """
    Returns non-Vitruvian exercises only.
    Optional filter: ?exercise_type=DUMBBELL | BODYWEIGHT | TRX | BARBELL
    Used by travel mode exercise picker.
    """
    exercises = [e for e in HYBRID_EXERCISE_LIBRARY if e.exercise_type != ExerciseType.VITRUVIAN]
    if exercise_type:
        exercises = [e for e in exercises if e.exercise_type == exercise_type.upper()]
    return exercises


@app.get("/user/{user_id}/history")
def get_user_history(user_id: str, limit: int = 10):
    """
    Returns recent workout history for a user.
    MVP: stub. V2: query Supabase with user's workout logs.
    """
    return {
        "user_id": user_id,
        "workouts": [],
        "note": "History logging coming in V2 (Supabase integration)"
    }


# ── Entry point ─────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8505))
    host = os.getenv("HOST", "0.0.0.0")
    print(f"Phoenix Workout API starting on {host}:{port}")
    uvicorn.run("main:app", host=host, port=port, reload=True)
