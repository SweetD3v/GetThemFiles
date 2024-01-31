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


class ExpandableItemIndicatorImplAnim extends ExpandableItemIndicator.Impl {
    private ImageView mImageView;
    private int mColor;

    @Override
    public void onInit(Context context, AttributeSet attrs, int defStyleAttr, ExpandableItemIndicator thiz) {
        View v = LayoutInflater.from(context).inflate(R.layout.widget_expandable_item_indicator, thiz, true);
        mImageView = v.findViewById(R.id.image_view);
        mColor = ContextCompat.getColor(context, R.color._c7c7cc);
    }

    @Override
    public void setExpandedState(boolean isExpanded, boolean animate) {
        if (animate) {
            int resId = isExpanded ? R.drawable.ic_expand_more_to_expand_less : R.drawable.ic_expand_less_to_expand_more;
            mImageView.setImageResource(resId);
            DrawableCompat.setTint(mImageView.getDrawable(), mColor);
            ((Animatable) mImageView.getDrawable()).start();
        } else {
            int resId = isExpanded ? R.drawable.ic_expand_less_vector : R.drawable.ic_expand_more_vector;
            mImageView.setImageResource(resId);
            DrawableCompat.setTint(mImageView.getDrawable(), mColor);
        }
    }
}
