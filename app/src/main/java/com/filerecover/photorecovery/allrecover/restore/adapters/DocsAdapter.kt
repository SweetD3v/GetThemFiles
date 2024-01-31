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
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemHeaderBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_HEADER
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.beVisibleIf
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.gone
import java.io.File

class DocsAdapter :
    ListAdapter<RecoveryMedia, RecyclerView.ViewHolder>(
        DocsComparator()
    ) {

    var mediaSelectionListener: MediaItemSelectionListener? = null

    var isSelecting = false
    var selected: HashSet<Int> = hashSetOf()
    var selectedList: HashSet<String> = hashSetOf()

    private var _docsSelection: MutableLiveData<MutableList<RecoveryMedia>> = MutableLiveData()
    val docsSelection: LiveData<MutableList<RecoveryMedia>> = _docsSelection

    var recoveryType: Int = RECOVERY_TYPE_IMAGES

    private val listDiffer: AsyncListDiffer<RecoveryMedia> by lazy {
        AsyncListDiffer(this, DocsComparator())
    }

    fun updateList(mediaList: MutableList<RecoveryMedia>) {
        listDiffer.submitList(mediaList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_HEADER -> VHHeader(
                ListItemHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> {
                VHDocs(
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
                if (holder is VHDocs) {
                    updateChecks(holder, position)
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
                val holder = vh as VHDocs
                holder.bind(media)
            }
        }
    }

    private fun updateChecks(holder: VHDocs, position: Int) {
        holder.binding.run {
            icCheck.beVisibleIf(isSelecting)
            icCheck.isSelected = selectedList.contains(listDiffer.currentList[position].path)
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
        if (!isSelecting) notifyItemRangeChanged(0, itemCount, isSelecting)
        _docsSelection.postValue(getSelectedDocs())
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
        _docsSelection.postValue(getSelectedDocs())
    }

    fun clearSelection() {
        selected.clear()
        selectedList.clear()
        isSelecting = false
        notifyItemRangeChanged(0, listDiffer.currentList.size, isSelecting)
        _docsSelection.postValue(arrayListOf())
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
        _docsSelection.postValue(getSelectedDocs())
    }

    fun getSelectedDocs(): MutableList<RecoveryMedia> {
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

    inner class VHDocs(val binding: ListItemDocsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(media: RecoveryMedia) {

            binding.run {
                icCheck.beVisibleIf(isSelecting)
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
                    if (isSelecting)
                        toggleSelection(layoutPosition)
                    else mediaSelectionListener?.onMediaClick(
                        layoutPosition,
                        media.path
                    )
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

    inner class VHHeader(val binding: ListItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: RecoveryMedia) {
            binding.run {
                icDropdown.gone()
                txtSectionHeader.text = File(media.path).parentFile?.name
            }
        }
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }
}

class DocsComparator : DiffUtil.ItemCallback<RecoveryMedia>() {
    override fun areItemsTheSame(
        oldItem: RecoveryMedia,
        newItem: RecoveryMedia
    ): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(
        oldItem: RecoveryMedia,
        newItem: RecoveryMedia
    ): Boolean {
        return oldItem.path == newItem.path
    }
}