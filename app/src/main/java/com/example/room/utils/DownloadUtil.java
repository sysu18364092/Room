package com.example.room.utils;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtil {
    private static DownloadUtil downloadUtil ;
    private final OkHttpClient okHttpClient ;

    public static DownloadUtil getDownloadUtil() {
        if (downloadUtil==null){
            downloadUtil = new DownloadUtil();
        }
        return downloadUtil;
    }

    public DownloadUtil() {
        this.okHttpClient = new OkHttpClient();
    }

    /**
     *
     * @param url 下载链接
     * @param saveDir 储存下载文件的目录
     * @param listener 下载监听
     */
    public void download(final String url,final String saveDir,final OnDownloadListener listener){
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null ;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null ;
                //储存下载文件的目录
                //String savePath = isExistDir(saveDir);
                String savePath = saveDir + getNameFromUrl(url);

                try{
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    //File file = new File(savePath,getNameFromUrl(url));
                    File file = new File(savePath);
                    fos = new FileOutputStream(file);
                    long sum = 0 ;
                    while ((len = is.read(buf))!=-1){
                        fos.write(buf,0,len);
                        sum += len;
                        int progress = (int)(sum*1.0f/total*100);

                        listener.onDownloading();
                    }
                    fos.flush();
                    listener.onDownloadSuccess();
                }catch (Exception e){
                    listener.onDownloadFailed();
                }finally {
                    try {
                        if (is!=null)
                            is.close();
                    }catch (IOException e){
                    }
                    try {
                        if (fos!=null)
                            fos.close();
                    }catch (IOException e){
                    }
                }

            }
        });
    }
    /*public String isExistDir(String saveDir) throws IOException{
        //下载位置
        File downloadFile = new File(Environment.getExternalStorageDirectory(),saveDir);
        if (!downloadFile.mkdirs()){
            downloadFile.createNewFile();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }*/
    @NonNull
    public static String getNameFromUrl(String url){
        return url.substring(url.lastIndexOf("/")+1);
    }
    public interface OnDownloadListener{
        void onDownloadSuccess();
        void onDownloading();
        void onDownloadFailed();
    }
}
