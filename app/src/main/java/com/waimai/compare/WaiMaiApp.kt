package com.waimai.compare

import android.app.Application
import com.waimai.compare.data.HistoryStore

class WaiMaiApp : Application() {

    lateinit var historyStore: HistoryStore
        private set

    override fun onCreate() {
        super.onCreate()
        historyStore = HistoryStore(this)
    }
}
