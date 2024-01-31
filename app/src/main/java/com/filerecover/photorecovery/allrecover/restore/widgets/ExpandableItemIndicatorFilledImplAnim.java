package com.filerecover.photorecovery.allrecover.restore.widgets;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.filerecover.photorecovery.allrecover.restore.R;


class ExpandableItemIndicatorFilledImplAnim extends ExpandableItemIndicatorFilled.Impl {
    private ImageView mImageView;
    private int mColor;
    private int mColorExpanded;

    @Override
    public void onInit(Context context, AttributeSet attrs, int defStyleAttr, ExpandableItemIndicatorFilled thiz) {
        View v = LayoutInflater.from(context).inflate(R.layout.widget_expandable_item_indicator_filled, thiz, true);
        mImageView = v.findViewById(R.id.image_view);
        mImageView.setImageResource(R.drawable.ic_down);
        mColor = ContextCompat.getColor(context, R.color._999999);
        mColorExpanded = ContextCompat.getColor(context, R.color._007aff);
    }

    @Override
    public void setExpandedState(boolean isExpanded, boolean animate) {
        if (isExpanded)
            DrawableCompat.setTint(mImageView.getDrawable(), mColorExpanded);
        else DrawableCompat.setTint(mImageView.getDrawable(), mColor);
        if (animate) {
            int resId = isExpanded ? R.drawable.ic_expand_more_to_expand_less_filled : R.drawable.ic_expand_less_to_expand_more_filled;
            mImageView.setImageResource(resId);
            ((Animatable) mImageView.getDrawable()).start();
        } else {
            int resId = isExpanded ? R.drawable.ic_up : R.drawable.ic_down;
            mImageView.setImageResource(resId);
        }
    }
}
