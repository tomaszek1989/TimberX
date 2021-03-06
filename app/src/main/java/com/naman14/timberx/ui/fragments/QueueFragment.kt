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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.naman14.timberx.R
import com.naman14.timberx.models.QueueData
import com.naman14.timberx.repository.SongsRepository
import com.naman14.timberx.ui.adapters.SongsAdapter
import com.naman14.timberx.ui.widgets.DragSortRecycler
import com.naman14.timberx.util.Constants
import com.naman14.timberx.util.doAsyncPostWithResult
import com.naman14.timberx.util.extensions.addOnItemClick
import com.naman14.timberx.util.extensions.getExtraBundle
import com.naman14.timberx.util.extensions.keepInOrder
import com.naman14.timberx.util.extensions.toSongIds
import kotlinx.android.synthetic.main.fragment_queue.*

class QueueFragment : BaseNowPlayingFragment() {

    lateinit var adapter: SongsAdapter

    private lateinit var queueData: QueueData

    private var isReorderFromUser = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = SongsAdapter().apply {
            isQueue = true
            popupMenuListener = mainViewModel.popupMenuListener
        }

        recyclerView.run {
            layoutManager = LinearLayoutManager(activity)
            adapter = adapter
        }

        nowPlayingViewModel.queueData.observe(this, Observer {
            this.queueData = it
            tvQueueTitle.text = it?.queueTitle
            if (it.queue.isNotEmpty()) {
                fetchQueueSongs(it.queue)
            }
        })

        recyclerView.addOnItemClick { position, _ ->
            adapter.getSongForPosition(position)?.let { song ->
                val extras = getExtraBundle(adapter.songs.toSongIds(), queueData.queueTitle)
                mainViewModel.mediaItemClicked(song, extras)
            }
        }
    }

    private fun fetchQueueSongs(queue: LongArray) {
        //to avoid lag when reordering queue, we dont refetch queue if we know the reorder was from user
        if (isReorderFromUser) {
            isReorderFromUser = false
            return
        }

        doAsyncPostWithResult(handler = {
            SongsRepository.getSongsForIDs(activity!!, queue).keepInOrder(queue)
        }, postHandler = {
            if (it != null) {
                adapter.updateData(it)

                val dragSortRecycler = DragSortRecycler().apply {
                    setViewHandleId(R.id.ivReorder)
                    setOnItemMovedListener { from, to ->
                        isReorderFromUser = true
                        adapter.reorderSong(from, to)
                        mainViewModel.transportControls().sendCustomAction(Constants.ACTION_QUEUE_REORDER, Bundle().apply {
                            putInt(Constants.QUEUE_FROM, from)
                            putInt(Constants.QUEUE_TO, to)
                        })
                    }
                }

                recyclerView.run {
                    addItemDecoration(dragSortRecycler)
                    addOnItemTouchListener(dragSortRecycler)
                    addOnScrollListener(dragSortRecycler.scrollListener)
                }
            }
        }).execute()
    }
}
