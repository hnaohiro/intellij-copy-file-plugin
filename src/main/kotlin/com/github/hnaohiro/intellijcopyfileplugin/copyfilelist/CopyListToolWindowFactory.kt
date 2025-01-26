package com.github.hnaohiro.intellijcopyfileplugin.copyfilelist

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.messages.MessageBusConnection
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.BoxLayout.X_AXIS
import javax.swing.BoxLayout.Y_AXIS
import javax.swing.border.EmptyBorder

class CopyListToolWindowFactory : ToolWindowFactory {

    private lateinit var mainPanel: JPanel
    private lateinit var containerPanel: JPanel
    private lateinit var copyListService: CopyListService
    private lateinit var project: Project

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.project = project
        copyListService = CopyListService.getInstance(project)

        // メインの大枠パネル
        mainPanel = JPanel(BorderLayout())

        // 上部のボタンパネル
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 8)).apply {
            val copyAllButton = JButton("Copy All").apply {
                addActionListener { copyAllFiles() }
            }
            add(copyAllButton)

            val removeAllButton = JButton("Remove All").apply {
                addActionListener { copyListService.clear() }
            }
            add(removeAllButton)
        }
        mainPanel.add(topPanel, BorderLayout.NORTH)

        // (A) containerPanel: ファイル行を BoxLayout(Y_AXIS) で上から順に追加
        containerPanel = JPanel().apply {
            layout = BoxLayout(this, Y_AXIS)
            // 左寄せ＆上寄せ設定（子要素が BoxLayout で並ぶ）
            alignmentX = JComponent.LEFT_ALIGNMENT
            alignmentY = JComponent.TOP_ALIGNMENT
        }

        // (B) contentPanel: BorderLayout
        //     containerPanel を NORTH に配置し、余白があっても上側に詰める
        val contentPanel = JPanel(BorderLayout()).apply {
            add(containerPanel, BorderLayout.NORTH)
        }

        // (C) contentPanel をスクロールペインに入れて CENTER へ
        val scrollPane = JScrollPane(
            contentPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // ツールウィンドウに登録
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

        // ファイル追加/削除の通知を受け取り、UI 再描画
        val connection: MessageBusConnection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(
            CopyListNotifier.COPY_LIST_CHANGED_TOPIC,
            object : CopyListNotifier {
                override fun fileListUpdated() {
                    refreshUI()
                }
            }
        )

        // 初期表示
        refreshUI()
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    private fun refreshUI() {
        containerPanel.removeAll()

        val files = copyListService.getFiles()
        for (file in files) {
            // 1行分 (X_AXIS) で「x」＋ファイル名を表示
            val rowPanel = JPanel().apply {
                layout = BoxLayout(this, X_AXIS)
                // 行自体を上寄せ・左寄せ
                alignmentX = JComponent.LEFT_ALIGNMENT
                alignmentY = JComponent.TOP_ALIGNMENT
                border = EmptyBorder(4, 8, 4, 8)
            }

            // “x” ラベル (クリックで削除) → 上寄せ
            val removeLabel = JLabel("x").apply {
                toolTipText = "Remove this file"
                alignmentX = JComponent.LEFT_ALIGNMENT
                alignmentY = JComponent.TOP_ALIGNMENT
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        copyListService.removeFile(file)
                    }
                })
            }
            rowPanel.add(removeLabel)

            // xラベルとファイル名ラベルの間隔 (お好みで調整可)
            rowPanel.add(Box.createHorizontalStrut(4))

            // ファイル名ラベル → 上寄せ & 上下パディング0
            val basePath = project.basePath
            val baseVFile = basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
            val relativePath = if (baseVFile != null) {
                VfsUtilCore.getRelativePath(file, baseVFile, '/')
            } else {
                file.path
            }

            val pathLabel = JLabel(relativePath ?: file.path).apply {
                alignmentX = JComponent.LEFT_ALIGNMENT
                alignmentY = JComponent.TOP_ALIGNMENT
            }
            rowPanel.add(pathLabel)

            containerPanel.add(rowPanel)
            // 必要なら行間をあける: containerPanel.add(Box.createVerticalStrut(2)) など
        }

        // リストを最上部へ詰め、あまったスペースは下部へ集める
        containerPanel.add(Box.createVerticalGlue())

        containerPanel.revalidate()
        containerPanel.repaint()
    }

    /**
     * "Copy All" ボタンの処理
     */
    private fun copyAllFiles() {
        val files = copyListService.getFiles()
        val fullText = buildString {
            val basePath = project.basePath
            val baseVFile = basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
            files.forEachIndexed { index, file ->
                val relPath = if (baseVFile != null) {
                    VfsUtilCore.getRelativePath(file, baseVFile, '/')
                } else file.path
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
