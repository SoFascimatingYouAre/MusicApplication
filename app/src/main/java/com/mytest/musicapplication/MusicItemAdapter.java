package com.mytest.musicapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.mytest.musicapplication.databinding.MusicItemLayoutBinding;

import java.util.List;

public class MusicItemAdapter extends RecyclerView.Adapter<MusicItemAdapter.MusicItemAdapterViewHolder> {

    private static final String TAG = "[MusicApplication] " + MusicItemAdapter.class.getSimpleName();

    private Context context;
    private List<MusicItemBean> data;
    MusicItemLayoutBinding binding;
    private MusicItemListener listener;

    public void setListener(MusicItemListener listener) {
        this.listener = listener;
    }

    public MusicItemAdapter(Context context) {
        this.context = context;
    }
    
    public void updateData(List<MusicItemBean> data) {
        Log.d(TAG, "updateData");
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicItemAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "MusicItemAdapterViewHolder");
//        LayoutInflater inflater = LayoutInflater.from(context);
//        binding = MusicItemLayoutBinding.inflate(inflater, parent, false);
//        return new MusicItemAdapterViewHolder(binding.getRoot());

        binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.music_item_layout, parent, false);
        return new MusicItemAdapterViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicItemAdapterViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");
        MusicItemBean musicItemBean = data.get(position);
        Log.d(TAG, "aid = " + musicItemBean.getId() + " position = " + position);
        holder.binding.txAllMusicNum.setText(musicItemBean.getId());
        holder.binding.tvSongName.setText(musicItemBean.getName());
        holder.binding.tvSongSinger.setText(musicItemBean.getSinger());
        holder.binding.tvAlbum.setText(musicItemBean.getAlbum());
        holder.binding.tvTime.setText(musicItemBean.getTime());

        //此处修改为lambda表达式，其实和new View.OnClickListener()没区别，取消注释会导致上方onBindViewHolder的int position出现红色提示，所以修改
        holder.binding.cvCard.setOnClickListener(v -> {
            if (position < 0) {
                //在布局加载没有完全成功时会出现点击时收到的position为-1，会立刻出现非法参数异常导致崩溃
                Log.e(TAG, "onBindViewHolder()-> onClick, position = " + position + " is ERROR!");
                return;
            }
            Log.d(TAG, "onBindViewHolder()-> onClick, position = " + position);
            listener.onItemCLick(position);
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount");
        int size = data.size();
        Log.d(TAG, "size = " + size);
        return size;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    class MusicItemAdapterViewHolder extends RecyclerView.ViewHolder {
        private MusicItemLayoutBinding binding;
        public MusicItemAdapterViewHolder(@NonNull MusicItemLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * 此部分是首次使用，bind一行报错，无法使用
     * 似乎是因为bind方法是获取xml中<data>修饰的内容，而不返回MusicItemLayoutBinding导致
     */
//    class MusicItemAdapterViewHolder extends RecyclerView.ViewHolder {
//
//        MusicItemLayoutBinding binding;
//
//        public MusicItemAdapterViewHolder(View itemView) {
//            super(itemView);
//            binding = MusicItemLayoutBinding.bind(itemView);
//        }
//    }

    public interface MusicItemListener {
        /**
         * 点击适配器中的某个item
         * @param position 下标
         */
        void onItemCLick(int position);
    }
}
