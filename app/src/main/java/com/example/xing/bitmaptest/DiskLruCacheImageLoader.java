package com.example.xing.bitmaptest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by xiaoxingxing on 2016/8/3.
 */
public class DiskLruCacheImageLoader {

    private DiskLruCache mDiskLruCache;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 10;//10M的磁盘缓存空间
    private static final int IO_BUFFER_SIZE = 8 * 1024; //缓存区8K

    public DiskLruCacheImageLoader(Context context){
        File diskCacheFile = getDiskCacheDir(context,"diskLruCache");
        //如果文件不存在，则需要手动创建
        if (!diskCacheFile.exists()){
            diskCacheFile.mkdirs();
        }else{
            Log.i("TAG","文件存在" + diskCacheFile.getPath());
        }

        //当磁盘空间大于所需空间的时候，才能进行磁盘缓存
        if(getUsableSpace(diskCacheFile)>DISK_CACHE_SIZE){
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheFile,1,1,DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 写入缓存
     * @param uri
     */
    public void add2DiskLruCache(final String uri){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //将uri的MD5作为key
                String key = hashKeyFormUrl(uri);
                try {
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    if (editor != null){
                        OutputStream out = editor.newOutputStream(0);
                        if (downloadUriToStream(uri,out)){
                            editor.commit();
                        }else{
                            editor.abort();
                        }
                    }
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 读取缓存
     * @param uri
     * @return
     */
    public Bitmap getDiskLruCache(String uri){
        String key = hashKeyFormUrl(uri);
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null){
                InputStream inputStream = snapshot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                return bitmap;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    private boolean downloadUriToStream(String urlString,OutputStream outputStream){
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b;
            while ((b=in.read()) != -1){
                out.write(b);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            try {
                if (in != null){
                    in.close();
                }
                if (out != null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  false;
    }

    /**
     * 获取磁盘缓存的文件
     * @param context
     * @param fileName 文件名称
     * @return
     */
    public File getDiskCacheDir(Context context,String fileName){

        boolean externalStorageAvailable = Environment
                .getExternalStorageDirectory().equals(Environment.MEDIA_MOUNTED);
        //磁盘缓存的绝对路径
        final String cachePath;
        //当SD卡存在的情况下
        if (externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else{
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + fileName);
    }

    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    private String hashKeyFormUrl(String url){
        String cacheKey = "";
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<bytes.length;i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
