package com.zdon.core.data.repository

import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.database.dao.HistoryDao
import com.zdon.core.downloader.mapper.toDomain
import com.zdon.core.model.HistoryEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [HistoryRepository]. */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : HistoryRepository {

    override fun observeHistory(): Flow<List<HistoryEntry>> =
        historyDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeHistory(query: String): Flow<List<HistoryEntry>> =
        if (query.isBlank()) {
            observeHistory()
        } else {
            historyDao.observeMatching(query.trim())
                .map { entities -> entities.map { it.toDomain() } }
        }

    override fun observeRecentUrls(limit: Int): Flow<List<String>> =
        historyDao.observeRecentUrls(limit)

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        historyDao.deleteById(id)
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        historyDao.deleteAll()
    }
}
