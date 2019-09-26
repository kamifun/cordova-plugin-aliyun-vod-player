# cordova-plugin-aliyun-vod-player
阿里云播放器cordova插件，目前只支持android平台

## 安装
cordova plugin add https://github.com/kamifun/cordova-plugin-aliyun-vod-player.git

## 使用方式

```
cordova.plugins.AliyunVodPlayer.play('http://demo-videos.qnsdk.com/movies/qiniu.mp4', ()=>{console.log('播放结束回调')}, ()=>{console.log('手动返回回调')})
```