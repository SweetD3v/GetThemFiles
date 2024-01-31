package com.filerecover.photorecovery.allrecover.restore.adapters

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemGroupBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemHeaderBinding
import com.filerecover.photorecovery.allrecover.restore.databinding.ListItemImageBinding
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaClickListener
import com.filerecover.photorecovery.allrecover.restore.models.GroupMedia
import com.filerecover.photorecovery.allrecover.restore.models.RecoveryMedia
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_GROUP
import com.filerecover.photorecovery.allrecover.restore.utils.ITEM_TYPE_HEADER
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_IMAGES
import com.filerecover.photorecovery.allrecover.restore.utils.RECOVERY_TYPE_VIDEOS
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import java.io.File

class MediaListAdapter :
    ListAdapter<RecoveryMedia, RecyclerView.ViewHolder>(
        MediaComparator()
    ) {

    private var delayHandler = Handler(Looper.getMainLooper())
    private var loadImageInstantly = false

    private var visibleItemPaths = ArrayList<String>()
    private val IMAGE_LOAD_DELAY = 100L
    private val INSTANT_LOAD_DURATION = 1000L

    var mediaClickListener: MediaClickListener? = null

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
        AsyncListDiffer(this, MediaComparator())
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

            ITEM_TYPE_GROUP -> VHGroup(
                ListItemGroupBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> VHMedia(
                ListItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        val media = listDiffer.currentList[vh.layoutPosition]

        when (vh.itemViewType) {
            ITEM_TYPE_HEADER -> {
                val holder = vh as VHHeader
                holder.bind(media)
            }

            ITEM_TYPE_GROUP -> {
                val holder = vh as VHGroup
                holder.bind(media.group)
            }

            else -> {
                val holder = vh as VHMedia
                holder.bind(media)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return listDiffer.currentList[position].itemType
    }

    inner class VHMedia(val binding: ListItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(media: RecoveryMedia) {
            visibleItemPaths.add(media.path)

            binding.run {
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
                    mediaClickListener?.onMediaClick(
                        media.path
                    )
                }
            }
        }
    }

    inner class VHHeader(val binding: ListItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: RecoveryMedia) {
            binding.run {
                txtSectionHeader.text = File(media.path).parentFile?.name

                root.setOnClickListener {
                    mediaClickListener?.onGroupClick(
                        media.path
                    )
                }
            }
        }
    }

    inner class VHGroup(val binding: ListItemGroupBinding) : RecyclerView.ViewHolder(binding.root) {

        private val groupedImageViews =
            arrayListOf(binding.imgThumb1, binding.imgThumb2, binding.imgThumb3, binding.imgThumb4)

        private val groupedPlayBtns =
            arrayListOf(binding.imgPlay1, binding.imgPlay2, binding.imgPlay3, binding.imgPlay4)

        fun bind(groupedMedia: MutableList<GroupMedia>) {
            val mediaListMax = groupedMedia.subList(0, groupedMedia.size.coerceAtMost(4))
            visibleItemPaths.addAll(mediaListMax.map { it.path })

            binding.run {
                txtMoreCount.text = String.format(
                    itemView.context.getString(R.string._count_more), "+${groupedMedia.size}"
                )

                for (index in mediaListMax.indices) {
                    val media = mediaListMax[index]
                    val imgView = groupedImageViews[index]
                    val imgPlay = groupedPlayBtns[index]

                    imgView.tag = media.path

                    if (recoveryType == RECOVERY_TYPE_VIDEOS)
                        imgPlay.visible()

                    if (loadImageInstantly) Glide.with(itemView.context).load(media.path)
                        .signature(ObjectKey(media.path)).into(imgView)
                    else {
                        imgView.setImageDrawable(null)
                        delayHandler.postDelayed({
                            val isVisible = visibleItemPaths.contains(media.path)
                            if (isVisible) {
                                Glide.with(itemView.context).load(media.path).into(imgView)
                            }
                        }, IMAGE_LOAD_DELAY)
                    }
                }

                root.setOnClickListener {
                    mediaClickListener?.onGroupClick(
                        File(mediaListMax.firstOrNull()?.path.toString()).parentFile?.path.toString()
                    )
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is VHMedia -> {
                visibleItemPaths.remove(holder.binding.imgThumb.tag)
                Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb)
            }

            is VHGroup -> {
                visibleItemPaths.remove(holder.binding.imgThumb1.tag)
                visibleItemPaths.remove(holder.binding.imgThumb2.tag)
                visibleItemPaths.remove(holder.binding.imgThumb3.tag)
                visibleItemPaths.remove(holder.binding.imgThumb4.tag)
                Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb1)
                Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb2)
                Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb3)
                Glide.with(holder.binding.root.context).clear(holder.binding.imgThumb4)
            }
        }
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }
}

class MediaComparator :
    DiffUtil.ItemCallback<RecoveryMedia>() {
    override fun areItemsTheSame(
        oldItem: RecoveryMedia,
        newItem: RecoveryMedia
    ): Boolean {
        return oldItem.path == newItem.path && oldItem.group.map { it.path } == newItem.group.map { it.path }
    }

    override fun areContentsTheSame(
        oldItem: RecoveryMedia,
        newItem: RecoveryMedia
    ): Boolean {
        return oldItem.path == newItem.path && oldItem.group.map { it.path } == newItem.group.map { it.path }
    }
}