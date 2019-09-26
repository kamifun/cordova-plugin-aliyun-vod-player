package aliyun.vod.player;

import android.content.Intent;
import android.util.Log;
import android.util.LogPrinter;

import com.aliyun.vodplayerview.activity.AliyunPlayerSkinActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class AliyunVodPlayer extends CordovaPlugin {
    private static final String TAG = "AliyunVodPlayer";
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("play")) {
            String videoPath = args.getString(0);
            this.play(videoPath, callbackContext);
            return true;
        }
        return false;
    }

    private void play(String videoPath, CallbackContext callbackContext) {
        if (videoPath != null && videoPath != "null" && videoPath.length() > 0) {
            Intent intent = new Intent(cordova.getActivity().getApplicationContext(), AliyunPlayerSkinActivity.class);
            intent.putExtra("videoPath", videoPath);
            cordova.startActivityForResult((CordovaPlugin)this,intent, 7677);
        } else {
            callbackContext.error("无效的视频路径");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 7677) {
            if (intent == null) {
                this.callbackContext.error("返回");
            } else if (intent != null && resultCode == 7777) {
                this.callbackContext.success("回调成功");
            }
        }
    }
}
