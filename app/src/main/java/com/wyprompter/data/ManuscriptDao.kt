package com.wyprompter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ManuscriptDao {

    @Query("SELECT * FROM manuscripts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Manuscript>>

    @Query("SELECT * FROM manuscripts WHERE id = :id")
    suspend fun getById(id: Long): Manuscript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manuscript: Manuscript): Long

    @Update
    suspend fun update(manuscript: Manuscript)

    @Delete
    suspend fun delete(manuscript: Manuscript)
}