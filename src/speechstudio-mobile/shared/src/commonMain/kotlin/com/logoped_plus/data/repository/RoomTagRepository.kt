package com.logoped_plus.data.repository

import com.logoped_plus.data.local.TagDao
import com.logoped_plus.data.local.toDomain
import com.logoped_plus.domain.model.Tag
import com.logoped_plus.domain.repository.TagLoadState
import com.logoped_plus.domain.repository.TagRepository
import com.logoped_plus.domain.repository.TagWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoomTagRepository(private val dao: TagDao, private val scope: CoroutineScope) : TagRepository {
    private val mutableState = MutableStateFlow<TagLoadState>(TagLoadState.Loading())
    override val state: StateFlow<TagLoadState> = mutableState.asStateFlow()
    private val retries = MutableStateFlow(0L)

    init {
        scope.launch {
            retries.collectLatest {
                mutableState.value = TagLoadState.Loading(mutableState.value.tags)
                try {
                    dao.observeTags().collect { rows ->
                        mutableState.value = TagLoadState.Ready(rows.map { it.toDomain() })
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = TagLoadState.Error(mutableState.value.tags)
                }
            }
        }
    }

    override fun retryLoading() {
        val current = mutableState.value
        if (current is TagLoadState.Error &&
            mutableState.compareAndSet(current, TagLoadState.Loading(current.tags))) {
            retries.update { it + 1 }
        }
    }

    override suspend fun saveTag(tag: Tag) = write { dao.saveOnce(tag) }

    override suspend fun deleteTag(tag: Tag) = write { dao.deleteOnce(tag.id) }

    override suspend fun reorderTags(orderedIds: List<String>) = write { dao.reorderOnce(orderedIds) }

    private suspend fun write(action: suspend () -> TagWriteResult): TagWriteResult {
        if (state.value !is TagLoadState.Ready) return TagWriteResult.Failure(TagWriteResult.Reason.NotReady)
        return try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            TagWriteResult.Failure(TagWriteResult.Reason.StorageUnavailable)
        }
    }
}
