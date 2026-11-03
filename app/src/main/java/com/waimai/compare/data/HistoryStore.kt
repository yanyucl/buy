package com.waimai.compare.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 搜索历史：SharedPreferences 持久化 + StateFlow 供 UI 收集 */
class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<String>> = _history

    fun add(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _history.value = (listOf(q) + _history.value.filter { it != q }).take(10)
        persist()
    }

    fun clear() {
        _history.value = emptyList()
        persist()
    }

    private fun load(): List<String> =
        prefs.getStringSet(KEY, emptySet())?.sortedByDescending { it.substringBefore("|").toLongOrNull() ?: 0L }
            ?.map { it.substringAfter("|") }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    private fun persist() {
        // 用 "时间戳|关键词" 保存以维持插入顺序（StringSet 本身无序）
        val now = System.currentTimeMillis()
        val set = _history.value.mapIndexed { i, q -> "${now - i}|$q" }.toSet()
        prefs.edit().putStringSet(KEY, set).apply()
    }

    private companion object {
        const val KEY = "history"
    }
}
