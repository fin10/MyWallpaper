package com.fin10.android.mywallpaper.widget;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

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
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//        StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
//        int index = params.getSpanIndex();
//        int position = params.getViewAdapterPosition();
//        Log.d("spanIdx:%d, adapter_pos:%d, layout_pos:%d", index, position, parent.getChildLayoutPosition(view));
//
//        int left = mSpacing;
//        int right = mSpacing;
//        int top = mSpacing;
//        int bottom = 0;
//
//        if (index == 0) {
//            right = 0;
//        }
//
//        outRect.set(left, top, right, bottom);
    }
}
