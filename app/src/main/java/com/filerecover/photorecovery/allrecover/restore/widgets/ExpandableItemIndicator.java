package com.filerecover.photorecovery.allrecover.restore.widgets;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.FrameLayout;

public class ExpandableItemIndicator extends FrameLayout {
    static abstract class Impl {
        public abstract void onInit(Context context, AttributeSet attrs, int defStyleAttr, ExpandableItemIndicator thiz);

        public abstract void setExpandedState(boolean isExpanded, boolean animate);
    }

    private Impl mImpl;

    public ExpandableItemIndicator(Context context) {
        super(context);
        onInit(context, null, 0);
    }

    public ExpandableItemIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        onInit(context, attrs, 0);
    }

    public ExpandableItemIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onInit(context, attrs, defStyleAttr);
    }

    protected void onInit(Context context, AttributeSet attrs, int defStyleAttr) {
        // NOTE: VectorDrawable only supports API level 21 or later
        mImpl = new ExpandableItemIndicatorImplAnim();
        mImpl.onInit(context, attrs, defStyleAttr, this);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchThawSelfOnly(container);
    }

    public void setExpandedState(boolean isExpanded, boolean animate) {
        mImpl.setExpandedState(isExpanded, animate);
    }
}
