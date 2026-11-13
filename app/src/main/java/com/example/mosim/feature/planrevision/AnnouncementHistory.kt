package com.example.mosim.feature.planrevision

/** 앱 프로세스 안에서 화면을 다시 열어도 같은 제안을 자동 안내하지 않는다. */
class AnnouncementHistory {
    private val ids = mutableSetOf<String>()

    fun claim(id: String): Boolean = ids.add(id)

    companion object {
        val session = AnnouncementHistory()
    }
}
