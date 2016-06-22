package com.fin10.android.mywallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public final class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private WallpaperAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapter = new WallpaperAdapter(this);

        GridView gridView = (GridView) findViewById(R.id.grid_view);
        gridView.setAdapter(mAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d("[%d] onItemClick", position);
        WallpaperModel model = (WallpaperModel) adapterView.getItemAtPosition(position);
        WallpaperManager wm = WallpaperManager.getInstance(this);
        try {
            wm.setStream(new FileInputStream(model.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final WallpaperModel model = (WallpaperModel) adapterView.getItemAtPosition(position);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Delete ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WallpaperModel.removeModel(model);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create();

        dialog.show();

        return false;
    }

    private static final class WallpaperAdapter extends BaseAdapter implements WallpaperModel.OnEventListener {

        private final LayoutInflater mLayoutInflater;
        private final List<WallpaperModel> mModels;

        public WallpaperAdapter(@NonNull Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            mModels = WallpaperModel.getModels();
            WallpaperModel.addEventListener(this);
        }

        public void clear() {
            WallpaperModel.removeEventListener(this);
        }

        @Override
        public int getCount() {
            return mModels.size();
        }

        @Override
        public Object getItem(int position) {
            return mModels.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.wallpaer_gallery_item, viewGroup, false);
            }

            WallpaperModel model = (WallpaperModel) getItem(position);
            ImageView imgView = (ImageView) convertView;
            Glide.with(convertView.getContext())
                    .load(model.getPath())
                    .centerCrop()
                    .into(imgView);

            return convertView;
        }

        @Override
        public void onAdded(@NonNull WallpaperModel model) {
            mModels.add(model);
            notifyDataSetChanged();
        }

        @Override
        public void onRemoved(@NonNull WallpaperModel model) {
            mModels.remove(model);
            notifyDataSetChanged();
        }
    }
}
