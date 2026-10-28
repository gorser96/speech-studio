package com.speech_studio.data.repository

import com.speech_studio.data.local.ChildDao
import com.speech_studio.data.local.toDomain
import com.speech_studio.domain.model.Child
import com.speech_studio.domain.model.ChildScheduleValidation
import com.speech_studio.domain.repository.ChildLoadState
import com.speech_studio.domain.repository.ChildRepository
import com.speech_studio.domain.repository.ChildWriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoomChildRepository(private val dao: ChildDao, private val scope: CoroutineScope) : ChildRepository {
    private val mutableState = MutableStateFlow<ChildLoadState>(ChildLoadState.Loading())
    override val state: StateFlow<ChildLoadState> = mutableState.asStateFlow()
    private val retries = MutableStateFlow(0L)

    init {
        scope.launch {
            retries.collectLatest {
                mutableState.value = ChildLoadState.Loading(mutableState.value.children)
                try {
                    dao.observeChildrenWithTags().collect { rows ->
                        mutableState.value = ChildLoadState.Ready(rows.map { it.toDomain() })
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = ChildLoadState.Error(mutableState.value.children)
                }
            }
        }
    }

    override fun retryLoading() {
        val current = mutableState.value
        if (current is ChildLoadState.Error &&
            mutableState.compareAndSet(current, ChildLoadState.Loading(current.children))) {
            retries.update { it + 1 }
        }
    }

    override fun observeCard(id: String): Flow<Child?> =
        dao.observeCard(id).map { rows -> rows.firstOrNull()?.toDomain() }

    override suspend fun addChild(child: Child) = write(child) { name ->
        if (dao.addOnce(child.id, name)) ChildWriteResult.Success
        else ChildWriteResult.Failure(ChildWriteResult.Reason.Conflict)
    }

    override suspend fun updateChild(child: Child) = write(child) { name ->
        if (dao.rename(child.id, name) == 1) ChildWriteResult.Success
        else ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound)
    }

    override suspend fun saveCard(child: Child): ChildWriteResult {
        val name = child.name.trim()
        if (name.isBlank()) return ChildWriteResult.Failure(ChildWriteResult.Reason.InvalidName)
        if (ChildScheduleValidation.validate(child.schedule) != null)
            return ChildWriteResult.Failure(ChildWriteResult.Reason.InvalidSchedule)
        if (state.value !is ChildLoadState.Ready) return ChildWriteResult.Failure(ChildWriteResult.Reason.NotReady)
        val card = child.copy(name = name, tagIds = child.tagIds.filter { it.isNotBlank() }.distinct())
        return try {
            dao.saveCardOnce(card)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ChildWriteResult.Failure(ChildWriteResult.Reason.StorageUnavailable)
        }
    }

    override suspend fun deleteChild(child: Child) = write(child) {
        if (dao.deleteOnce(child.id)) ChildWriteResult.Success
        else ChildWriteResult.Failure(ChildWriteResult.Reason.NotFound)
    }

    private suspend fun write(child: Child, action: suspend (String) -> ChildWriteResult): ChildWriteResult {
        val name = child.name.trim()
        if (name.isBlank()) return ChildWriteResult.Failure(ChildWriteResult.Reason.InvalidName)
        if (state.value !is ChildLoadState.Ready) return ChildWriteResult.Failure(ChildWriteResult.Reason.NotReady)
        return try {
            action(name)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ChildWriteResult.Failure(ChildWriteResult.Reason.StorageUnavailable)
        }
    }
}
