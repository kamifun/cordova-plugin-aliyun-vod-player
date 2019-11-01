var exec = require('cordova/exec');

exports.playWithUrl = function(videoUrl, success, error) {
  exec(success, error, 'AliyunVodPlayer', 'playWithUrl', [videoUrl]);
};
exports.playWithVid = function(params, success, error) {
  var options = Object.assign({}, {
    region: 'cn-shanghai'
  }, params);
  exec(success, error, 'AliyunVodPlayer', 'playWithVid', [options]);
};
