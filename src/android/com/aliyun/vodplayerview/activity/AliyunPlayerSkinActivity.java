package com.aliyun.vodplayerview.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorCode;
import com.aliyun.player.nativeclass.MediaInfo;
import com.aliyun.player.nativeclass.PlayerConfig;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidSts;
import com.aliyun.private_service.PrivateService;
import com.aliyun.svideo.common.utils.ToastUtils;
import com.aliyun.utils.VcPlayerLog;
import cn.com.Timekey.EasyHospital.R;
import com.aliyun.vodplayerview.constants.PlayParameter;
import com.aliyun.vodplayerview.listener.OnChangeQualityListener;
import com.aliyun.vodplayerview.listener.OnStoppedListener;
import com.aliyun.vodplayerview.listener.RefreshStsCallback;
// import com.aliyun.vodplayerview.utils.Common;
import com.aliyun.vodplayerview.utils.FixedToastUtils;
import com.aliyun.vodplayerview.utils.ScreenUtils;
import com.aliyun.vodplayerview.utils.VidStsUtil;
import com.aliyun.vodplayerview.view.choice.AlivcShowMoreDialog;
import com.aliyun.vodplayerview.view.control.ControlView;
import com.aliyun.vodplayerview.view.gesturedialog.BrightnessDialog;
import com.aliyun.vodplayerview.view.more.AliyunShowMoreValue;
import com.aliyun.vodplayerview.view.more.ShowMoreView;
import com.aliyun.vodplayerview.view.more.SpeedValue;
import com.aliyun.vodplayerview.view.tipsview.ErrorInfo;
import com.aliyun.vodplayerview.widget.AliyunScreenMode;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView.OnPlayerViewClickListener;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView.PlayViewType;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 播放器和播放列表界面 Created by Mulberry on 2018/4/9.
 */
public class AliyunPlayerSkinActivity extends BaseActivity {

    private PlayerHandler playerHandler;
    private AlivcShowMoreDialog showMoreDialog;

    private AliyunScreenMode currentScreenMode = AliyunScreenMode.Small;

    private AliyunVodPlayerView mAliyunVodPlayerView = null;

    private ErrorInfo currentError = ErrorInfo.Normal;
    //判断是否在后台
    private boolean mIsInBackground = false;
    /**
     * 开启设置界面的请求码
     */
    private static final int CODE_REQUEST_SETTING = 1000;
    private static final String DEFAULT_VID = "8bb9b7d5c7c64cf49d51fa808b1f0957";
    /**
     * get StsToken stats
     */
    private boolean inRequest;

    private long oldTime;

    /**
     * 是否鉴权过期
     */
    private boolean mIsTimeExpired = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (isStrangePhone()) {
            //            setTheme(R.style.ActTheme);
        } else {
            setTheme(R.style.NoActionTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.alivc_player_layout_skin);

        requestVidSts();
        PlayParameter.PLAY_PARAM_URL = getIntent().getStringExtra("videoPath");
        initAliyunPlayerView();
    }

    /**
     * 设置屏幕亮度
     */
    private void setWindowBrightness(int brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255.0f;
        window.setAttributes(lp);
    }


    private void initAliyunPlayerView() {
        mAliyunVodPlayerView = (AliyunVodPlayerView) findViewById(R.id.video_view);
        //保持屏幕敞亮
        mAliyunVodPlayerView.setKeepScreenOn(true);
        String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save_cache";
        mAliyunVodPlayerView.setPlayingCache(false, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/);
        mAliyunVodPlayerView.setTheme(AliyunVodPlayerView.Theme.Blue);
        //mAliyunVodPlayerView.setCirclePlay(true);
        mAliyunVodPlayerView.setAutoPlay(true);

        mAliyunVodPlayerView.setOnPreparedListener(new MyPrepareListener(this));
        mAliyunVodPlayerView.setNetConnectedListener(new MyNetConnectedListener(this));
        mAliyunVodPlayerView.setOnCompletionListener(new MyCompletionListener(this));
        mAliyunVodPlayerView.setOnFirstFrameStartListener(new MyFrameInfoListener(this));
        mAliyunVodPlayerView.setOnChangeQualityListener(new MyChangeQualityListener(this));
        //TODO
        mAliyunVodPlayerView.setOnStoppedListener(new MyStoppedListener(this));
        mAliyunVodPlayerView.setmOnPlayerViewClickListener(new MyPlayViewClickListener(this));
        mAliyunVodPlayerView.setOrientationChangeListener(new MyOrientationChangeListener(this));
//        mAliyunVodPlayerView.setOnUrlTimeExpiredListener(new MyOnUrlTimeExpiredListener(this));
        mAliyunVodPlayerView.setOnTimeExpiredErrorListener(new MyOnTimeExpiredErrorListener(this));
        mAliyunVodPlayerView.setOnShowMoreClickListener(new MyShowMoreClickLisener(this));
        mAliyunVodPlayerView.setOnPlayStateBtnClickListener(new MyPlayStateBtnClickListener(this));
        mAliyunVodPlayerView.setOnSeekCompleteListener(new MySeekCompleteListener(this));
        mAliyunVodPlayerView.setOnSeekStartListener(new MySeekStartListener(this));
        mAliyunVodPlayerView.setOnScreenBrightness(new MyOnScreenBrightnessListener(this));
        mAliyunVodPlayerView.setOnErrorListener(new MyOnErrorListener(this));
        mAliyunVodPlayerView.setScreenBrightness(BrightnessDialog.getActivityBrightness(AliyunPlayerSkinActivity.this));
        mAliyunVodPlayerView.enableNativeLog();
        setPlaySource();
    }

    /**
     * 请求sts
     */
    private void requestVidSts() {
        Log.e("scar", "requestVidSts: ");
        if (inRequest) {
            return;
        }
        inRequest = true;
        if (TextUtils.isEmpty(PlayParameter.PLAY_PARAM_VID)) {
            PlayParameter.PLAY_PARAM_VID = DEFAULT_VID;
        }
        Log.e("scar", "requestVidSts:xx ");
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new MyStsListener(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // loadPlayList();
        //vid/url设置界面并且是取消
        if (requestCode == CODE_REQUEST_SETTING && resultCode == Activity.RESULT_CANCELED) {
            return;
        } else if (requestCode == CODE_REQUEST_SETTING && resultCode == Activity.RESULT_OK) {
            setPlaySource();
        }
    }

    private int currentVideoPosition;

    /**
     * 播放本地资源
     */
    private void changePlayLocalSource(String url, String title) {
        UrlSource urlSource = new UrlSource();
        urlSource.setUri(url);
        urlSource.setTitle(title);
        mAliyunVodPlayerView.setLocalSource(urlSource);
    }

    private static class MyPrepareListener implements IPlayer.OnPreparedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyPrepareListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<>(skinActivity);
        }

        @Override
        public void onPrepared() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onPrepared();
            }
        }
    }

    private void onPrepared() {
//        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_prepare_success);
    }

    private static class MyCompletionListener implements IPlayer.OnCompletionListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyCompletionListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onCompletion() {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onCompletion();
            }
        }
    }

    private void onCompletion() {
        Intent intent = new Intent();
        setResult(7777, intent);
        finish();
//        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_play_compleion);
    }

    private static class MyFrameInfoListener implements IPlayer.OnRenderingStartListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyFrameInfoListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onRenderingStart() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onFirstFrameStart();
            }
        }
    }

    private void onFirstFrameStart() {
        if (mAliyunVodPlayerView != null) {
            Map<String, String> debugInfo = mAliyunVodPlayerView.getAllDebugInfo();
            if (debugInfo == null) {
                return;
            }
            long createPts = 0;
            if (debugInfo.get("create_player") != null) {
                String time = debugInfo.get("create_player");
                createPts = (long) Double.parseDouble(time);
                // logStrs.add(format.format(new Date(createPts)) + getString(R.string.log_player_create_success));
            }
            if (debugInfo.get("open-url") != null) {
                String time = debugInfo.get("open-url");
                long openPts = (long) Double.parseDouble(time) + createPts;
                // logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_open_url_success));
            }
            if (debugInfo.get("find-stream") != null) {
                String time = debugInfo.get("find-stream");
                long findPts = (long) Double.parseDouble(time) + createPts;
                // logStrs.add(format.format(new Date(findPts)) + getString(R.string.log_request_stream_success));
            }
            if (debugInfo.get("open-stream") != null) {
                String time = debugInfo.get("open-stream");
                long openPts = (long) Double.parseDouble(time) + createPts;
                // logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_start_open_stream));
            }
        }
    }

    private class MyPlayViewClickListener implements OnPlayerViewClickListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyPlayViewClickListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onClick(AliyunScreenMode screenMode, PlayViewType viewType) {
            long currentClickTime = System.currentTimeMillis();
            // 防止快速点击
            if (currentClickTime - oldTime <= 1000) {
                return;
            }
            oldTime = currentClickTime;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static class MyChangeQualityListener implements OnChangeQualityListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyChangeQualityListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onChangeQualitySuccess(String finalQuality) {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualitySuccess(finalQuality);
            }
        }

        @Override
        public void onChangeQualityFail(int code, String msg) {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualityFail(code, msg);
            }
        }
    }

    private void onChangeQualitySuccess(String finalQuality) {
        // logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_success));
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(),
                getString(R.string.log_change_quality_success));
    }

    void onChangeQualityFail(int code, String msg) {
        // logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_fail) + " : " + msg);
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(),
                getString(R.string.log_change_quality_fail));
    }

    private static class MyStoppedListener implements OnStoppedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyStoppedListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onStop() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onStopped();
            }
        }
    }

    private static class MyRefreshStsCallback implements RefreshStsCallback {

        @Override
        public VidSts refreshSts(String vid, String quality, String format, String title, boolean encript) {
            VcPlayerLog.d("refreshSts ", "refreshSts , vid = " + vid);
            //NOTE: 注意：这个不能启动线程去请求。因为这个方法已经在线程中调用了。
            VidSts vidSts = VidStsUtil.getVidSts(vid);
            if (vidSts == null) {
                return null;
            } else {
                vidSts.setVid(vid);
                vidSts.setQuality(quality, true);
                vidSts.setTitle(title);
                return vidSts;
            }
        }
    }

    private void onStopped() {
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.log_play_stopped);
    }

    private void setPlaySource() {
        if ("localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            UrlSource urlSource = new UrlSource();
            urlSource.setUri(PlayParameter.PLAY_PARAM_URL);
            //默认是5000
            int maxDelayTime = 5000;
            if (PlayParameter.PLAY_PARAM_URL.startsWith("artp")) {
                //如果url的开头是artp，将直播延迟设置成100，
                maxDelayTime = 100;
            }
            if (mAliyunVodPlayerView != null) {
                PlayerConfig playerConfig = mAliyunVodPlayerView.getPlayerConfig();
                playerConfig.mMaxDelayTime = maxDelayTime;
                mAliyunVodPlayerView.setPlayerConfig(playerConfig);
                mAliyunVodPlayerView.setLocalSource(urlSource);
            }

        } else if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            if (!inRequest) {
                VidSts vidSts = new VidSts();
                vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
                vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
                vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
                vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
                vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView.setVidSts(vidSts);
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mIsInBackground = false;
        updatePlayerViewMode();
        // updateDownloadView();
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(true);
            mAliyunVodPlayerView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsInBackground = true;
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(false);
            mAliyunVodPlayerView.onStop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePlayerViewMode();
    }

    private void updatePlayerViewMode() {
        if (mAliyunVodPlayerView != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //转为竖屏了。
                //显示状态栏
                //                if (!isStrangePhone()) {
                //                    getSupportActionBar().show();
                //                }

                this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                //设置view的布局，宽高之类
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams) mAliyunVodPlayerView
                        .getLayoutParams();
                aliVcVideoViewLayoutParams.height = (int) (ScreenUtils.getWidth(this) * 9.0f / 16);
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //转到横屏了。
                //隐藏状态栏
                if (!isStrangePhone()) {
                    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                //设置view的布局，宽高
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams) mAliyunVodPlayerView
                        .getLayoutParams();
                aliVcVideoViewLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.onDestroy();
            mAliyunVodPlayerView = null;
        }

        if (playerHandler != null) {
            playerHandler.removeMessages(DOWNLOAD_ERROR);
            playerHandler = null;
        }

        super.onDestroy();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAliyunVodPlayerView != null) {
            boolean handler = mAliyunVodPlayerView.onKeyDown(keyCode, event);
            if (!handler) {
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //解决某些手机上锁屏之后会出现标题栏的问题。
        updatePlayerViewMode();
    }

    private static final int DOWNLOAD_ERROR = 1;
    private static final String DOWNLOAD_ERROR_KEY = "error_key";

    private static class PlayerHandler extends Handler {
        //持有弱引用AliyunPlayerSkinActivity,GC回收时会被回收掉.
        private final WeakReference<AliyunPlayerSkinActivity> mActivty;

        public PlayerHandler(AliyunPlayerSkinActivity activity) {
            mActivty = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AliyunPlayerSkinActivity activity = mActivty.get();
            super.handleMessage(msg);
            if (activity != null) {
                switch (msg.what) {
                    case DOWNLOAD_ERROR:
                        ToastUtils.show(activity,msg.getData().getString(DOWNLOAD_ERROR_KEY));
                        Log.d("donwload", msg.getData().getString(DOWNLOAD_ERROR_KEY));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static class MyStsListener implements VidStsUtil.OnStsResultListener {

        private WeakReference<AliyunPlayerSkinActivity> weakActivity;

        MyStsListener(AliyunPlayerSkinActivity act) {
            weakActivity = new WeakReference<>(act);
        }

        @Override
        public void onSuccess(String vid, final String akid, final String akSecret, final String token) {
            AliyunPlayerSkinActivity activity = weakActivity.get();
            if (activity != null) {
                activity.onStsSuccess(vid, akid, akSecret, token);
            }
        }

        @Override
        public void onFail() {
            AliyunPlayerSkinActivity activity = weakActivity.get();
            if (activity != null) {
                activity.onStsFail();
            }
        }
    }

    private void onStsFail() {

        //FixedToastUtils.show(getApplicationContext(), R.string.request_vidsts_fail);
        inRequest = false;
        //finish();
    }

    private void onStsSuccess(String mVid, String akid, String akSecret, String token) {
        PlayParameter.PLAY_PARAM_VID = mVid;
        PlayParameter.PLAY_PARAM_AK_ID = akid;
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token;

        mIsTimeExpired = false;

        inRequest = false;
    }

    private static class MyOrientationChangeListener implements AliyunVodPlayerView.OnOrientationChangeListener {

        private final WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOrientationChangeListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void orientationChange(boolean from, AliyunScreenMode currentMode) {
            AliyunPlayerSkinActivity activity = weakReference.get();

            if (activity != null) {
                // activity.hideDownloadDialog(from, currentMode);
                activity.hideShowMoreDialog(from, currentMode);

            }
        }
    }

    private void hideShowMoreDialog(boolean from, AliyunScreenMode currentMode) {
        if (showMoreDialog != null) {
            if (currentMode == AliyunScreenMode.Small) {
                showMoreDialog.dismiss();
                currentScreenMode = currentMode;
            }
        }
    }

    /**
     * 判断是否有网络的监听
     */
    private class MyNetConnectedListener implements AliyunVodPlayerView.NetConnectedListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyNetConnectedListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onReNetConnected(boolean isReconnect) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onReNetConnected(isReconnect);
            }
        }

        @Override
        public void onNetUnConnected() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onNetUnConnected();
            }
        }
    }

    private void onNetUnConnected() {
        currentError = ErrorInfo.UnConnectInternet;
    }

    private void onReNetConnected(boolean isReconnect) {
        currentError = ErrorInfo.Normal;
        if (isReconnect) {
        }
    }

    /**
     * 因为鉴权过期,而去重新鉴权
     */
    private static class RetryExpiredSts implements VidStsUtil.OnStsResultListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public RetryExpiredSts(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(String vid, String akid, String akSecret, String token) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onStsRetrySuccess(vid, akid, akSecret, token);
            }
        }

        @Override
        public void onFail() {

        }
    }

    private void onStsRetrySuccess(String mVid, String akid, String akSecret, String token) {
        PlayParameter.PLAY_PARAM_VID = mVid;
        PlayParameter.PLAY_PARAM_AK_ID = akid;
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token;

        inRequest = false;
        mIsTimeExpired = false;

        VidSts vidSts = new VidSts();
        vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
        vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
        vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
        vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);

        mAliyunVodPlayerView.setVidSts(vidSts);
    }

    public static class MyOnTimeExpiredErrorListener implements AliyunVodPlayerView.OnTimeExpiredErrorListener {

        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnTimeExpiredErrorListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onTimeExpiredError() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onTimExpiredError();
            }
        }
    }

    private void onUrlTimeExpired(String oldVid, String oldQuality) {
        //requestVidSts();
        VidSts vidSts = VidStsUtil.getVidSts(oldVid);
        PlayParameter.PLAY_PARAM_VID = vidSts.getVid();
        PlayParameter.PLAY_PARAM_AK_SECRE = vidSts.getAccessKeySecret();
        PlayParameter.PLAY_PARAM_AK_ID = vidSts.getAccessKeyId();
        PlayParameter.PLAY_PARAM_SCU_TOKEN = vidSts.getSecurityToken();

        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setVidSts(vidSts);
        }
    }

    /**
     * 鉴权过期
     */
    private void onTimExpiredError() {
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new RetryExpiredSts(this));
    }

    private static class MyShowMoreClickLisener implements ControlView.OnShowMoreClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyShowMoreClickLisener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void showMore() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                long currentClickTime = System.currentTimeMillis();
                // 防止快速点击
                if (currentClickTime - activity.oldTime <= 1000) {
                    return;
                }
                activity.oldTime = currentClickTime;
                activity.showMore(activity);
                activity.requestVidSts();
            }

        }
    }

    private void showMore(final AliyunPlayerSkinActivity activity) {
        showMoreDialog = new AlivcShowMoreDialog(activity);
        AliyunShowMoreValue moreValue = new AliyunShowMoreValue();
        moreValue.setSpeed(mAliyunVodPlayerView.getCurrentSpeed());
        moreValue.setVolume((int) mAliyunVodPlayerView.getCurrentVolume());

        ShowMoreView showMoreView = new ShowMoreView(activity, moreValue);
        showMoreDialog.setContentView(showMoreView);
        showMoreDialog.show();

        showMoreView.setOnSpeedCheckedChangedListener(new ShowMoreView.OnSpeedCheckedChangedListener() {
            @Override
            public void onSpeedChanged(RadioGroup group, int checkedId) {
                // 点击速度切换
                if (checkedId == R.id.rb_speed_normal) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.One);
                } else if (checkedId == R.id.rb_speed_onequartern) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneQuartern);
                } else if (checkedId == R.id.rb_speed_onehalf) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneHalf);
                } else if (checkedId == R.id.rb_speed_twice) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.Twice);
                }

            }
        });

        /**
         * 初始化亮度
         */
        if (mAliyunVodPlayerView != null) {
            showMoreView.setBrightness(mAliyunVodPlayerView.getScreenBrightness());
        }
        // 亮度seek
        showMoreView.setOnLightSeekChangeListener(new ShowMoreView.OnLightSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                setWindowBrightness(progress);
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView.setScreenBrightness(progress);
                }
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

        /**
         * 初始化音量
         */
        if (mAliyunVodPlayerView != null) {
            showMoreView.setVoiceVolume(mAliyunVodPlayerView.getCurrentVolume());
        }
        showMoreView.setOnVoiceSeekChangeListener(new ShowMoreView.OnVoiceSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                mAliyunVodPlayerView.setCurrentVolume(progress / 100.00f);
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

    }

    /**
     * 获取url的scheme
     *
     * @param url
     * @return
     */
    private String getUrlScheme(String url) {
        return Uri.parse(url).getScheme();
    }

    private static class MyPlayStateBtnClickListener implements AliyunVodPlayerView.OnPlayStateBtnClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyPlayStateBtnClickListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onPlayBtnClick(int playerState) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onPlayStateSwitch(playerState);
            }
        }
    }

    /**
     * 播放状态切换
     */
    private void onPlayStateSwitch(int playerState) {
        if (playerState == IPlayer.started) {
            // tvLogs.append(format.format(new Date()) + " 暂停 \n");
        } else if (playerState == IPlayer.paused) {
            // tvLogs.append(format.format(new Date()) + " 开始 \n");
        }

    }

    private static class MySeekCompleteListener implements IPlayer.OnSeekCompleteListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekCompleteListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekComplete() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekComplete();
            }
        }
    }

    private void onSeekComplete() {
        // tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_completed) + "\n");
    }

    private static class MySeekStartListener implements AliyunVodPlayerView.OnSeekStartListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekStartListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekStart(int position) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekStart(position);
            }
        }
    }

    private static class MyOnFinishListener implements AliyunVodPlayerView.OnFinishListener {

        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnFinishListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onFinishClick() {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.finish();
            }
        }
    }

    private static class MyOnScreenBrightnessListener implements AliyunVodPlayerView.OnScreenBrightnessListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnScreenBrightnessListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onScreenBrightness(int brightness) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.setWindowBrightness(brightness);
                if (aliyunPlayerSkinActivity.mAliyunVodPlayerView != null) {
                    aliyunPlayerSkinActivity.mAliyunVodPlayerView.setScreenBrightness(brightness);
                }
            }
        }
    }

    /**
     * 播放器出错监听
     */
    private static class MyOnErrorListener implements IPlayer.OnErrorListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnErrorListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onError(com.aliyun.player.bean.ErrorInfo errorInfo) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.onError(errorInfo);
            }
        }
    }

    private void onError(com.aliyun.player.bean.ErrorInfo errorInfo) {
        //鉴权过期
        if (errorInfo.getCode().getValue() == ErrorCode.ERROR_SERVER_POP_UNKNOWN.getValue()) {
            mIsTimeExpired = true;
        }
    }

    private void onSeekStart(int position) {
        // tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_start) + "\n");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getKeyCode() == 67) {
            if (mAliyunVodPlayerView != null) {
                //删除按键监听,部分手机在EditText没有内容时,点击删除按钮会隐藏软键盘
                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

}
