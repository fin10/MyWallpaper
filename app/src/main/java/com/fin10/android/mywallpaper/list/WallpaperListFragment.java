package com.fin10.android.mywallpaper.list;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.fin10.android.mywallpaper.Constants;
import com.fin10.android.mywallpaper.Log;
import com.fin10.android.mywallpaper.R;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.fin10.android.mywallpaper.widget.GridSpacingItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class WallpaperListFragment extends Fragment implements OnItemEventListener, View.OnClickListener {

    private View mEmptyView;
    private WallpaperListAdapter mAdapter;
    private ActionMode mActionMode;
    private Dialog mDeleteDialog;
    private final ActionMode.Callback mCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.wallpaper_list_fragment, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_share: {
                    List<WallpaperModel> items = mAdapter.getSelectedItems();
                    ArrayList<Uri> imageUris = new ArrayList<>();
                    for (WallpaperModel item : items) {
                        imageUris.add(FileProvider.getUriForFile(getActivity(), Constants.FileProvider.AUTHORITY, new File(item.getImagePath())));
                    }

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
                    shareIntent.setType("image/*");
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                    actionMode.finish();
                    break;
                }
                case R.id.menu_item_delete: {
                    if (mDeleteDialog == null) {
                        mDeleteDialog = new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.do_you_want_to_delete)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        List<WallpaperModel> items = mAdapter.getSelectedItems();
                                        mAdapter.remove(items);
                                        actionMode.finish();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .create();
                    }
                    mDeleteDialog.show();
                    break;
                }
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mAdapter.setSelectionMode(false);
            mActionMode = null;
        }
    };

    private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            refreshEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            refreshEmptyView();
        }

        private void refreshEmptyView() {
            int count = mAdapter.getItemCount();
            if (count == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else if (mEmptyView.getVisibility() == View.VISIBLE) {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wallpaper_list, container, false);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mAdapter = new WallpaperListAdapter(this);

        mEmptyView = root.findViewById(R.id.empty_view);
        mEmptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        View discoveryButton = mEmptyView.findViewById(R.id.discovery_button);
        discoveryButton.setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(layoutManager.getSpanCount(),
                GridSpacingItemDecoration.convertDpToPx(getResources(), 1)));

        mAdapter.registerAdapterDataObserver(mObserver);

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
        int count = mAdapter.getItemCount();
        if (count == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else if (mEmptyView.getVisibility() == View.VISIBLE) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter.clear();
        mAdapter.unregisterAdapterDataObserver(mObserver);
    }

    @Override
    public void onItemClick(@NonNull View itemView, int position) {
        Log.d("[%d] onItemClick", position);
        if (mAdapter.isSelectionMode()) {
            mAdapter.setSelected(position);
            if (mActionMode != null) {
                mActionMode.setTitle(String.valueOf(mAdapter.getSelectedItems().size()));
            }
        } else {
            mAdapter.setMarked(getActivity(), position);
        }
    }

    @Override
    public boolean onItemLongClick(@NonNull View itemView, int position) {
        Log.d("[%d] onItemLongClick", position);
        if (!mAdapter.isSelectionMode()) {
            mAdapter.setSelectionMode(true);
            mActionMode = getActivity().startActionMode(mCallback);
            onItemClick(itemView, position);
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.discovery_button:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=wallpaper&tbm=isch")));
                break;
        }
    }

    private static final class WallpaperListAdapter extends RecyclerView.Adapter {

        private final List<WallpaperModel> mModels;
        private final List<WallpaperModel> mSelectedModels = new ArrayList<>();
        private final OnItemEventListener mListener;

        private long mMarkedModelId = -1;
        private boolean mSelectionMode = false;

        public WallpaperListAdapter(@Nullable OnItemEventListener listener) {
            mListener = listener;
            mModels = WallpaperModel.getModels();
            EventBus.getDefault().register(this);
        }

        public void clear() {
            EventBus.getDefault().unregister(this);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WallpaperViewHolder(LayoutInflater.from(parent.getContext()), parent, mListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            WallpaperModel model = mModels.get(position);
            boolean marked = WallpaperModel.isCurrentWallpaper(holder.itemView.getContext(), model);
            if (marked) mMarkedModelId = model.getId();
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

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onAdded(@NonNull WallpaperModel.AddEvent event) {
            mModels.add(0, event.model);
            notifyItemInserted(0);
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onRemoved(@NonNull WallpaperModel.RemoveEvent event) {
            for (WallpaperModel model : mSelectedModels) {
                if (model.getId() == event.id) {
                    mSelectedModels.remove(model);
                    break;
                }
            }

            int position = 0;
            for (WallpaperModel model : mModels) {
                if (model.getId() == event.id) {
                    mModels.remove(model);
                    notifyItemRemoved(position);
                    break;
                }
                ++position;
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onWallpaperChanged(@NonNull WallpaperModel.SetAsWallpaperEvent event) {
            if (mMarkedModelId > 0) {
                int position = 0;
                for (WallpaperModel model : mModels) {
                    if (model.getId() == mMarkedModelId) {
                        notifyItemChanged(position);
                        break;
                    }
                    ++position;
                }
            }

            int position = 0;
            for (WallpaperModel model : mModels) {
                if (model.getId() == event.id) {
                    notifyItemChanged(position);
                    break;
                }
                ++position;
            }

            mMarkedModelId = position;
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

        public void setMarked(@NonNull Context context, int position) {
            WallpaperModel model = mModels.get(position);
            if (mMarkedModelId != model.getId()) {
                model.setAsWallpaper(context);
                mMarkedModelId = model.getId();
            }
        }

        public void setSelected(int position) {
            WallpaperModel model = mModels.get(position);
            if (!mSelectedModels.remove(model)) {
                mSelectedModels.add(model);
            }

            notifyDataSetChanged();
        }

        @NonNull
        public List<WallpaperModel> getSelectedItems() {
            return mSelectedModels;
        }

        public void remove(List<WallpaperModel> models) {
            for (WallpaperModel model : models) {
                WallpaperModel.removeModel(model);
            }
        }

        private static final class WallpaperViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

            private final OnItemEventListener mListener;

            private WallpaperViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable OnItemEventListener listener) {
                super(inflater.inflate(R.layout.wallpaper_list_item, parent, false));
                mListener = listener;
                itemView.setTag(R.id.image_view, itemView.findViewById(R.id.image_view));
                itemView.setTag(R.id.marker_view, itemView.findViewById(R.id.marker_view));
                itemView.setTag(R.id.check_box, itemView.findViewById(R.id.check_box));

                View clickView = itemView.findViewById(R.id.click_view);
                clickView.setOnClickListener(this);
                clickView.setOnLongClickListener(this);
            }

            public void setModel(@NonNull WallpaperModel model, boolean marked) {
                setModel(model, marked, false);
                ((View) itemView.getTag(R.id.check_box)).setVisibility(View.GONE);
            }

            public void setModel(WallpaperModel model, boolean marked, boolean selected) {
                Glide.with(itemView.getContext())
                        .load(model.getImagePath())
                        .centerCrop()
                        .into((ImageView) itemView.getTag(R.id.image_view));

                View markerView = (View) itemView.getTag(R.id.marker_view);
                markerView.setVisibility(marked ? View.VISIBLE : View.GONE);

                CheckBox checkBox = (CheckBox) itemView.getTag(R.id.check_box);
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selected);
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
