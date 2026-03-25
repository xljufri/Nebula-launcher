package com.nebula.launcher.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nebula.launcher.core.model.AppNode

@Dao
interface AppNodeDao {
    @Query("SELECT * FROM app_nodes")
    suspend fun getAllNodes(): List<AppNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<AppNode>)

    @Query("DELETE FROM app_nodes")
    suspend fun deleteAll()
}
