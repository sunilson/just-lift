package at.sunilson.justlift

import at.sunilson.justlift.features.workout.data.VitruvianDeviceManager
import at.sunilson.justlift.features.workout.data.database.ExerciseEntity
import at.sunilson.justlift.features.workout.data.normalized
import at.sunilson.justlift.features.workout.presentation.history.averageWith
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class NormalizationTest {

    @Test
    fun `test workout state normalization - left active stays left`() {
        val state = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsed = 60.seconds,
            averageUpwardForceLeft = 20.0,
            averageUpwardForceRight = 0.0,
            minPositionLeft = 0.1,
            maxPositionLeft = 0.8,
            minPositionRight = 0.0,
            maxPositionRight = 0.02
        )

        val normalized = state.normalized()
        assertEquals(20.0, normalized.averageUpwardForceLeft, 0.01)
        assertEquals(0.0, normalized.averageUpwardForceRight, 0.01)
        assertEquals(0.1, normalized.minPositionLeft, 0.01)
        assertEquals(0.8, normalized.maxPositionLeft, 0.01)
    }

    @Test
    fun `test workout state normalization - right active moves to left`() {
        val state = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsed = 60.seconds,
            averageUpwardForceLeft = 0.0,
            averageUpwardForceRight = 20.0,
            minPositionLeft = 0.0,
            maxPositionLeft = 0.02,
            minPositionRight = 0.1,
            maxPositionRight = 0.8
        )

        val normalized = state.normalized()
        assertEquals(20.0, normalized.averageUpwardForceLeft, 0.01)
        assertEquals(0.0, normalized.averageUpwardForceRight, 0.01)
        assertEquals(0.1, normalized.minPositionLeft, 0.01)
        assertEquals(0.8, normalized.maxPositionLeft, 0.01)
        assertEquals(0.0, normalized.minPositionRight, 0.01)
        assertEquals(0.02, normalized.maxPositionRight, 0.01)
    }

    @Test
    fun `test exercise entity normalization - right active moves to left`() {
        val entity = ExerciseEntity(
            userId = 1,
            name = "Test",
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsedMillis = 60000,
            averageUpwardForce = 20.0,
            averageDownwardForce = 15.0,
            maxUpwardForce = 25.0,
            maxDownwardForce = 20.0,
            averageUpwardForceLeft = 0.0,
            averageUpwardForceRight = 20.0,
            minPositionLeft = 0.0,
            maxPositionLeft = 0.02,
            minPositionRight = 0.1,
            maxPositionRight = 0.8
        )

        val normalized = entity.normalized()
        assertEquals(20.0, normalized.averageUpwardForceLeft, 0.01)
        assertEquals(0.0, normalized.averageUpwardForceRight, 0.01)
        assertEquals(0.1, normalized.minPositionLeft, 0.01)
        assertEquals(0.8, normalized.maxPositionLeft, 0.01)
    }

    @Test
    fun `test side-agnostic averaging`() {
        // Stored exercise was done on the Left
        val entity = ExerciseEntity(
            userId = 1,
            name = "Test",
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsedMillis = 60000,
            averageUpwardForce = 20.0,
            averageDownwardForce = 15.0,
            maxUpwardForce = 25.0,
            maxDownwardForce = 20.0,
            averageUpwardForceLeft = 20.0,
            averageUpwardForceRight = 0.0,
            minPositionLeft = 0.1,
            maxPositionLeft = 0.8,
            minPositionRight = 0.0,
            maxPositionRight = 0.0
        )

        // New workout done on the Right
        val state = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsed = 60.seconds,
            averageUpwardForce = 20.0,
            averageDownwardForce = 15.0,
            maxUpwardForce = 25.0,
            maxDownwardForce = 20.0,
            averageUpwardForceLeft = 0.0,
            averageUpwardForceRight = 20.0,
            minPositionLeft = 0.0,
            maxPositionLeft = 0.0,
            minPositionRight = 0.1,
            maxPositionRight = 0.8
        )

        val averaged = entity.averageWith(state)
        
        // After averaging, it should still look like it's on the Left (because of normalization)
        assertEquals(20.0, averaged.averageUpwardForceLeft, 0.01)
        assertEquals(0.0, averaged.averageUpwardForceRight, 0.01)
        assertEquals(0.1, averaged.minPositionLeft, 0.01)
        assertEquals(0.8, averaged.maxPositionLeft, 0.01)
    }

    @Test
    fun `test double cable exercise not normalized`() {
        val state = VitruvianDeviceManager.WorkoutState(
            calibratingRepsCompleted = 3,
            maxReps = null,
            upwardRepetitionsCompleted = 10,
            downwardRepetitionsCompleted = 10,
            timeElapsed = 60.seconds,
            averageUpwardForceLeft = 10.0,
            averageUpwardForceRight = 10.0,
            minPositionLeft = 0.1,
            maxPositionLeft = 0.8,
            minPositionRight = 0.1,
            maxPositionRight = 0.8
        )

        val normalized = state.normalized()
        assertEquals(10.0, normalized.averageUpwardForceLeft, 0.01)
        assertEquals(10.0, normalized.averageUpwardForceRight, 0.01)
        assertEquals(0.1, normalized.minPositionLeft, 0.01)
        assertEquals(0.8, normalized.maxPositionLeft, 0.01)
        assertEquals(0.1, normalized.minPositionRight, 0.01)
        assertEquals(0.8, normalized.maxPositionRight, 0.01)
    }
}
