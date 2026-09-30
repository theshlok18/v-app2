package com.samai.assistant.memory

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao
) {
    fun getAllMemories(): Flow<List<MemoryEntry>> = dao.getAllMemories()
    fun searchMemories(query: String): Flow<List<MemoryEntry>> = dao.searchMemories(query)

    suspend fun addMemory(title: String, content: String, category: MemoryCategory = MemoryCategory.GENERAL): Long {
        return dao.addMemory(MemoryEntry(title = title, content = content, category = category))
    }

    suspend fun updateMemory(entry: MemoryEntry) {
        dao.updateMemory(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(entry: MemoryEntry) = dao.deleteMemory(entry)
    suspend fun clearAll() = dao.clearAllMemories()
    suspend fun getById(id: Int): MemoryEntry? = dao.getMemoryById(id)
}

@Singleton
class MemoryManager @Inject constructor(
    private val repository: MemoryRepository
) {
    fun getAll() = repository.getAllMemories()
    fun search(query: String) = repository.searchMemories(query)

    suspend fun remember(title: String, content: String, category: MemoryCategory = MemoryCategory.GENERAL) {
        repository.addMemory(title, content, category)
    }

    suspend fun update(id: Int, title: String, content: String) {
        repository.getById(id)?.let { existing ->
            repository.updateMemory(existing.copy(title = title, content = content))
        }
    }

    suspend fun forget(id: Int) {
        repository.getById(id)?.let { repository.deleteMemory(it) }
    }

    suspend fun forgetAll() = repository.clearAll()
}
