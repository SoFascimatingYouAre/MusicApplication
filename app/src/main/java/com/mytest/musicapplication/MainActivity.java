package com.mytest.musicapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mytest.musicapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 翻箱子
 * MVVM中的View层
 * 项目较为精短，暂无Model层，大致思路为ViewModel<—相互传递信息—>View<—相互传递信息—>Adapter，而ViewModel层和Adapter不直接交互，一定要经过View层达到交互目的
 * ViewModel层处理所有逻辑，View层只负责修改UI相关的代码，若需要adapter更新则由View进行通知
 * 该项目xml中View的id和视频中有所区别，但足够见名知义
 * 项目视频链接：
 * <a href="https://www.bilibili.com/video/BV1oJ41197fi?p=1&vd_source=764e8a00ab53b6d7aeeb3332b569fdb1">
 * Android实战练习--超简单本地音乐播放器
 * </a>
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "[MusicApplication] " + MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    /**
     * View层代码
     */
    private MainViewModel mainViewModel;

    private MusicItemAdapter adapter;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        //修改为DataBinding的方式setContentView
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //mainViewModel初始化
        mainViewModel = new MainViewModel();
        //设置MainActivity的监听，用于ViewModel层处理逻辑之后修改UI界面，使用成员变量方式设置，原因下方查找
        mainViewModel.setListener(viewModelListener);

        //传入viewModel到xml的data标签，使xml可以绑定使用mainViewModel对象
        binding.setMainViewModel(mainViewModel);

        adapter = new MusicItemAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.rvMusicList.setAdapter(adapter);
        binding.rvMusicList.setLayoutManager(layoutManager);
        setEventListener();
        mCheckPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        //onDestroy主动销毁，省略空异常打印(懒得写)
        if (adapter != null) {
            adapter.setListener(null);
        }
        mainViewModel.onDestroy();
        stopService(intent);
    }

    private void setEventListener() {
        Log.d(TAG, "setEventListener()");
        adapter.setListener(adapterListener);
    }

    /**
     * 建议使用成员变量的方式设置Listener而非视频中的匿名内部类，方便在合适的实际注册和反注册，反注册可以避免内存泄漏
     * 建议按照生命周期对应注册和反注册，如此项目中在onCreate注册了Listener，则需要在onDestroy中反注册，或setListener = null
     * 一般在第二及以后得Activity/Fragment时需要反注册，避免回到第一个view的时候由于没有反注册litener而导致内存泄漏或者空指针崩溃
     */
    private final MusicItemAdapter.MusicItemListener adapterListener = new MusicItemAdapter.MusicItemListener() {
        @Override
        public void onItemCLick(int position) {
            Log.d(TAG, "updateData() -> position = " + position);
            //将position传入用于获取list中的对应bean
            mainViewModel.playNewSong(position);
        }
    };

    private final MainViewModel.ViewModelListener viewModelListener = new MainViewModel.ViewModelListener() {
        @Override
        public void updateData(ArrayList<MusicItemBean> data) {
            Log.d(TAG, "updateData()");
            //不适用set之后直接调用adapter.notifyDataSetChanged(); 以方法的形式在adapter执行自身相关代码——不要在其他类执行非本类的逻辑处理
            adapter.updateData(data);
        }

        @Override
        public void updateMediaItemData(List<MediaBrowserCompat.MediaItem> data) {
            adapter.updateMediaItemData(data);
        }

        @Override
        public void makeMyToast(int msg) {
            Log.d(TAG, "makeMyToast()-> msg = " + msg);
            String message = "";
            switch (msg) {
                case MainViewModel.THIS_IS_FIRST_SONG:
                    message = getResources().getString(R.string.toast_this_is_first_song);
                    break;
                case MainViewModel.THIS_IS_LAST_SONG:
                    message = getResources().getString(R.string.toast_this_is_last_song);
                    break;
                case MainViewModel.CHECK_MUSIC_PLEASE:
                    message = getResources().getString(R.string.toast_check_music_please);
                    break;
            }
            if (!message.isEmpty()) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public Context getContext() {
            Log.d(TAG, "getContext");
            return MainActivity.this;
        }
    };

    /**
     * 加载本地存储当中的音乐文件到集合当中
     * 需要先启动Service再初始化Manager。流程如下：
     * startService --Service.onCreate--> setPlayBackState(NONE)  --addListener-->Manager --initPlayer --player动作-->listener--newState-->Service--setPlayBackState(newState)
     */
    private void initData() {
        intent = new Intent(this, MusicService.class);
        startService(intent);
        //简化View层代码，转移到ViewModel层
        mainViewModel.initData(this, getContentResolver());
    }

    /**
     * 检查权限,动态获取权限
     */
    private void mCheckPermission() {
        List<String> permissionNameList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionNameList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        //经过实验，获取位置信息与蓝牙断开广播无关，可恶的ChatGPT骗我
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            permissionNameList.add(Manifest.permission.ACCESS_FINE_LOCATION);
//            permissionNameList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//        }
        if (!permissionNameList.isEmpty()) {
            Log.e(TAG, "get permission failed");
            int size = permissionNameList.size();
            String[] permissionList = new String[size];
            for (int i = 0; i < size; i++) {
                permissionList[i] = permissionNameList.get(i);
            }
            ActivityCompat.requestPermissions(this, permissionList, 0);
        } else {
            initData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult() -> grantResults = " + Arrays.toString(grantResults));
        initData();
    }
}