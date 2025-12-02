package com.example.loracle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.example.loracle.models.SessionPreview

class SessionAdapter(
    private val onClick: (SessionPreview) -> Unit
) : ListAdapter<SessionPreview, SessionAdapter.SessionViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SessionPreview>() {
            override fun areItemsTheSame(old: SessionPreview, new: SessionPreview): Boolean {
                return old.sessionId == new.sessionId
            }

            override fun areContentsTheSame(old: SessionPreview, new: SessionPreview): Boolean {
                return old == new
            }
        }
    }

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.txtSessionTitle)
        private val last: TextView = itemView.findViewById(R.id.txtSessionLastMessage)

        fun bind(item: SessionPreview) {
            title.text = item.title
            last.text = item.lastMessage

            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drawer_session_row, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
