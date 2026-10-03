package com.example.lifeorganizer.waypoints.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Junction
import androidx.room.Relation

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val notes: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val isPinned: Boolean = false,
    val lastUsed: Long = 0L,
    val lastEdited: Long = 0L
)

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String
)

@Entity(
    tableName = "waypoint_label_cross_ref",
    primaryKeys = ["waypointId", "labelId"]
)
data class WaypointLabelCrossRef(
    val waypointId: Long,
    val labelId: Long
)

data class WaypointWithLabels(
    @androidx.room.Embedded val waypoint: WaypointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WaypointLabelCrossRef::class,
            parentColumn = "waypointId",
            entityColumn = "labelId"
        )
    )
    val labels: List<LabelEntity>
)
