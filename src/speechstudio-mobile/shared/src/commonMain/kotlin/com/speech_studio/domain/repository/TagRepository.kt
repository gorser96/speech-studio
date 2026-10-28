package com.speech_studio.domain.repository

import com.speech_studio.domain.model.Tag
import kotlinx.coroutines.flow.StateFlow

interface TagRepository {

    val state: StateFlow<TagLoadState>
    fun retryLoading()
    suspend fun saveTag(tag: Tag): TagWriteResult
    suspend fun deleteTag(tag: Tag): TagWriteResult
    suspend fun reorderTags(orderedIds: List<String>): TagWriteResult
}
