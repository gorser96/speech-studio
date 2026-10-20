package com.logoped_plus.ui.screen.schedule

import androidx.compose.runtime.Composable
import com.logoped_plus.domain.model.Child

@Composable
fun LessonEditView(
    editor: LessonEditorState,
    children: List<Child>,
    childrenReady: Boolean,
    canSave: Boolean,
    onAction: (ScheduleAction) -> Unit,
    videoScanInProgress: Boolean,
    videoScanMessage: String?
) {
    LessonForm(editor, children, childrenReady, canSave, onAction, videoScanInProgress, videoScanMessage)
}
