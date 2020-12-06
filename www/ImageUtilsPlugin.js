var exec = require('cordova/exec');

exports.compressImage = function (arg0, success, error) {
    exec(success, error, 'ImageUtilsPlugin', 'compressImage', arg0);
};
