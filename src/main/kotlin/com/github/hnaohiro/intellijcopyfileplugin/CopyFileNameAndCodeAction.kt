package com.github.hnaohiro.intellijcopyfileplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Paths

class CopyFileNameAndCodeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val vFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (vFile == null) {
            println("No file selected.")
            return
        }

        // ファイル名・内容を取得（※ファイル名はログ用に使うなら残してOK）
        val fileName = vFile.name
        val fileContent = String(vFile.contentsToByteArray(), vFile.charset)

        // プロジェクトのベースパスからの相対パスを作る
        val project = e.project
        val relativePath: String? = if (project != null && project.basePath != null) {
            val projectBasePath = Paths.get(project.basePath!!)
            val fileAbsolutePath = Paths.get(vFile.path)
            projectBasePath.relativize(fileAbsolutePath).toString()
        } else {
            vFile.path // プロジェクトが取れない場合は絶対パスを利用
        }

        // クリップボードへコピーする文字列:
        //   1行目 -> 相対パス（インデントなし）
        //   2行目 -> 空行
        //   3行目以降 -> ファイル内容
        val combinedText = "$relativePath\n\n$fileContent"

        // クリップボードにセット
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(combinedText)
        clipboard.setContents(selection, selection)

        // ログなどに利用 (必要なら)
        println("Copied file path and code to clipboard: $relativePath ($fileName)")
    }
}