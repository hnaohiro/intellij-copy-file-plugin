package com.github.hnaohiro.intellijcopyfileplugin.copyfilelist

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.messages.MessageBusConnection
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class CopyListToolWindowFactory : ToolWindowFactory {

    private lateinit var mainPanel: JPanel
    private lateinit var containerPanel: JPanel
    private lateinit var copyListService: CopyListService
    private lateinit var project: Project

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.project = project
        copyListService = CopyListService.getInstance(project)

        mainPanel = JPanel(BorderLayout())

        // 縦方向にファイル行を積み上げ
        containerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        // 上寄せのため NORTH に配置
        mainPanel.add(containerPanel, BorderLayout.NORTH)

        // 下部パネル
        val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))

        // Copy All ボタン
        val copyAllButton = JButton("Copy All").apply {
            addActionListener {
                copyAllFiles()
            }
        }
        bottomPanel.add(copyAllButton)

        // Remove All ボタン (名称変更)
        val removeAllButton = JButton("Remove All").apply {
            addActionListener {
                // 全ファイル削除
                copyListService.clear()
            }
        }
        bottomPanel.add(removeAllButton)

        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        // ツールウィンドウに登録
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        // メッセージバス購読 → リスト更新時に refreshUI()
        val connection: MessageBusConnection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(
            CopyListNotifier.COPY_LIST_CHANGED_TOPIC,
            object : CopyListNotifier {
                override fun fileListUpdated() {
                    refreshUI()
                }
            }
        )

        // 初期描画
        refreshUI()
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    /**
     * ファイル一覧を再描画
     */
    private fun refreshUI() {
        containerPanel.removeAll()

        val files = copyListService.getFiles()
        for (file in files) {
            // 各行パネルを BoxLayout(X_AXIS) に
            val rowPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
            }

            // 相対パス
            val basePath = project.basePath
            val baseVFile = basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
            val relativePath = if (baseVFile != null) {
                VfsUtilCore.getRelativePath(file, baseVFile, '/')
            } else {
                file.path
            }

            val label = JLabel(relativePath ?: file.path)
            rowPanel.add(label)

            // 水平のグルーを入れることで、次のコンポーネント(ボタン)が右端に寄る
            rowPanel.add(Box.createHorizontalGlue())

            val removeButton = JButton("Remove").apply {
                addActionListener {
                    copyListService.removeFile(file)
                }
            }
            rowPanel.add(removeButton)

            containerPanel.add(rowPanel)
        }

        containerPanel.revalidate()
        containerPanel.repaint()
    }

    /**
     * "Copy All" ボタン処理
     */
    private fun copyAllFiles() {
        val files = copyListService.getFiles()
        val fullText = buildString {
            val basePath = project.basePath
            val baseVFile = basePath?.let {
                LocalFileSystem.getInstance().findFileByPath(it)
            }
            files.forEachIndexed { index, file ->
                val relPath = if (baseVFile != null) {
                    VfsUtilCore.getRelativePath(file, baseVFile, '/')
                } else {
                    file.path
                }
                append(relPath).append("\n")

                append("```\n")
                val content = String(file.contentsToByteArray(), file.charset)
                append(content)
                append("```\n")

                if (index < files.size - 1) {
                    append("---\n")
                }
            }
        }

        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val selection = java.awt.datatransfer.StringSelection(fullText)
        clipboard.setContents(selection, selection)
    }
}