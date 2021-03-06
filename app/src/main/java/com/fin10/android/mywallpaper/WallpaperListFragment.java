package com.fin10.android.mywallpaper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fin10.android.mywallpaper.drive.SyncManager;
import com.fin10.android.mywallpaper.live.LiveWallpaperService;
import com.fin10.android.mywallpaper.model.WallpaperChanger;
import com.fin10.android.mywallpaper.model.WallpaperModel;
import com.fin10.android.mywallpaper.settings.PreferenceModel;
import com.fin10.android.mywallpaper.widget.GridSpacingItemDecoration;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public final class WallpaperListFragment extends Fragment implements OnItemEventListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WallpaperListFragment.class);

    private View mEmptyView;
    private SwipeRefreshLayout mRefreshLayout;
    private WallpaperListAdapter mAdapter;
    private ActionMode mActionMode;
    private Dialog mDeleteDialog;
    private final ActionMode.Callback mCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            final Activity activity = getActivity();
            if (activity == null) return false;

            activity.getMenuInflater().inflate(R.menu.wallpaper_list_fragment, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            final Activity activity = getActivity();
            if (activity == null) return false;

            switch (menuItem.getItemId()) {
                case R.id.menu_item_share: {
                    ArrayList<Uri> imageUris = mAdapter.getSelectedItems().stream()
                            .map(item -> FileProvider.getUriForFile(activity, Constants.FileProvider.AUTHORITY, new File(item.getImagePath())))
                            .collect(Collectors.toCollection(ArrayList::new));

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
                    shareIntent.setType("image/*");
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));

                    activity.runOnUiThread(actionMode::finish);
                    break;
                }
                case R.id.menu_item_delete: {
                    if (mDeleteDialog == null) {
                        mDeleteDialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.delete)
                                .setMessage(R.string.do_you_want_to_delete)
                                .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                                    List<WallpaperModel> items = mAdapter.getSelectedItems();
                                    mAdapter.remove(activity, new ArrayList<>(items));

                                    activity.runOnUiThread(actionMode::finish);
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
            final Activity activity = getActivity();
            if (activity == null) return;

            mAdapter.setSelectionMode(false);
            mActionMode = null;

            if (PreferenceModel.isSyncEnabled(activity)) {
                mRefreshLayout.setEnabled(true);
            }
        }
    };
    private Snackbar mSnackBar;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Activity activity = getActivity();
        if (activity == null) return null;

        EventBus.getDefault().register(this);

        View root = inflater.inflate(R.layout.fragment_wallpaper_list, container, false);

        mAdapter = new WallpaperListAdapter(this);

        mEmptyView = root.findViewById(R.id.empty_view);
        View discoveryButton = mEmptyView.findViewById(R.id.discovery_button);
        discoveryButton.setOnClickListener(this);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            LOGGER.error("Did not found layout manager in recycler view");
            return null;
        }

        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(layoutManager.getSpanCount(),
                GridSpacingItemDecoration.convertDpToPx(getResources(), 8)));

        mAdapter.registerAdapterDataObserver(mObserver);

        mRefreshLayout = root.findViewById(R.id.swipe_layout);
        mRefreshLayout.setColorSchemeResources(R.color.primary);
        mRefreshLayout.setOnRefreshListener(this);

        if (PreferenceModel.isSyncEnabled(activity)) {
            SyncManager.sync(activity);
        }

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();
        if (activity == null) return;

        mSnackBar = Snackbar.make(activity.findViewById(R.id.coordinator_layout), R.string.it_needs_to_set_live_wallpaper, Snackbar.LENGTH_SHORT);
        mSnackBar.setActionTextColor(ActivityCompat.getColor(activity, R.color.primary));
        mSnackBar.setAction(R.string.set, view -> {
            startActivity(LiveWallpaperService.getIntentForSetLiveWallpaper());
            mSnackBar.dismiss();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        if (activity == null) return;

        mRefreshLayout.setEnabled(PreferenceModel.isSyncEnabled(activity));

        mAdapter.notifyDataSetChanged();
        int count = mAdapter.getItemCount();
        if (count == 0) mEmptyView.setVisibility(View.VISIBLE);
        else if (mEmptyView.getVisibility() == View.VISIBLE) mEmptyView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        mAdapter.clear();
        mAdapter.unregisterAdapterDataObserver(mObserver);
    }

    @Override
    public void onItemClick(@NonNull View itemView, int position) {
        final Activity activity = getActivity();
        if (activity == null) return;

        if (mAdapter.isSelectionMode()) {
            mAdapter.setSelected(position);
            if (mActionMode != null) {
                mActionMode.setTitle(String.valueOf(mAdapter.getSelectedItems().size()));
            }
        } else {
            try {
                if (!LiveWallpaperService.isSet(activity)) {
                    if (!mSnackBar.isShown()) mSnackBar.show();
                } else {
                    WallpaperModel model = mAdapter.mModels.get(position);
                    WallpaperChanger.change(activity, model.getId());
                }
            } catch (IndexOutOfBoundsException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public boolean onItemLongClick(@NonNull View itemView, int position) {
        final Activity activity = getActivity();
        if (activity == null) return false;

        if (!mAdapter.isSelectionMode()) {
            mAdapter.setSelectionMode(true);
            mRefreshLayout.setEnabled(false);
            mActionMode = activity.startActionMode(mCallback);
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

    @Override
    public void onRefresh() {
        final Activity activity = getActivity();
        if (activity == null) return;

        SyncManager.sync(activity);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSyncEvent(@NonNull SyncManager.SyncEvent event) {
        mRefreshLayout.setRefreshing(false);

        mAdapter.notifyDataSetChanged();
        int count = mAdapter.getItemCount();
        if (count == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else if (mEmptyView.getVisibility() == View.VISIBLE) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWallpaperChanged(@NonNull WallpaperChanger.ChangeWallpaperEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    private static final class WallpaperListAdapter extends RecyclerView.Adapter {

        private final List<WallpaperModel> mModels;
        private final List<WallpaperModel> mSelectedModels = new ArrayList<>();
        private final OnItemEventListener mListener;

        private boolean mSelectionMode = false;

        WallpaperListAdapter(@Nullable OnItemEventListener listener) {
            mListener = listener;
            mModels = WallpaperModel.getModels();
            EventBus.getDefault().register(this);
        }

        void clear() {
            EventBus.getDefault().unregister(this);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new WallpaperViewHolder(LayoutInflater.from(parent.getContext()), parent, mListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            WallpaperModel model = mModels.get(position);
            boolean marked = PreferenceModel.getCurrentWallpaper(holder.itemView.getContext()) == model.getId();
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
            mModels.add(event.model);
            mModels.sort(Comparator.comparingLong(WallpaperModel::getId).reversed());
            notifyItemInserted(mModels.indexOf(event.model));
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onUpdated(@NonNull WallpaperModel.UpdateEvent event) {
            int index = mModels.indexOf(event.model);
            if (index > 0) {
                mModels.set(index, event.model);
                notifyItemChanged(index);
            } else {
                LOGGER.warn("{} not founds.", event.model.getId());
            }
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
                if (!model.exists()) {
                    mModels.remove(model);
                    notifyItemRemoved(position);
                    break;
                }
                ++position;
            }
        }

        boolean isSelectionMode() {
            return mSelectionMode;
        }

        void setSelectionMode(boolean selectionMode) {
            if (selectionMode != mSelectionMode) {
                mSelectionMode = selectionMode;
                mSelectedModels.clear();
                notifyDataSetChanged();
            }
        }

        void setSelected(int position) {
            WallpaperModel model = mModels.get(position);
            if (!mSelectedModels.remove(model)) {
                mSelectedModels.add(model);
            }

            notifyDataSetChanged();
        }

        @NonNull
        List<WallpaperModel> getSelectedItems() {
            return mSelectedModels;
        }

        void remove(@NonNull Context context, @NonNull List<WallpaperModel> models) {
            for (WallpaperModel model : models) {
                WallpaperModel.removeModel(model);
            }

            if (PreferenceModel.isSyncEnabled(context)) {
                SyncManager.dismiss(context, models);
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
                itemView.setTag(R.id.synced_view, itemView.findViewById(R.id.synced_view));

                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            void setModel(@NonNull WallpaperModel model, boolean marked) {
                setModel(model, marked, false);
                ((View) itemView.getTag(R.id.check_box)).setVisibility(View.GONE);
            }

            void setModel(WallpaperModel model, boolean marked, boolean selected) {
                final Context context = itemView.getContext();

                final BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(model.getImagePath(), opts);

                Glide.with(context)
                        .load(model.getImagePath())
                        .apply(RequestOptions.sizeMultiplierOf(0.5f)
                                .dontAnimate()
                                .override(opts.outWidth, opts.outHeight)
                                .placeholder(new ColorDrawable(Color.GRAY))
                        )
                        .into((ImageView) itemView.getTag(R.id.image_view));

                View markerView = (View) itemView.getTag(R.id.marker_view);
                markerView.setVisibility(marked ? View.VISIBLE : View.GONE);

                CheckBox checkBox = (CheckBox) itemView.getTag(R.id.check_box);
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selected);

                View syncedView = (View) itemView.getTag(R.id.synced_view);
                syncedView.setVisibility(PreferenceModel.isSyncEnabled(context) && model.isSynced() ? View.VISIBLE : View.GONE);
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
