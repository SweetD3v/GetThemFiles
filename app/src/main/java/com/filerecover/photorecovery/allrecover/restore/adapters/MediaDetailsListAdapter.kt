package com.filerecover.photorecovery.allrecover.restore.adapters

import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemImageBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaItemSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_MEDIA
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_VIDEOS
import com.filerecover.photorecovery.allrecover.restore.utils.beVisibleIf
import com.filerecover.photorecovery.allrecover.restore.utils.visible

class MediaDetailsListAdapter :
    ListAdapter<RecoveryMedia, RecyclerView.ViewHolder>(
        MediaDetailsComparator()
    ) {

    private var delayHandler = Handler(Looper.getMainLooper())
    private var loadImageInstantly = false

    private var visibleItemPaths = ArrayList<String>()
    private val IMAGE_LOAD_DELAY = 100L
    private val INSTANT_LOAD_DURATION = 1000L

    var mediaSelectionListener: MediaItemSelectionListener? = null
    var isSelecting = false
    var selected: HashSet<Int> = hashSetOf()
    var selectedList: HashSet<String> = hashSetOf()

    private var _mediaSelection: MutableLiveData<MutableList<RecoveryMedia>> = MutableLiveData()
    val mediaSelection: LiveData<MutableList<RecoveryMedia>> = _mediaSelection

    var recoveryType: Int = RECOVERY_TYPE_IMAGES

    init {
        enableInstantLoad()
    }

    private fun enableInstantLoad() {
        loadImageInstantly = true
        delayHandler.postDelayed({
            loadImageInstantly = false
        }, INSTANT_LOAD_DURATION)
    }

    private val listDiffer: AsyncListDiffer<RecoveryMedia> by lazy {
        AsyncListDiffer(this, MediaDetailsComparator())
    }

    fun updateList(mediaList: List<RecoveryMedia>) {
        listDiffer.submitList(mediaList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHMedia(
            ListItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { pl ->
                isSelecting = pl as Boolean
                Log.e("TAG", "onBindViewHolder: $position")
                if (holder is VHMedia) {
                    updateChecks(holder, position)
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        val media = listDiffer.currentList[vh.layoutPosition]

        if (vh.itemViewType == ITEM_TYPE_MEDIA) {
            val holder = vh as VHMedia
            holder.bind(media)
        }
    }

    private fun updateChecks(holder: VHMedia, position: Int) {
        holder.binding.run {
            icCheck.beVisibleIf(isSelecting)
            icCheck.isSelected = selectedList.contains(listDiffer.currentList[position].path)
        }
    }

    fun toggleSelection(pos: Int) {
        val currentList = listDiffer.currentList
        if (selectedList.contains(currentList[pos].path)) {
            selected.remove(pos)
            selectedList.remove(currentList[pos].path)
        } else {
            selected.add(pos)
            selectedList.add(currentList[pos].path)
        }
        notifyItemChanged(pos, isSelecting)
        isSelecting = selectedList.isNotEmpty()
        if (!isSelecting) notifyItemRangeChanged(0, itemCount, isSelecting)
        _mediaSelection.postValue(getSelectedMedia())
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        val currentList = listDiffer.currentList
        for (i in start..end) {
            if (selected) {
                this.selected.add(i)
                this.selectedList.add(currentList[i].path)
            } else {
                this.selected.remove(i)
                this.selectedList.remove(currentList[i].path)
            }
        }

        isSelecting = selectedList.isNotEmpty()
        notifyItemRangeChanged(start, end - start + 1, isSelecting)
        _mediaSelection.postValue(getSelectedMedia())
    }

    fun clearSelection() {
        selected.clear()
        selectedList.clear()
        isSelecting = false
        notifyItemRangeChanged(0, listDiffer.currentList.size, isSelecting)
        _mediaSelection.postValue(arrayListOf())
    }

    fun selectAll() {
        isSelecting = true
        val currentList = listDiffer.currentList
        for (i in 0 until currentList.size) {
            selectedList.add(currentList[i].path)
            selected.add(i)
        }
        notifyItemRangeChanged(0, listDiffer.currentList.size, isSelecting)
        _mediaSelection.postValue(getSelectedMedia())
    }

    fun getSelectedMedia(): MutableList<RecoveryMedia> {
        return listDiffer.currentList.filter { selectedList.contains(it.path) }
            .distinctBy { it.path }
            .toMutableList()
    }

    fun getSelection(): HashSet<Int> {
        return selected
    }

    override fun getItemViewType(position: Int): Int {
        return listDiffer.currentList[position].itemType
    }

    inner class VHMedia(val binding: ListItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(media: RecoveryMedia) {
            visibleItemPaths.add(media.path)

            binding.run {
                icCheck.beVisibleIf(isSelecting)
                icCheck.isSelected = selectedList.contains(media.path)

                imgThumb.tag = media.path

                if (recoveryType == RECOVERY_TYPE_VIDEOS)
                    imgPlay.visible()

                if (loadImageInstantly) Glide.with(itemView.context).load(media.path)
                    .signature(ObjectKey(media.path)).into(imgThumb)
                else {
                    imgThumb.setImageDrawable(null)
                    delayHandler.postDelayed({
                        val isVisible = visibleItemPaths.contains(media.path)
                        if (isVisible) {
                            Glide.with(itemView.context).load(media.path).into(imgThumb)
                        }
                    }, IMAGE_LOAD_DELAY)
                }

                root.setOnClickListener {
                    if (isSelecting)
                        toggleSelection(layoutPosition)
                    else mediaSelectionListener?.onMediaClick(layoutPosition, media.path)
                }

                root.setOnLongClickListener {
                    if (!isSelecting) {
                        isSelecting = true
                        mediaSelectionListener?.onMediaLongPressed(
                            layoutPosition, listDiffer.currentList[layoutPosition].path
                        ) == true
                    } else if (getSelectedMedia().size in 1 until itemCount) {
                        isSelecting = true
                        mediaSelectionListener?.onMediaLongPressed(
                            layoutPosition, listDiffer.currentList[layoutPosition].path
                        ) == true
                    } else false
                }
            }
        }
    }

    override fun onViewRecycled(vh: RecyclerView.ViewHolder) {
        super.onViewRecycled(vh)
        if (vh.itemViewType == ITEM_TYPE_MEDIA) {
            val holder = vh as VHMedia
            visibleItemPaths.remove(holder.binding.imgThumb.tag)
            Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb)
        }
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }
}

class MediaDetailsComparator :
    DiffUtil.ItemCallback<RecoveryMedia>() {
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