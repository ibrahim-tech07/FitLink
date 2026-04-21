package com.example.fitlinktrainer.data.templates

import com.example.fitlinktrainer.data.model.Exercise

object WorkoutTemplates {

    // ======================================
    // WARMUP TEMPLATES (5–7 MIN)
    // ======================================

    val warmupDynamic = listOf(
        Exercise(
            name = "Arm Circles",
            sets = 2,
            reps = 20,
            durationSeconds = 120,
            restSeconds = 15,
            caloriesBurn = 15
        ),
        Exercise(
            name = "Leg Swings",
            sets = 2,
            reps = 20,
            durationSeconds = 120,
            restSeconds = 15,
            caloriesBurn = 15
        ),
        Exercise(
            name = "Torso Twists",
            sets = 2,
            reps = 20,
            durationSeconds = 120,
            restSeconds = 15,
            caloriesBurn = 15
        ),
        Exercise(
            name = "High Knees",
            sets = 2,
            reps = 30,
            durationSeconds = 120,
            restSeconds = 15,
            caloriesBurn = 20
        ),
        Exercise(
            name = "Butt Kicks",
            sets = 2,
            reps = 30,
            durationSeconds = 120,
            restSeconds = 15,
            caloriesBurn = 20
        )
    )

    val warmupMobility = listOf(
        Exercise(
            name = "Cat-Cow Stretch",
            sets = 2,
            reps = 10,
            durationSeconds = 120,
            caloriesBurn = 10
        ),
        Exercise(
            name = "Hip Circles",
            sets = 2,
            reps = 10,
            durationSeconds = 120,
            caloriesBurn = 10
        ),
        Exercise(
            name = "Ankle Rotations",
            sets = 2,
            reps = 10,
            durationSeconds = 120,
            caloriesBurn = 10
        ),
        Exercise(
            name = "Wrist Circles",
            sets = 2,
            reps = 10,
            durationSeconds = 120,
            caloriesBurn = 10
        ),
        Exercise(
            name = "Neck Rolls",
            sets = 2,
            reps = 5,
            durationSeconds = 120,
            caloriesBurn = 8
        )
    )

    // ======================================
    // BEGINNER FULL BODY (~25 MIN)
    // ======================================

    val beginnerFullBody = listOf(
        Exercise(
            name = "Jump Rope Warmup",
            sets = 1,
            reps = 1,
            durationSeconds = 300,
            restSeconds = 30,
            caloriesBurn = 40
        ),
        Exercise(
            name = "Push Ups",
            sets = 4,
            reps = 12,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 80
        ),
        Exercise(
            name = "Bodyweight Squats",
            sets = 4,
            reps = 15,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 70
        ),
        Exercise(
            name = "Glute Bridges",
            sets = 3,
            reps = 15,
            durationSeconds = 210,
            restSeconds = 45,
            caloriesBurn = 50
        ),
        Exercise(
            name = "Plank Hold",
            sets = 3,
            reps = 1,
            durationSeconds = 180,
            restSeconds = 45,
            caloriesBurn = 45
        ),
        Exercise(
            name = "Walking Cooldown",
            sets = 1,
            reps = 1,
            durationSeconds = 300,
            caloriesBurn = 25
        )
    )

    // ======================================
    // FAT BURN CARDIO (~25 MIN)
    // ======================================

    val fatBurnCardio = listOf(
        Exercise(
            name = "Jog Warmup",
            sets = 1,
            reps = 1,
            durationSeconds = 300,
            restSeconds = 30,
            caloriesBurn = 50
        ),
        Exercise(
            name = "Jumping Jacks",
            sets = 4,
            reps = 40,
            durationSeconds = 240,
            restSeconds = 30,
            caloriesBurn = 100
        ),
        Exercise(
            name = "Burpees",
            sets = 4,
            reps = 20,
            durationSeconds = 240,
            restSeconds = 45,
            caloriesBurn = 120
        ),
        Exercise(
            name = "Mountain Climbers",
            sets = 4,
            reps = 50,
            durationSeconds = 240,
            restSeconds = 45,
            caloriesBurn = 110
        ),
        Exercise(
            name = "High Knees",
            sets = 3,
            reps = 40,
            durationSeconds = 210,
            restSeconds = 30,
            caloriesBurn = 90
        ),
        Exercise(
            name = "Stretch Cooldown",
            sets = 1,
            reps = 1,
            durationSeconds = 300,
            caloriesBurn = 20
        )
    )

    // ======================================
    // STRENGTH WORKOUT (~30 MIN)
    // ======================================

    val strengthWorkout = listOf(
        Exercise(
            name = "Barbell Bench Press",
            sets = 5,
            reps = 8,
            durationSeconds = 300,
            restSeconds = 120,
            caloriesBurn = 70
        ),
        Exercise(
            name = "Barbell Squats",
            sets = 5,
            reps = 8,
            durationSeconds = 360,
            restSeconds = 120,
            caloriesBurn = 90
        ),
        Exercise(
            name = "Deadlift",
            sets = 4,
            reps = 6,
            durationSeconds = 300,
            restSeconds = 150,
            caloriesBurn = 80
        ),
        Exercise(
            name = "Pull Ups",
            sets = 4,
            reps = 10,
            durationSeconds = 240,
            restSeconds = 90,
            caloriesBurn = 70
        ),
        Exercise(
            name = "Dumbbell Shoulder Press",
            sets = 4,
            reps = 12,
            durationSeconds = 240,
            restSeconds = 90,
            caloriesBurn = 60
        ),
        Exercise(
            name = "Plank Core Hold",
            sets = 3,
            reps = 1,
            durationSeconds = 180,
            caloriesBurn = 40
        )
    )

    // ======================================
    // HIIT ADVANCED (~22–24 MIN)
    // ======================================

    val hiitAdvanced = listOf(
        Exercise(
            name = "Sprint Intervals",
            sets = 6,
            reps = 1,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 150
        ),
        Exercise(
            name = "Burpee Jump Combo",
            sets = 5,
            reps = 15,
            durationSeconds = 240,
            restSeconds = 45,
            caloriesBurn = 140
        ),
        Exercise(
            name = "Box Jumps",
            sets = 4,
            reps = 20,
            durationSeconds = 240,
            restSeconds = 45,
            caloriesBurn = 120
        ),
        Exercise(
            name = "Battle Rope Waves",
            sets = 4,
            reps = 40,
            durationSeconds = 240,
            restSeconds = 45,
            caloriesBurn = 130
        ),
        Exercise(
            name = "Jump Squats",
            sets = 4,
            reps = 20,
            durationSeconds = 210,
            restSeconds = 45,
            caloriesBurn = 110
        )
    )

    // ======================================
    // HOME WORKOUT (~20 MIN)
    // ======================================

    val homeWorkout = listOf(
        Exercise(
            name = "Jump Rope",
            sets = 1,
            reps = 1,
            durationSeconds = 300,
            caloriesBurn = 50
        ),
        Exercise(
            name = "Push Ups",
            sets = 4,
            reps = 15,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 80
        ),
        Exercise(
            name = "Chair Squats",
            sets = 4,
            reps = 20,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 70
        ),
        Exercise(
            name = "Glute Bridges",
            sets = 4,
            reps = 20,
            durationSeconds = 240,
            restSeconds = 60,
            caloriesBurn = 60
        ),
        Exercise(
            name = "Plank",
            sets = 3,
            reps = 1,
            durationSeconds = 180,
            caloriesBurn = 45
        )
    )

    // ======================================
    // COOLDOWN (~5 MIN)
    // ======================================

    val cooldownStretch = listOf(
        Exercise(
            name = "Quad Stretch",
            sets = 1,
            reps = 1,
            durationSeconds = 60,
            caloriesBurn = 5
        ),
        Exercise(
            name = "Hamstring Stretch",
            sets = 1,
            reps = 1,
            durationSeconds = 60,
            caloriesBurn = 5
        ),
        Exercise(
            name = "Chest Stretch",
            sets = 1,
            reps = 1,
            durationSeconds = 60,
            caloriesBurn = 5
        ),
        Exercise(
            name = "Triceps Stretch",
            sets = 1,
            reps = 1,
            durationSeconds = 60,
            caloriesBurn = 5
        ),
        Exercise(
            name = "Child's Pose",
            sets = 1,
            reps = 1,
            durationSeconds = 90,
            caloriesBurn = 5
        )
    )

    val cooldownBreathing = listOf(
        Exercise(
            name = "Deep Belly Breathing",
            sets = 1,
            reps = 1,
            durationSeconds = 120,
            caloriesBurn = 3
        ),
        Exercise(
            name = "Alternate Nostril Breathing",
            sets = 1,
            reps = 5,
            durationSeconds = 90,
            caloriesBurn = 3
        ),
        Exercise(
            name = "Box Breathing",
            sets = 1,
            reps = 5,
            durationSeconds = 120,
            caloriesBurn = 3
        )
    )
}