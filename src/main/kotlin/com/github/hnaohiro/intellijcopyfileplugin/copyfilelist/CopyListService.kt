package com.github.hnaohiro.intellijcopyfileplugin.copyfilelist

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class CopyListService(val project: Project) {
    private val files = mutableListOf<VirtualFile>()

    fun addFile(file: VirtualFile) {
        // 既に同じパスのファイルがあれば重複追加しない
        if (files.none { it.path == file.path }) {
            files.add(file)
            project.messageBus
                .syncPublisher(CopyListNotifier.COPY_LIST_CHANGED_TOPIC)
                .fileListUpdated()
        }
    }

    fun removeFile(file: VirtualFile) {
        // パスで探して削除
        val found = files.find { it.path == file.path }
        if (found != null) {
            files.remove(found)
            project.messageBus
                .syncPublisher(CopyListNotifier.COPY_LIST_CHANGED_TOPIC)
                .fileListUpdated()
        }
    }

    fun getFiles(): List<VirtualFile> = files.toList()

    fun clear() {
        files.clear()
        project.messageBus
            .syncPublisher(CopyListNotifier.COPY_LIST_CHANGED_TOPIC)
            .fileListUpdated()
    }

    companion object {
        fun getInstance(project: Project): CopyListService = project.service()
    }
}