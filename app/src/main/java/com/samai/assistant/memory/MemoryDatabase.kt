package com.samai.assistant.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: MemoryCategory = MemoryCategory.GENERAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

enum class MemoryCategory {
    GENERAL, PREFERENCE, NOTE, COMMAND, CONTEXT, TASK
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntry>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Int): MemoryEntry?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryEntry>>

    @Query("SELECT * FROM memories WHERE title LIKE :query OR content LIKE :query ORDER BY updatedAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntry>>

    @Insert
    suspend fun addMemory(memory: MemoryEntry): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntry)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntry)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}

@Database(entities = [MemoryEntry::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
