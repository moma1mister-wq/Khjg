package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LectureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getLecturesForDay(dayOfWeek: Int): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures")
    suspend fun getAllLecturesSync(): List<LectureEntity>

    @Query("SELECT COUNT(*) FROM lectures")
    suspend fun countLectures(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureEntity>)

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteLectureById(id: Int)

    @Query("DELETE FROM lectures")
    suspend fun deleteAllLectures()
}
