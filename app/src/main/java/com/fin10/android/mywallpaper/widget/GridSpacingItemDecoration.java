package com.fin10.android.mywallpaper.widget;

import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int mSpanCount;
    private final int mSpacing;

    public GridSpacingItemDecoration(int spanCount, int spacing) {
        mSpanCount = spanCount;
        mSpacing = spacing;
    }

    public static int convertDpToPx(@NonNull Resources res, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int left = mSpacing;
        int right = mSpacing;
        int top = 0;
        int bottom = mSpacing;

        StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        int index = params.getSpanIndex();
        if (index == 0) {
            right = 0;
        }

        int position = params.getViewAdapterPosition();
        if (position < mSpanCount) {
            top = mSpacing;
        }

        outRect.set(left, top, right, bottom);
    }
}
