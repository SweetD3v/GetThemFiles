package com.filerecover.photorecovery.allrecover.restore.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.filerecover.photorecovery.allrecover.restore.databinding.ItemStorageStatBinding
import com.filerecover.photorecovery.allrecover.restore.models.CallbackStatus
import com.filerecover.photorecovery.allrecover.restore.models.FileTypes
import com.filerecover.photorecovery.allrecover.restore.models.StatsModel
import com.filerecover.photorecovery.allrecover.restore.utils.beVisibleIf
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.formatSizeCeil
import com.filerecover.photorecovery.allrecover.restore.utils.invisible
import com.filerecover.photorecovery.allrecover.restore.utils.visible

class StorageStatsAdapter : ListAdapter<StatsModel, StorageStatsAdapter.VH>(StatComparator()) {

    private val listDiffer: AsyncListDiffer<StatsModel> by lazy {
        AsyncListDiffer(this, StatComparator())
    }

    private var stat: StatsModel? = null

    fun updateStat(stat: StatsModel?) {
        this.stat = stat
    }

    fun updateList(statsList: List<StatsModel>, index: Int = -1) {
        listDiffer.submitList(statsList)
        if (index >= 0) {
            notifyItemChanged(index, stat)
        }
    }

    inner class VH(val binding: ItemStorageStatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: StatsModel) {
            binding.run {
                imgColor.imageTintList = ColorStateList.valueOf(stat.statColor)
                txtStatType.text = stat.stateName
                txtStatSize.text =
                    if (stat.result.fileTypes == FileTypes.SYSTEM) stat.result.size.formatSizeCeil() else stat.result.size.formatSize()

                if (stat.result.status == CallbackStatus.SUCCESS)
                    txtStatSize.visible()
                else txtStatSize.invisible()
                progressBar.beVisibleIf(
                    stat.result.status == CallbackStatus.LOADING ||
                            stat.result.status == CallbackStatus.IDLE
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemStorageStatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { data ->
                if (data is StatsModel) {
                    holder.bind(data)
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val stat = listDiffer.currentList[holder.layoutPosition]
        holder.bind(stat)
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }
}

class StatComparator : DiffUtil.ItemCallback<StatsModel>() {
    override fun areItemsTheSame(oldItem: StatsModel, newItem: StatsModel): Boolean {
        return (oldItem.result.fileTypes == newItem.result.fileTypes
                && oldItem.result.size == newItem.result.size
                && oldItem.result.status == newItem.result.status)
    }

    override fun areContentsTheSame(oldItem: StatsModel, newItem: StatsModel): Boolean {
        return (oldItem.result.fileTypes == newItem.result.fileTypes
                && oldItem.result.size == newItem.result.size
                && oldItem.result.status == newItem.result.status)
    }
}