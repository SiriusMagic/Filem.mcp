package com.zirakusa.filem.ui

import com.zirakusa.filem.data.FileItem

/** What the screens ask of the phone (MainActivity does it; tests record it). */
interface Actions {
    fun open(item: FileItem)
    fun share(paths: List<String>)
    fun copyLink(text: String)
    fun goToHub()
}
