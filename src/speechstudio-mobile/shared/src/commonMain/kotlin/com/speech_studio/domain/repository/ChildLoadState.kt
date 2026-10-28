package com.speech_studio.domain.repository

import com.speech_studio.domain.model.Child

sealed interface ChildLoadState {
    val children: List<Child>
    data class Loading(override val children: List<Child> = emptyList()) : ChildLoadState
    data class Ready(override val children: List<Child>) : ChildLoadState
    data class Error(override val children: List<Child> = emptyList()) : ChildLoadState
}
