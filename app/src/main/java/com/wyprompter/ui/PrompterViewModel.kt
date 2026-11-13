package com.wyprompter.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wyprompter.data.Manuscript
import com.wyprompter.data.ManuscriptDao
import com.wyprompter.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PrompterViewModel(
    private val dao: ManuscriptDao,
    private val settings: SettingsRepository
) : ViewModel() {

    val manuscripts: Flow<List<Manuscript>> = dao.observeAll()

    var fontSize: Float by mutableStateOf(settings.fontSize)
        private set
    var scrollSpeed: Float by mutableStateOf(settings.scrollSpeed)
        private set
    var opacity: Float by mutableStateOf(settings.opacity)
        private set
    var autoScroll: Boolean by mutableStateOf(settings.autoScroll)
        private set
    var fontColor: Int by mutableStateOf(settings.fontColor)
        private set
    var backgroundColor: Int by mutableStateOf(settings.backgroundColor)
        private set
    var overlaySize: Int by mutableStateOf(settings.overlayPresetSize)
        private set
    var floatingWindow: Boolean by mutableStateOf(settings.floatingWindow)
        private set

    var editingId by mutableStateOf(0L)
    var editingTitle by mutableStateOf("")
    var editingContent by mutableStateOf("")

    fun save(
        id: Long,
        title: String,
        content: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == 0L) {
                val newId = dao.insert(
                    Manuscript(title = title, content = content, createdAt = now, updatedAt = now)
                )
                onSaved(newId)
            } else {
                val existing = dao.getById(id)
                if (existing != null) {
                    dao.update(existing.copy(title = title, content = content, updatedAt = now))
                    onSaved(id)
                }
            }
        }
    }

    fun setCurrent(id: Long) {
        settings.currentManuscriptId = id
    }

    fun delete(manuscript: Manuscript) {
        viewModelScope.launch { dao.delete(manuscript) }
    }

    fun updateFontSize(v: Float) {
        fontSize = v
        settings.fontSize = v
    }

    fun updateScrollSpeed(v: Float) {
        scrollSpeed = v
        settings.scrollSpeed = v
    }

    fun updateOpacity(v: Float) {
        opacity = v
        settings.opacity = v
    }

    fun updateAutoScroll(v: Boolean) {
        autoScroll = v
        settings.autoScroll = v
    }

    fun updateFontColor(c: Int) {
        fontColor = c
        settings.fontColor = c
    }

    fun updateBackgroundColor(c: Int) {
        backgroundColor = c
        settings.backgroundColor = c
    }

    fun updateOverlaySize(i: Int) {
        overlaySize = i
        settings.overlayPresetSize = i
    }

    fun updateFloatingWindow(v: Boolean) {
        floatingWindow = v
        settings.floatingWindow = v
    }

    class Factory(
        private val dao: ManuscriptDao,
        private val settings: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrompterViewModel(dao, settings) as T
        }
    }
}