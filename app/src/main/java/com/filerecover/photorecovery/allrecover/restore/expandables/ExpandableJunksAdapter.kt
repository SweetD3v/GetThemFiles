package com.filerecover.photorecovery.allrecover.restore.expandables

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.filerecover.photorecovery.allrecover.restore.R
import com.filerecover.photorecovery.allrecover.restore.expandables.data.AbstractExpandableDataProvider
import com.filerecover.photorecovery.allrecover.restore.expandables.data.ExpandableJunkDataProvider
import com.filerecover.photorecovery.allrecover.restore.interfaces.MediaSelectionListener
import com.filerecover.photorecovery.allrecover.restore.models.JunkMedia
import com.filerecover.photorecovery.allrecover.restore.utils.CELLS_IDS
import com.filerecover.photorecovery.allrecover.restore.utils.MEDIA_GRID_SIZE
import com.filerecover.photorecovery.allrecover.restore.utils.formatSize
import com.filerecover.photorecovery.allrecover.restore.utils.visible
import com.filerecover.photorecovery.allrecover.restore.widgets.ExpandableItemIndicatorFilled
import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder

class ExpandableJunksAdapter(
    private val context: Context, private val mediaSelectionListener: MediaSelectionListener?
) : AbstractExpandableItemAdapter<ExpandableJunksAdapter.GroupViewHolder, ExpandableJunksAdapter.GridRowHolder>() {

    private lateinit var mProvider: AbstractExpandableDataProvider
    var rvExpandableItemManager: RecyclerViewExpandableItemManager? = null

    var isSelecting = false
    var selectedList: MutableList<String> = mutableListOf()
    private var recoveredListMap: MutableList<Pair<JunkMedia, MutableList<JunkMedia>>> =
        mutableListOf()

    private var delayHandler = Handler(Looper.getMainLooper())
    private var loadImageInstantly = false

    private var visibleItemPaths = ArrayList<String>()
    private val IMAGE_LOAD_DELAY = 100L
    private val INSTANT_LOAD_DURATION = 1000L

    init {
        enableInstantLoad()
    }

    private fun enableInstantLoad() {
        loadImageInstantly = true
        delayHandler.postDelayed({
            loadImageInstantly = false
        }, INSTANT_LOAD_DURATION)
    }

    fun updateDataList(
        mProvider: AbstractExpandableDataProvider,
        recoveredListMap: MutableList<Pair<JunkMedia, MutableList<JunkMedia>>> = mutableListOf()
    ) {
        this.mProvider = mProvider
        this.recoveredListMap = recoveredListMap
        notifyDataSetChanged()
    }

    abstract class MyBaseViewHolder(v: View) : AbstractExpandableItemViewHolder(v) {
        var mContainer: FrameLayout

        init {
            mContainer = v.findViewById<View>(R.id.container) as FrameLayout
        }
    }

    inner class GroupViewHolder(v: View) : MyBaseViewHolder(v) {
        var icDropdown: ExpandableItemIndicatorFilled
        var txtSectionHeader: TextView
        var txtTotalSize: TextView
        var icCheck: AppCompatImageView

        init {
            icDropdown = v.findViewById<View>(R.id.icDropdown) as ExpandableItemIndicatorFilled
            txtSectionHeader = v.findViewById(R.id.txtSectionHeader)
            txtTotalSize = v.findViewById(R.id.txtTotalSize)
            icCheck = v.findViewById(R.id.icCheck)
        }
    }

    inner class GridRowHolder(v: View, mediaSelectionListener: MediaSelectionListener?) :
        MyBaseViewHolder(v) {
        var txtTitle = arrayOfNulls<TextView>(MEDIA_GRID_SIZE)
        var txtSize = arrayOfNulls<TextView>(MEDIA_GRID_SIZE)
        var cells = arrayOfNulls<FrameLayout>(CELLS_IDS.size)
        var images = arrayOfNulls<ImageView>(CELLS_IDS.size)
        var checkBoxes = arrayOfNulls<AppCompatImageView>(CELLS_IDS.size)
        var mediaSelectionListener: MediaSelectionListener? = null
        private var dataList: List<JunkMedia> = listOf()
        private var parentId: Long = -1

        init {
            this.mediaSelectionListener = mediaSelectionListener
            txtTitle = arrayOf(
                v.findViewById<View>(R.id.txtTitle1) as TextView,
                v.findViewById<View>(R.id.txtTitle2) as TextView,
                v.findViewById<View>(R.id.txtTitle3) as TextView,
            )
            txtSize = arrayOf(
                v.findViewById<View>(R.id.txtSize1) as TextView,
                v.findViewById<View>(R.id.txtSize2) as TextView,
                v.findViewById<View>(R.id.txtSize3) as TextView,
            )
            images = arrayOf(
                v.findViewById(R.id.imgThumb1),
                v.findViewById(R.id.imgThumb2),
                v.findViewById(R.id.imgThumb3),
            )
            checkBoxes = arrayOf(
                v.findViewById(R.id.icCheck1),
                v.findViewById(R.id.icCheck2),
                v.findViewById(R.id.icCheck3),
            )
            cells = arrayOf(
                v.findViewById<View>(CELLS_IDS[0]) as FrameLayout,
                v.findViewById<View>(CELLS_IDS[1]) as FrameLayout,
                v.findViewById<View>(CELLS_IDS[2]) as FrameLayout,
            )
        }

        fun updateCheckedStatus(media: JunkMedia) {
            dataList.forEachIndexed { index, _ ->
                if (dataList[index].path == media.path) {
                    val icCheck = checkBoxes[index] ?: CheckBox(context)
                    icCheck.isSelected = selectedList.contains(media.path)
                }
            }
        }

        fun populateGridImages(
            groupPosition: Int,
            mediaList: List<JunkMedia>,
            childPosition: Int = -1
        ) {
            dataList = mediaList
            this.parentId = groupPosition.toLong()
            // make sure grid data array does not exceed the number of cells
            if (mediaList.size > cells.size) {
                throw ArrayIndexOutOfBoundsException()
            }
            var lastDataIndex = -1
            var index = 0
            dataList.forEach {
                if (childPosition != -1 && childPosition == index) index = childPosition
                val junkMedia = dataList[index]
                val rootView = cells[index] ?: FrameLayout(context)
                val imgThumb = images[index] ?: ImageView(context)
                val icCheck = checkBoxes[index] ?: CheckBox(context)

                txtSize[index]?.text = junkMedia.size.formatSize()
                txtTitle[index]?.text = junkMedia.name

                visibleItemPaths.add(junkMedia.path)
                imgThumb.tag = junkMedia.path
//                icCheck.beVisibleIf(isSelecting)
                icCheck.isSelected = selectedList.contains(junkMedia.path)

                junkMedia.appIcon?.let { icon ->
                    if (loadImageInstantly) Glide.with(context).load(icon)
                        .signature(ObjectKey(junkMedia.path)).into(imgThumb)
                    else {
                        imgThumb.setImageDrawable(null)
                        delayHandler.postDelayed({
                            val isVisible = visibleItemPaths.contains(junkMedia.path)
                            if (isVisible) {
                                Glide.with(itemView.context).load(icon).into(imgThumb)
                            }
                        }, IMAGE_LOAD_DELAY)
                    }
                } ?: {
                    Glide.with(context).load(R.drawable.ic_other_ext)
                        .signature(ObjectKey(junkMedia.path)).into(imgThumb)
                }

                // attach data tag to cell
                rootView.tag = junkMedia
                // set a cell clickable only if it hold data. no need to click a blank cell
                rootView.setOnClickListener {
//                    icCheck.isSelected = !icCheck.isSelected
//                    if (icCheck.isSelected && !selectedList.contains(junkMedia.path)) selectedList.add(
//                        junkMedia.path
//                    )
//                    else if (!icCheck.isSelected && selectedList.contains(junkMedia.path)) {
//                        selectedList.remove(junkMedia.path)
//                    }
//                    isSelecting = selectedList.isNotEmpty()
//                    notifyItemChanged(childPosition, selectedList)
//                    mediaSelectionListener?.onMediaChecked(
//                        junkMedia.path,
//                        groupPosition,
//                        childPosition,
//                        icCheck.isSelected
//                    )

                    icCheck.isSelected = !icCheck.isSelected
                    if (!isSelecting) {
                        mediaSelectionListener?.onMediaLongPressed(
                            junkMedia.path, groupPosition, index, true
                        )
                    } else {
                        if (icCheck.isSelected && !selectedList.contains(junkMedia.path)) selectedList.add(
                            junkMedia.path
                        )
                        else if (!icCheck.isSelected && selectedList.contains(junkMedia.path)) {
                            selectedList.remove(junkMedia.path)
                        }
                        isSelecting = selectedList.isNotEmpty()
                        notifyItemChanged(childPosition, selectedList)
                        mediaSelectionListener?.onMediaChecked(
                            junkMedia.path,
                            groupPosition,
                            childPosition,
                            icCheck.isSelected
                        )
                    }
                }

//                icCheck.setOnClickListener {
//                    icCheck.isSelected = !icCheck.isSelected
//                    if (!isSelecting) {
//                        mediaSelectionListener?.onMediaLongPressed(
//                            junkMedia.path, groupPosition, index, true
//                        )
//                    } else {
//                        if (icCheck.isSelected && !selectedList.contains(junkMedia.path)) selectedList.add(
//                            junkMedia.path
//                        )
//                        else if (!icCheck.isSelected && selectedList.contains(junkMedia.path)) {
//                            selectedList.remove(junkMedia.path)
//                        }
//                        isSelecting = selectedList.isNotEmpty()
//                        notifyItemChanged(childPosition, selectedList)
//                        mediaSelectionListener?.onMediaChecked(
//                            junkMedia.path,
//                            groupPosition,
//                            childPosition,
//                            icCheck.isSelected
//                        )
//                    }
//                }

//                rootView.setOnLongClickListener {
//                    if (!isSelecting) {
//                        isSelecting = true
//                        if (!selectedList.contains(junkMedia.path)) selectedList.add(
//                            junkMedia.path
//                        )
//                        mediaSelectionListener?.onMediaLongPressed(
//                            junkMedia.path, groupPosition, index, true
//                        )
//                        return@setOnLongClickListener true
//                    }
//                    return@setOnLongClickListener false
//                }

                // make cell visible
                rootView.visible()
                // keep track of index of last cell used in this row
                lastDataIndex = index
                index++
            }
            // remove unused cells from row
            cleanUpGrid(lastDataIndex)
        }

        // RecyclerView is called so because it reuses the layouts of its list items
        // So we do make sure that only necessary cells of a recycled row can stay visible :)
        private fun cleanUpGrid(lastUsedIndex: Int) {
            var lastUsedIndex = lastUsedIndex
            lastUsedIndex++
            for (j in lastUsedIndex until cells.size) {
                // hide unused cell
                cells[j]?.visibility = View.GONE
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getGroupCount(): Int {
        return mProvider.groupCount
    }

    override fun getChildCount(groupPosition: Int): Int {
        // Value returned here is the paginated version of children list
        // as we want to put four (4) children in each child row
        // to provide an illusive gridview for children
        return (mProvider.getGroupItem(groupPosition) as ExpandableJunkDataProvider.ConcreteGroupData).concreteGridDataList.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong() // or mProvider.getGroupItem(groupPosition).getGroupId();
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return (mProvider.getGroupItem(groupPosition) as ExpandableJunkDataProvider.ConcreteGroupData).concreteGridDataList[childPosition].gridId
    }

    override fun getGroupItemViewType(groupPosition: Int): Int {
        return 0
    }

    override fun getChildItemViewType(groupPosition: Int, childPosition: Int): Int {
        return 0
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_junk_header, parent, false)
        return GroupViewHolder(v)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): GridRowHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.list_item_junks, parent, false)
        return GridRowHolder(v, mediaSelectionListener)
    }

    override fun onBindGroupViewHolder(
        holder: GroupViewHolder, groupPosition: Int, viewType: Int
    ) {
        // child item
        val item: AbstractExpandableDataProvider.BaseData = mProvider.getGroupItem(groupPosition)

        // set text
        holder.txtSectionHeader.text = item.text
        recoveredListMap[groupPosition].second.map { it.path }.toMutableList()
            .retainAll(selectedList.toSet())
        holder.txtTotalSize.text =
            recoveredListMap[groupPosition].second.sumOf { it.size }.formatSize()

        // mark as clickable
        holder.itemView.isClickable = true
//        holder.icCheck.beVisibleIf(isSelecting)

        // set background resource (target view ID: container)
        val expandState = holder.expandStateFlags
        if (expandState and ExpandableItemConstants.STATE_FLAG_IS_UPDATED != 0) {
            val animateIndicator =
                expandState and ExpandableItemConstants.STATE_FLAG_HAS_EXPANDED_STATE_CHANGED != 0
            val isExpanded = expandState and ExpandableItemConstants.STATE_FLAG_IS_EXPANDED != 0
            holder.icDropdown.setExpandedState(isExpanded, animateIndicator)

            val totalCount = recoveredListMap[groupPosition].second.size
            val selectedCount =
                selectedList.intersect(recoveredListMap[groupPosition].second.map { it.path }
                    .toSet()).size
            holder.icCheck.isSelected = selectedCount == totalCount

            holder.icCheck.setOnClickListener {
                holder.icCheck.isSelected = !holder.icCheck.isSelected
                holder.icCheck.post {
                    mediaSelectionListener?.onAllItemsChecked(
                        recoveredListMap[groupPosition].second.map { it.path }.toMutableList(),
                        groupPosition,
                        holder.icCheck.isSelected
                    )
                    rvExpandableItemManager?.notifyChildItemRangeChanged(
                        groupPosition,
                        0,
                        totalCount
                    )
                }
            }
        }
    }

    override fun onBindGroupViewHolder(
        holder: GroupViewHolder,
        groupPosition: Int,
        viewType: Int,
        payloads: MutableList<Any>
    ) {
        val totalCount = recoveredListMap[groupPosition].second.size
        val selectedCount =
            selectedList.intersect(recoveredListMap[groupPosition].second.map { it.path }
                .toSet()).size
        if (payloads.isNotEmpty()) {
            payloads.forEach { data ->
                if (data is String) {
//                    holder.icCheck.beVisibleIf(isSelecting)
                    recoveredListMap[groupPosition].second.map { it.path }.toMutableList()
                        .retainAll(selectedList.toSet())
                    holder.txtTotalSize.text =
                        recoveredListMap[groupPosition].second.sumOf { it.size }.formatSize()
                    holder.icCheck.isSelected = selectedCount == totalCount
                }
            }
        } else super.onBindGroupViewHolder(holder, groupPosition, viewType, payloads)
    }

    override fun onBindChildViewHolder(
        holder: GridRowHolder, groupPosition: Int, childPosition: Int, viewType: Int
    ) {
        val item = mProvider.getGridItem(
            groupPosition, childPosition
        ) as ExpandableJunkDataProvider.ConcreteGridData
        val gridData = item.gridDataArray
        holder.populateGridImages(groupPosition, gridData, childPosition)
    }

    override fun onBindChildViewHolder(
        holder: GridRowHolder,
        groupPosition: Int,
        childPosition: Int,
        viewType: Int,
        payloads: MutableList<Any>?
    ) {
        if (!payloads.isNullOrEmpty()) {
            payloads.forEach { data ->
                if (data is JunkMedia) {
                    holder.updateCheckedStatus(data)
                }
            }
        } else super.onBindChildViewHolder(holder, groupPosition, childPosition, viewType, payloads)
    }

    fun clearSelection(lm: RecyclerViewExpandableItemManager) {
//        isSelecting = false
        selectedList.clear()
        for (index in 0 until mProvider.groupCount) {
            lm.notifyGroupItemChanged(
                index,
                "${recoveredListMap[index].second.sumOf { it.size }.formatSize()}"
            )
            lm.notifyChildItemRangeChanged(index, 0, recoveredListMap[index].second.size)
        }
//        notifyDataSetChanged()
    }

    fun updateHeaderText(
        lm: RecyclerViewExpandableItemManager,
        groupPosition: Int,
        selectedStr: String
    ) {
        lm.notifyGroupItemChanged(
            groupPosition, selectedStr
        )
    }

    override fun onCheckCanExpandOrCollapseGroup(
        holder: GroupViewHolder, groupPosition: Int, x: Int, y: Int, expand: Boolean
    ): Boolean {
        // check the item is *not* pinned
        return if (mProvider.getGroupItem(groupPosition).isPinned) {
            // return false to raise View.OnClickListener#onClick() event
            false
        } else {
            val canExpand = if (!holder.icCheck.isVisible) true else {
                x > holder.icCheck.x + holder.icCheck.width || x < holder.icCheck.x
            }
            holder.itemView.isEnabled && holder.itemView.isClickable && canExpand
        }

        // check is enabled
    }

    companion object {
        private const val TAG = "ExpandableJunksAdapter"
    }
}