package com.example.lifeorganizer.waypoints.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWaypoint(waypoint: WaypointEntity): Long

    @Update
    fun updateWaypoint(waypoint: WaypointEntity): Int

    @Query("UPDATE waypoints SET lastUsed = :timestamp WHERE id = :id")
    fun updateLastUsed(id: Long, timestamp: Long)

    @Delete
    fun deleteWaypoint(waypoint: WaypointEntity): Int

    @Query("SELECT * FROM waypoints ORDER BY isPinned DESC, id DESC")
    fun getAllWaypoints(): Flow<List<WaypointEntity>>

    @Transaction
    @Query("SELECT * FROM waypoints")
    fun getAllWaypointsWithLabels(): Flow<List<WaypointWithLabels>>
    
    @Transaction
    @Query("SELECT * FROM waypoints")
    fun getAllWaypointsWithLabelsSync(): List<WaypointWithLabels>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    fun getWaypointById(id: Long): WaypointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLabel(label: LabelEntity): Long

    @Query("SELECT * FROM labels")
    fun getAllLabels(): Flow<List<LabelEntity>>
    
    @Query("SELECT * FROM labels")
    fun getAllLabelsSync(): List<LabelEntity>
    
    @Delete
    fun deleteLabel(label: LabelEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertWaypointLabelCrossRef(crossRef: WaypointLabelCrossRef): Long

    @Delete
    fun deleteWaypointLabelCrossRef(crossRef: WaypointLabelCrossRef): Int
    
    @Query("DELETE FROM waypoint_label_cross_ref WHERE waypointId = :waypointId")
    fun deleteAllLabelsForWaypoint(waypointId: Long): Int
}
