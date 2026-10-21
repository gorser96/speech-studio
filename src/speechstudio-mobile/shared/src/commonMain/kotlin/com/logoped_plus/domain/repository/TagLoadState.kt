package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Tag

sealed interface TagLoadState {
    val tags: List<Tag>
    data class Loading(override val tags: List<Tag> = emptyList()) : TagLoadState
    data class Ready(override val tags: List<Tag>) : TagLoadState
    data class Error(override val tags: List<Tag> = emptyList()) : TagLoadState
}
