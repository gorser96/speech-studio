package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Tag
import kotlinx.coroutines.flow.StateFlow

interface TagRepository {

    val state: StateFlow<TagLoadState>
    fun retryLoading()
    suspend fun saveTag(tag: Tag): TagWriteResult
    suspend fun deleteTag(tag: Tag): TagWriteResult
    suspend fun reorderTags(orderedIds: List<String>): TagWriteResult
}
