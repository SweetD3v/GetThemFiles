package com.h6ah4i.android.widget.advrecyclerview;

import androidx.recyclerview.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.animator.BaseItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class GeneralItemAnimatorTest extends ActivityInstrumentationTestCase2<TestActivity> {

    GeneralItemAnimator mAnimator;
    Adapter mAdapter;
    ViewGroup mDummyParent;
    CountDownLatch mExpectedItems;

    Set<RecyclerView.ViewHolder> mRemoveFinished = new HashSet<>();
    Set<RecyclerView.ViewHolder> mAddFinished = new HashSet<>();
    Set<RecyclerView.ViewHolder> mMoveFinished = new HashSet<>();
    Set<RecyclerView.ViewHolder> mChangeFinished = new HashSet<>();

    public GeneralItemAnimatorTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAnimator = onCreateTestTargetItemAnimator();
        mAnimator.setDebug(true);
        mAdapter = new Adapter(20);
        mDummyParent = getActivity().mContainer;
        mAnimator.setListener(new BaseItemAnimator.ItemAnimatorListener() {
            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                assertTrue(mRemoveFinished.add(item));
                onFinished();
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                assertTrue(mAddFinished.add(item));
                onFinished();
            }

            @Override
            public void onMoveFinished(RecyclerView.ViewHolder item) {
                assertTrue(mMoveFinished.add(item));
                onFinished();
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item) {
                assertTrue(mChangeFinished.add(item));
                onFinished();
            }

            private void onFinished() {
                if (mExpectedItems != null) {
                    mExpectedItems.countDown();
                }
            }
        });
    }

    protected abstract GeneralItemAnimator onCreateTestTargetItemAnimator();

    void expectItems(int count) {
        mExpectedItems = new CountDownLatch(count);
    }

    void runAndWait(int seconds) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimator.runPendingAnimations();
            }
        });
        waitForItems(seconds);
    }

    void waitForItems(int seconds) throws InterruptedException {
        mExpectedItems.await(seconds, TimeUnit.SECONDS);
        assertEquals("all expected finish events should happen", 0, mExpectedItems.getCount());
    }

    public void testAnimateAdd() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateAdd(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateRemove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateRemove(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateMove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateMove(vh, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateChange() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(2);
        expectItems(2);
        assertTrue(animateChange(vh, vh2, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    boolean animateAdd(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateAdd(vh);
            }
        });
        return result[0];
    }

    boolean animateRemove(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateRemove(vh);
            }
        });
        return result[0];
    }

    boolean animateMove(final RecyclerView.ViewHolder vh, final int fromX, final int fromY,
                        final int toX, final int toY) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateMove(vh, fromX, fromY, toX, toY);
            }
        });
        return result[0];
    }

    boolean animateChange(final RecyclerView.ViewHolder oldHolder,
                          final RecyclerView.ViewHolder newHolder,
                          final int fromX, final int fromY, final int toX, final int toY) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
            }
        });
        return result[0];
    }

    private ViewHolder createViewHolder(final int pos) throws Throwable {
        final ViewHolder vh = mAdapter.createViewHolder(mDummyParent, 1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.bindViewHolder(vh, pos);
                mDummyParent.addView(vh.itemView);
            }
        });

        return vh;
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        List<String> mItems;

        private Adapter(int count) {
            mItems = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                mItems.add("item-" + i);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        String mBindedText;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(String text) {
            mBindedText = text;
            ((TextView) itemView).setText(text);
        }
    }
}

