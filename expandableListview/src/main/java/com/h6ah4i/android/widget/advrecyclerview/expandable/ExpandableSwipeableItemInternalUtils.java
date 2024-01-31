/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.expandable;

import androidx.recyclerview.widget.RecyclerView;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;

class ExpandableSwipeableItemInternalUtils {
    private ExpandableSwipeableItemInternalUtils() {
    }

    @SuppressWarnings("unchecked")
    public static SwipeResultAction invokeOnSwipeItem(
            BaseExpandableSwipeableItemAdapter<?, ?> adapter, RecyclerView.ViewHolder holder,
            int groupPosition, int childPosition, int result) {

        if (adapter instanceof LegacyExpandableSwipeableItemAdapter) {
            int reaction;

            if (childPosition == RecyclerView.NO_POSITION) {
                reaction = ((LegacyExpandableSwipeableItemAdapter) adapter).onSwipeGroupItem(
                        holder, groupPosition, result);
            } else {
                reaction = ((LegacyExpandableSwipeableItemAdapter) adapter).onSwipeChildItem(
                        holder, groupPosition, childPosition, result);
            }

            switch (reaction) {
                case RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT:
                case RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION:
                case RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM:
                    //noinspection deprecation
                    return new LegacyExpandableSwipeResultAction<>(
                            (LegacyExpandableSwipeableItemAdapter<RecyclerView.ViewHolder, RecyclerView.ViewHolder>) adapter,
                            holder, groupPosition, childPosition, result, reaction);
                default:
                    throw new IllegalStateException("Unexpected reaction type: " + reaction);
            }
        } else {
            if (childPosition == RecyclerView.NO_POSITION) {
                return ((ExpandableSwipeableItemAdapter) adapter).onSwipeGroupItem(holder, groupPosition, result);
            } else {
                return ((ExpandableSwipeableItemAdapter) adapter).onSwipeChildItem(holder, groupPosition, childPosition, result);
            }
        }
    }
}
