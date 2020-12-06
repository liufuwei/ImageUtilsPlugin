package com.bingo.imageUtilsPlugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class echoes a string called from JavaScript.
 */
public class ImageUtilsPlugin extends CordovaPlugin {

    private String hostPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //参数格式
        //[["file:///storage/emulated/0/ImageCache/IMG_20190923_162219.jpg","file:///storage/emulated/0/ImageCache/IMG_20190923_162219.jpg"],"imageFilePath",600]
        if ("compressImage".equalsIgnoreCase(action)) {//调用压缩方法
            try {
                //图片地址集合
                JSONArray requestArray = args.getJSONArray(0);
                //图片存储的文件名
                String tempFileName = args.getString(1);
                //图片压缩至不大于这个数值
                int fileSize = args.getInt(2);

                JSONArray responeArray = new JSONArray();
                for (int i = 0; i < requestArray.length(); i++) {
                    String filePath = requestArray.get(i).toString();
                    String newPath = hostPath + tempFileName + File.separator + this.getLastName(filePath);

                    filePath = filePath.replace("file://", "");
                    filePath = filePath.replace("file:", "");//防止没有双斜杠
                    this.saveBitmap(filePath,newPath,fileSize);

                    responeArray.put("file://"+newPath);
                }
                callbackContext.success(responeArray);
            } catch (Exception e) {
                callbackContext.error(e.getMessage());
            }
            return true;
        }
        return false;
    }

	 /**
     * 获取最后的文件名
     *
     * @param filePath 文件路径
     * @return
     */
    private static String getLastName(String filePath) {
        String[] path = filePath.split(File.separator);
        if (path.length > 1) {
            return path[path.length - 1];
        }
        return null;
    }

    /**
     * 从本地path中获取bitmap，压缩后保存小图片到本地
     *
     * @param path 图片存放的路径
     * @param tagPath 图标保存的路径
     * @param fileSize 图标期望压缩至不大于这尺寸
     */
    public static void saveBitmap(String path,String tagPath,int fileSize) {
        String compressdPicPath = "";
        //设定尺寸压缩
        Bitmap bitmap = decodeSampledBitmapFromPath(path, 1200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        while (baos.toByteArray().length / 1024 > fileSize) {

            baos.reset();
            options -= 5;
            if (options < 6) {//为了防止图片大小一直达不到200kb，options一直在递减，当options<0时，下面的方法会报错
                // 也就是说即使达不到200kb，也就压缩到10了
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
                break;
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }

        File file = new File(tagPath);//将要保存图片的路径
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(baos.toByteArray());
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据图片要显示的宽和高，对图片进行压缩，避免OOM
     *
     * @param path
     * @param minSize 裁剪后的目标尺寸
     * @return
     */
    private static Bitmap decodeSampledBitmapFromPath(String path, int minSize) {

//      获取图片的宽和高，并不把他加载到内存当中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caculateInSampleSize(options, minSize);
//      使用获取到的inSampleSize再次解析图片(此时options里已经含有压缩比 options.inSampleSize，再次解析会得到压缩后的图片，不会oom了 )
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;

    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     *
     * @param options
     * @param minSize 裁剪后的目标尺寸
     * @return
     * @compressExpand 这个值是为了像预览图片这样的需求，他要比所要显示的imageview高宽要大一点，放大才能清晰
     */
    private static int caculateInSampleSize(BitmapFactory.Options options, int minSize) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        int size = width > height ? height : width;
        if(size>minSize){
            inSampleSize = Math.round(size * 1.0f / minSize);
        }

        return inSampleSize;
    }

}
