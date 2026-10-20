package com.logoped_plus.domain.repository

import com.logoped_plus.domain.model.Child

sealed interface ChildLoadState {
    val children: List<Child>
    data class Loading(override val children: List<Child> = emptyList()) : ChildLoadState
    data class Ready(override val children: List<Child>) : ChildLoadState
    data class Error(override val children: List<Child> = emptyList()) : ChildLoadState
}
