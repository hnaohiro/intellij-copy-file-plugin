package com.github.hnaohiro.intellijcopyfileplugin.copyfilelist

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AddToCopyListAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (vFile.isDirectory) {
            // ディレクトリなら、全ファイルを再帰的に追加
            addDirectoryRecursively(project, vFile)
        } else {
            // 通常のファイルなら単一追加
            val service = CopyListService.getInstance(project)
            service.addFile(vFile)
            println("Added to copy list: ${vFile.path}")
        }
    }

    /**
     * ディレクトリ配下の全ファイルを再帰的に追加する
     */
    private fun addDirectoryRecursively(project: Project, directory: VirtualFile) {
        val service = CopyListService.getInstance(project)

        // ディレクトリ直下の子要素を順に処理
        for (child in directory.children) {
            if (child.isDirectory) {
                // サブディレクトリなら、さらに再帰的に追加
                addDirectoryRecursively(project, child)
            } else {
                // ファイルならサービスに追加
                service.addFile(child)
                println("Added to copy list: ${child.path}")
            }
        }
    }
}
