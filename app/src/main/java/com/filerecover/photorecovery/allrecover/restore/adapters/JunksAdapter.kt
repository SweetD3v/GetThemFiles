package com.filerecover.photorecovery.allrecover.restore.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemDocsBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemHeaderJunkBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.JunkMedia
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_HEADER
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import java.io.File

class JunksAdapter :
    ListAdapter<JunkMedia, RecyclerView.ViewHolder>(
        JunksComparator()
    ) {

    var mediaSelectionListener: MediaItemSelectionListener? = null

    var isSelecting = false
    var selected: HashSet<Int> = hashSetOf()
    var selectedList: HashSet<String> = hashSetOf()

    private var _junksSelection: MutableLiveData<MutableList<JunkMedia>> = MutableLiveData()
    val junksSelection: LiveData<MutableList<JunkMedia>> = _junksSelection

    private val listDiffer: AsyncListDiffer<JunkMedia> by lazy {
        AsyncListDiffer(this, JunksComparator())
    }

    fun updateList(mediaList: MutableList<JunkMedia>) {
        listDiffer.submitList(mediaList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_HEADER -> VHHeader(
                ListItemHeaderJunkBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> {
                VHJunks(
                    ListItemDocsBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { pl ->
                isSelecting = pl as Boolean
                if (holder is VHJunks) {
                    updateChecks(holder, position)
                } else if (holder is VHHeader) {
                    updateHeaderCheck(holder)
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        val media = listDiffer.currentList[vh.layoutPosition]

        when (vh.itemViewType) {
            ITEM_TYPE_HEADER -> {
                val holder = vh as VHHeader
                holder.bind(media)
            }

            else -> {
                val holder = vh as VHJunks
                holder.bind(media)
            }
        }
    }

    private fun updateChecks(holder: VHJunks, position: Int) {
        holder.binding.run {
//            icCheck.beVisibleIf(isSelecting)
            icCheck.isSelected = selectedList.contains(listDiffer.currentList[position].path)
        }
    }

    private fun updateHeaderCheck(holder: VHHeader) {
        holder.binding.run {
            icCheck.isSelected = selectedList.size == itemCount - 1
        }
    }

    fun toggleSelection(pos: Int) {
        val currentList = listDiffer.currentList
        if (currentList[pos].itemType != ITEM_TYPE_HEADER) {
            if (selectedList.contains(currentList[pos].path)) {
                selected.remove(pos)
                selectedList.remove(currentList[pos].path)
            } else {
                selected.add(pos)
                selectedList.add(currentList[pos].path)
            }
        }
        notifyItemChanged(pos, isSelecting)
        isSelecting = selectedList.isNotEmpty()
        notifyItemChanged(0, isSelecting)
        if (!isSelecting) notifyItemRangeChanged(0, itemCount, isSelecting)
        _junksSelection.postValue(getSelectedDocs())
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        val currentList = listDiffer.currentList
        for (i in start..end) {
            if (currentList[i].itemType != ITEM_TYPE_HEADER) {
                if (selected) {
                    this.selected.add(i)
                    this.selectedList.add(currentList[i].path)
                } else {
                    this.selected.remove(i)
                    this.selectedList.remove(currentList[i].path)
                }
            }
        }

        isSelecting = selectedList.isNotEmpty()
        notifyItemRangeChanged(start, end - start + 1, isSelecting)
        notifyItemChanged(0, isSelecting)
        _junksSelection.postValue(getSelectedDocs())
    }

    fun clearSelection() {
        selected.clear()
        selectedList.clear()
        isSelecting = false
        notifyItemRangeChanged(0, listDiffer.currentList.size, isSelecting)
        notifyItemChanged(0, false)
        _junksSelection.postValue(arrayListOf())
    }

    fun selectAll() {
        isSelecting = true
        val currentList = listDiffer.currentList
        for (i in 0 until currentList.size) {
            if (currentList[i].itemType != ITEM_TYPE_HEADER) {
                selectedList.add(currentList[i].path)
                selected.add(i)
            }
        }
        notifyItemRangeChanged(0, listDiffer.currentList.size, isSelecting)
        _junksSelection.postValue(getSelectedDocs())
    }

    fun getSelectedDocs(): MutableList<JunkMedia> {
        return listDiffer.currentList.filter { selectedList.contains(it.path) && it.itemType != ITEM_TYPE_HEADER }
            .distinctBy { it.path }
            .toMutableList()
    }

    fun getSelection(): HashSet<Int> {
        return selected
    }

    override fun getItemViewType(position: Int): Int {
        return listDiffer.currentList[position].itemType
    }

    inner class VHJunks(val binding: ListItemDocsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(media: JunkMedia) {

            binding.run {
//                icCheck.beVisibleIf(isSelecting)
                icCheck.isSelected = selectedList.contains(media.path)

                txtTitle.text = File(media.path).name
                txtSize.text = media.size.formatSize()

                media.appIcon?.let { icon ->
                    Glide.with(itemView.context).load(icon)
                        .signature(ObjectKey(media.path)).into(imgThumb)
                } ?: {
                    Glide.with(itemView.context).load(R.drawable.ic_other_ext)
                        .signature(ObjectKey(media.path)).into(imgThumb)
                }

                root.setOnClickListener {
                    toggleSelection(layoutPosition)
                }

                root.setOnLongClickListener {
                    if (!isSelecting) {
                        isSelecting = true
                        mediaSelectionListener?.onMediaLongPressed(
                            layoutPosition, listDiffer.currentList[layoutPosition].path
                        ) == true
                    } else if (getSelectedDocs().size in 1 until itemCount) {
                        isSelecting = true
                        mediaSelectionListener?.onMediaLongPressed(
                            layoutPosition, listDiffer.currentList[layoutPosition].path
                        ) == true
                    } else false
                }
            }
        }
    }

    inner class VHHeader(val binding: ListItemHeaderJunkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: JunkMedia) {
            binding.run {
                icCheck.isSelected = isSelecting
                txtSectionHeader.text = media.name

                icCheck.setOnClickListener {
                    if (selectedList.size < itemCount - 1) {
                        selectAll()
                    } else clearSelection()

                    icCheck.isSelected = selectedList.size == itemCount - 1
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }
}

class JunksComparator : DiffUtil.ItemCallback<JunkMedia>() {
    override fun areItemsTheSame(
        oldItem: JunkMedia,
        newItem: JunkMedia
    ): Boolean {
        return oldItem.path == newItem.path && oldItem.name == newItem.name
    }

    override fun areContentsTheSame(
        oldItem: JunkMedia,
        newItem: JunkMedia
    ): Boolean {
        return oldItem.path == newItem.path && oldItem.name == newItem.name
    }
}