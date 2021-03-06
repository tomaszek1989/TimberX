/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.naman14.timberx.ui.fragments

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.naman14.timberx.R
import com.naman14.timberx.models.Song
import com.naman14.timberx.ui.adapters.SongsAdapter
import com.naman14.timberx.ui.listeners.SortMenuListener
import com.naman14.timberx.util.Constants.SONG_SORT_ORDER
import com.naman14.timberx.util.SongSortOrder.SONG_A_Z
import com.naman14.timberx.util.SongSortOrder.SONG_DURATION
import com.naman14.timberx.util.SongSortOrder.SONG_YEAR
import com.naman14.timberx.util.SongSortOrder.SONG_Z_A
import com.naman14.timberx.util.extensions.addOnItemClick
import com.naman14.timberx.util.extensions.defaultPrefs
import com.naman14.timberx.util.extensions.getExtraBundle
import com.naman14.timberx.util.extensions.toSongIds
import kotlinx.android.synthetic.main.layout_recyclerview.recyclerView

class SongsFragment : MediaItemFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_recyclerview, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val adapter = SongsAdapter().apply {
            showHeader = true

            popupMenuListener = mainViewModel.popupMenuListener

            sortMenuListener = object : SortMenuListener {
                override fun shuffleAll() {
                    songs.shuffled().apply {
                        mainViewModel.mediaItemClicked(this[0],
                                getExtraBundle(toSongIds(), "All songs"))
                    }
                }

                override fun sortAZ() {
                    activity?.defaultPrefs()?.edit {
                        putString(SONG_SORT_ORDER, SONG_A_Z)
                    }
                    mediaItemFragmentViewModel.reloadMediaItems()
                }

                override fun sortDuration() {
                    activity?.defaultPrefs()?.edit {
                        putString(SONG_SORT_ORDER, SONG_DURATION)
                    }
                    mediaItemFragmentViewModel.reloadMediaItems()
                }

                override fun sortYear() {
                    activity?.defaultPrefs()?.edit {
                        putString(SONG_SORT_ORDER, SONG_YEAR)
                    }
                    mediaItemFragmentViewModel.reloadMediaItems()
                }

                override fun sortZA() {
                    activity?.defaultPrefs()?.edit {
                        putString(SONG_SORT_ORDER, SONG_Z_A)
                    }
                    mediaItemFragmentViewModel.reloadMediaItems()
                }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        mediaItemFragmentViewModel.mediaItems.observe(this,
                Observer<List<MediaBrowserCompat.MediaItem>> { list ->
                    val isEmptyList = list?.isEmpty() ?: true
                    if (!isEmptyList) {
                        @Suppress("UNCHECKED_CAST")
                        adapter.updateData(list as ArrayList<Song>)
                    }
                })

        recyclerView.addOnItemClick { position: Int, _: View ->
            adapter.getSongForPosition(position)?.let { song ->
                val extras = getExtraBundle(adapter.songs.toSongIds(), "All songs")
                mainViewModel.mediaItemClicked(song, extras)
            }
        }
    }
}
