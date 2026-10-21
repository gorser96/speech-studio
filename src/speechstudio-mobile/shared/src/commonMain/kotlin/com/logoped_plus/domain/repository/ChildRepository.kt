package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Child
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChildRepository {

    val state: StateFlow<ChildLoadState>
    fun retryLoading()
    fun observeCard(id: String): Flow<Child?>
    suspend fun addChild(child: Child): ChildWriteResult
    suspend fun updateChild(child: Child): ChildWriteResult
    suspend fun saveCard(child: Child): ChildWriteResult
    suspend fun deleteChild(child: Child): ChildWriteResult
}
