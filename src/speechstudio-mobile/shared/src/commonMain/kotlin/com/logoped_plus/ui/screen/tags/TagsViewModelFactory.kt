package com.logoped_plus.ui.screen.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.logoped_plus.domain.repository.TagRepository
import kotlin.reflect.KClass

class TagsViewModelFactory(
    private val tagRepository: TagRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass == TagsViewModel::class) {
            return TagsViewModel(tagRepository = tagRepository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.simpleName}"
        )
    }
}
