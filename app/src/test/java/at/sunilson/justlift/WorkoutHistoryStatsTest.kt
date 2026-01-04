package at.sunilson.justlift

import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryEntity
import at.sunilson.justlift.features.workout.data.database.WorkoutHistoryWithStats
import at.sunilson.justlift.features.workout.presentation.history.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutHistoryStatsTest {

    @Test
    fun `test score calculation`() {
        val entity = WorkoutHistoryEntity(
            upwardRepetitionsCompleted = 10,
            averageUpwardForce = 20.0,
            downwardRepetitionsCompleted = 10,
            averageDownwardForce = 15.0,
            timestampMillis = 1000L,
            difficulty = "WARMUP",
            calibratingRepsCompleted = 3,
            maxReps = null,
            timeElapsedMillis = 60000L,
            maxUpwardForce = 25.0,
            maxDownwardForce = 20.0
        )
        
        // Current volume is (10 * 20.0) + (10 * 15.0) = 350.0
        // Average of others is 175.0
        // Score should be +100%
        val stats = WorkoutHistoryWithStats(
            entity = entity,
            averageVolumeOfOthers = 175.0
        )
        
        val domain = stats.toDomain()
        assertEquals(100.0, domain.score!!, 0.01)
        
        // Average of others is 700.0
        // Current is 350.0
        // Score should be -50%
        val stats2 = WorkoutHistoryWithStats(
            entity = entity,
            averageVolumeOfOthers = 700.0
        )
        assertEquals(-50.0, stats2.toDomain().score!!, 0.01)
    }

    @Test
    fun `test score is null when no other entries`() {
        val entity = WorkoutHistoryEntity(
            upwardRepetitionsCompleted = 10,
            averageUpwardForce = 20.0,
            timestampMillis = 1000L,
            difficulty = "WARMUP",
            calibratingRepsCompleted = 3,
            maxReps = null,
            downwardRepetitionsCompleted = 10,
            timeElapsedMillis = 60000L,
            averageDownwardForce = 15.0,
            maxUpwardForce = 25.0,
            maxDownwardForce = 20.0
        )
        
        val stats = WorkoutHistoryWithStats(
            entity = entity,
            averageVolumeOfOthers = null
        )
        
        val domain = stats.toDomain()
        assertNull(domain.score)
    }
}
