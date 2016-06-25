package com.fin10.android.mywallpaper;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fin10.android.mywallpaper.widget.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

public final class WallpaperListFragment extends Fragment {

    private WallpaperListAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wallpaper_list, container, false);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mAdapter = new WallpaperListAdapter();

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(layoutManager.getSpanCount(),
                GridSpacingItemDecoration.convertDpToPx(getResources(), 4)));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter.clear();
    }

    public boolean onBackPressed() {
        if (mAdapter.isSelectionMode()) {
            mAdapter.setSelectionMode(false);
            return true;
        } else {
            return false;
        }
    }

    public interface OnEventListener {
        void onItemClick(@NonNull View itemView, int position);

        boolean onItemLongClick(@NonNull View itemView, int position);
    }

    private static final class WallpaperListAdapter extends RecyclerView.Adapter implements WallpaperModel.OnEventListener, OnEventListener {

        @NonNull
        private final List<WallpaperModel> mModels;
        private final List<WallpaperModel> mSelectedModels = new ArrayList<>();
        @Nullable
        private WallpaperModel mMarkedModel;
        private boolean mSelectionMode = false;

        public WallpaperListAdapter() {
            mModels = WallpaperModel.getModels();
            WallpaperModel.addEventListener(this);
        }

        public void clear() {
            mModels.clear();
            WallpaperModel.removeEventListener(this);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WallpaperViewHolder(LayoutInflater.from(parent.getContext()), parent, this);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            WallpaperModel model = mModels.get(position);
            boolean marked = WallpaperModel.isCurrentWallpaper(holder.itemView.getContext(), model);
            if (marked) mMarkedModel = model;
            if (mSelectionMode) {
                ((WallpaperViewHolder) holder).setModel(model, marked, mSelectedModels.contains(model));
            } else {
                ((WallpaperViewHolder) holder).setModel(model, marked);
            }
        }

        @Override
        public int getItemCount() {
            return mModels.size();
        }

        @Override
        public void onAdded(@NonNull WallpaperModel model) {
            mModels.add(0, model);
            notifyItemInserted(0);
        }

        @Override
        public void onRemoved(@NonNull WallpaperModel model) {
            int position = mModels.indexOf(model);
            if (position >= 0) {
                mModels.remove(model);
                notifyItemRemoved(position);
            }
        }

        @Override
        public void onWallpaperChanged(@NonNull WallpaperModel model) {
            int position = mModels.indexOf(model);
            if (position >= 0) notifyItemChanged(position);

            if (mMarkedModel != null) {
                position = mModels.indexOf(mMarkedModel);
                if (position >= 0) notifyItemChanged(position);
            }

            mMarkedModel = model;
        }

        @Override
        public void onItemClick(@NonNull View itemView, int position) {
            Log.d("[%d] onItemClick", position);
            WallpaperModel model = mModels.get(position);
            if (mSelectionMode) {
                if (!mSelectedModels.remove(model)) {
                    mSelectedModels.add(model);
                }
                notifyItemChanged(position);
            } else {
                if (mMarkedModel != model) {
                    model.setAsWallpaper(itemView.getContext());
                }
            }
        }

        @Override
        public boolean onItemLongClick(@NonNull View itemView, int position) {
            Log.d("[%d] onItemLongClick", position);
            setSelectionMode(true);
            return false;
        }

        public boolean isSelectionMode() {
            return mSelectionMode;
        }

        public void setSelectionMode(boolean selectionMode) {
            if (selectionMode != mSelectionMode) {
                mSelectionMode = selectionMode;
                mSelectedModels.clear();
                notifyDataSetChanged();
            }
        }

        private static final class WallpaperViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            @Nullable
            private final OnEventListener mListener;

            private WallpaperViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable OnEventListener listener) {
                super(inflater.inflate(R.layout.wallpaper_list_item, parent, false));
                mListener = listener;
                itemView.setTag(R.id.image_view, itemView.findViewById(R.id.image_view));
                itemView.setTag(R.id.marker_view, itemView.findViewById(R.id.marker_view));
                itemView.setTag(R.id.check_box, itemView.findViewById(R.id.check_box));
                itemView.setTag(R.id.description_view, itemView.findViewById(R.id.description_view));

                View clickView = itemView.findViewById(R.id.click_view);
                clickView.setOnClickListener(this);
                clickView.setOnLongClickListener(this);
            }

            private void setModel(@NonNull WallpaperModel model, boolean marked) {
                setModel(model, marked, false);
                ((View) itemView.getTag(R.id.check_box)).setVisibility(View.GONE);
            }

            public void setModel(WallpaperModel model, boolean marked, boolean selected) {
                int position = getAdapterPosition();
                Log.d("[%d] %s", position, model.getPath());

                Glide.with(itemView.getContext())
                        .load(model.getPath())
                        .centerCrop()
                        .into((ImageView) itemView.getTag(R.id.image_view));

                View markerView = (View) itemView.getTag(R.id.marker_view);
                markerView.setVisibility(marked ? View.VISIBLE : View.GONE);

                CheckBox checkBox = (CheckBox) itemView.getTag(R.id.check_box);
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selected);

                TextView textView = (TextView) itemView.getTag(R.id.description_view);
                textView.setText(model.getPath());
            }

            @Override
            public void onClick(View view) {
                if (mListener != null) mListener.onItemClick(itemView, getAdapterPosition());
            }

            @Override
            public boolean onLongClick(View view) {
                return mListener != null && mListener.onItemLongClick(itemView, getAdapterPosition());
            }
        }
    }
}
