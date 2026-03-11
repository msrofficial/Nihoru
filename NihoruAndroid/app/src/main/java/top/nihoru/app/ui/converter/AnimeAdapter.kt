package top.nihoru.app.ui.converter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import top.nihoru.app.R
import top.nihoru.app.data.model.AnimeEntry

class AnimeAdapter(
    private val onDelete: (AnimeEntry) -> Unit,
    private val onStatusChange: (Int, String) -> Unit
) : ListAdapter<AnimeEntry, AnimeAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvTitleEng: TextView = view.findViewById(R.id.tv_title_eng)
        val tvType: TextView = view.findViewById(R.id.tv_type)
        val tvYear: TextView = view.findViewById(R.id.tv_year)
        val tvEpisodes: TextView = view.findViewById(R.id.tv_episodes)
        val btnStatusCw: Button = view.findViewById(R.id.btn_status_cw)
        val btnStatusC: Button = view.findViewById(R.id.btn_status_c)
        val btnStatusPtw: Button = view.findViewById(R.id.btn_status_ptw)
        val btnStatusOh: Button = view.findViewById(R.id.btn_status_oh)
        val btnStatusD: Button = view.findViewById(R.id.btn_status_d)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)

        holder.tvTitle.text = entry.title
        holder.tvTitleEng.text = entry.titleEnglish ?: ""
        holder.tvType.text = entry.type ?: "TV"
        holder.tvYear.text = entry.year?.toString() ?: ""
        holder.tvEpisodes.text = when {
            entry.episodes != null && entry.episodes > 0 -> "${entry.episodes} eps"
            entry.chapters != null && entry.chapters > 0 -> "${entry.chapters} ch"
            else -> ""
        }

        // Load image
        Glide.with(holder.ivCover.context)
            .load(entry.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        // Status buttons highlight
        updateStatusButtons(holder, entry.userStatus)

        val statusButtons = mapOf(
            holder.btnStatusCw to "Watching",
            holder.btnStatusC to "Completed",
            holder.btnStatusPtw to "Plan to Watch",
            holder.btnStatusOh to "On-Hold",
            holder.btnStatusD to "Dropped"
        )

        statusButtons.forEach { (btn, status) ->
            btn.setOnClickListener {
                onStatusChange(entry.malId, status)
                updateStatusButtons(holder, status)
            }
        }

        holder.btnDelete.setOnClickListener { onDelete(entry) }
    }

    private fun updateStatusButtons(holder: ViewHolder, activeStatus: String) {
        val ctx = holder.itemView.context
        val activeColor = ctx.getColor(R.color.accent)
        val inactiveColor = ctx.getColor(R.color.text_muted)

        val statusMap = mapOf(
            holder.btnStatusCw to "Watching",
            holder.btnStatusC to "Completed",
            holder.btnStatusPtw to "Plan to Watch",
            holder.btnStatusOh to "On-Hold",
            holder.btnStatusD to "Dropped"
        )

        statusMap.forEach { (btn, status) ->
            if (status == activeStatus) {
                btn.setBackgroundResource(R.drawable.status_btn_active_bg)
                btn.setTextColor(ctx.getColor(android.R.color.white))
            } else {
                btn.setBackgroundResource(R.drawable.status_btn_bg)
                btn.setTextColor(inactiveColor)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimeEntry>() {
        override fun areItemsTheSame(oldItem: AnimeEntry, newItem: AnimeEntry) =
            oldItem.malId == newItem.malId
        override fun areContentsTheSame(oldItem: AnimeEntry, newItem: AnimeEntry) =
            oldItem == newItem
    }
}
