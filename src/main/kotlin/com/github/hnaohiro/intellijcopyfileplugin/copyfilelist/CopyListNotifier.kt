package com.github.hnaohiro.intellijcopyfileplugin.copyfilelist

import com.intellij.util.messages.Topic

// CopyListService でファイルリストが更新された際に通知を受け取るためのインターフェース
interface CopyListNotifier {
    fun fileListUpdated()

    companion object {
        val COPY_LIST_CHANGED_TOPIC = Topic.create(
            "Copy List changed",
            CopyListNotifier::class.java
        )
    }
}