package com.example.xing.bitmaptest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaoxingxing on 2016/8/1.
 */
public class ImageLoader {

    private Context mContext;
    public static final int MESSAGE_POST_RESULT = 1;
    private static final int DISK_CACHE_INDEX = 0;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;//50M的磁盘缓存空间
    private static final int IO_BUFFER_SIZE = 8 * 1024; //缓存区8K
    private boolean mIsDiskLruCacheCreated = false;//标志是否有创建磁盘缓存
    private static final int TAG_KEY_URI = R.id.image1;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();//CPU核心数
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;//核心线程数
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 +1;//线程池的最大线程数
    private static final long KEEP_ALIVE = 10L;//非核心线程在闲置时的超时时间为10秒

    //线程工厂
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"ImageLoader#" + mCount.getAndIncrement());
        }
    };

    //线程池
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE,
            TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(),sThreadFactory
    );


    private ImageResizer mImageResizer = new ImageResizer();

    private LruCache<String,Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private Handler mMainHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            String uri = (String) imageView.getTag(TAG_KEY_URI);
            if (uri.equals(result.uri)){
                imageView.setImageBitmap(result.bitmap);
            }

        }
    };

    public ImageLoader(Context context){
        mContext = context;
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext,"bitmap");
        if (!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }

        //如果磁盘剩余空间小于磁盘缓存所需的大小
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //内存缓存的添加
    private void addBitmap2MemoryCache(String key,Bitmap bitmap){
        if (getBitmapFromMemoryCache(key)==null){
            mMemoryCache.put(key,bitmap);
        }
    }

    //内存缓存的获取
    private Bitmap getBitmapFromMemoryCache(String key){
        return mMemoryCache.get(key);
    }

    /**
     * 同步加载图片,步骤如下：
     * 1.尝试从内存缓存读取
     * 2.尝试从磁盘缓存读取
     * 3.最后才从网络上拉去图片
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public Bitmap loadBitmap(String url,int reqWidth,int reqHeight){
        Bitmap bitmap = loadBitmapFromMemCache(url,reqWidth,reqHeight);
        if (bitmap != null){
            return bitmap;
        }

        try {
            bitmap = loadBitmapFromDiskCache(url,reqWidth,reqHeight);
            if (bitmap != null){
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url,reqWidth,reqHeight);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //图片为空，而且并没有创建磁盘缓存，则直接从网络上下载
        //虽然loadBitmapFromHttp()也是有downBitmapFromUrl,但是是建立在磁盘缓存存在的情况下
        if (bitmap == null && !mIsDiskLruCacheCreated){
            bitmap = downBitmapFromUrl(url);
        }
        return bitmap;
    }

    /**
     * 异步加载图片
     * @param uri
     * @param imageView
     * @param reqWidth
     * @param reqHeight
     */
    public void bindBitmap(final String uri, final ImageView imageView,
                            final int reqWidth, final int reqHeight){
        imageView.setTag(TAG_KEY_URI,uri);
        Bitmap bitmap = loadBitmapFromMemCache(uri,reqWidth,reqHeight);
        if (bitmap != null){
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(uri,reqWidth,reqHeight);
                if (bitmap != null){
                    LoaderResult result = new LoaderResult(imageView,uri,bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget();
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }


    private Bitmap loadBitmapFromMemCache(String url,int reqWidth,int reqHeight){
        String key = hashKeyFormUrl(url);
        Bitmap bitmap = getBitmapFromMemoryCache(key);
        return bitmap;
    }


    //从网络上获取图片，并添加到本地中
    public Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{
        //网络操作不能在主线程上进行
        if (Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("can not visit network from UI Thread");
        }

        if (mDiskLruCache == null){
            return null;
        }
        //把url转化为key,一般是url的MD5
        String key = hashKeyFormUrl(url);
        //磁盘缓存的添加都是通过Editor完成的，它表示一个缓存对象的编辑对象
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            //访问url,并且通过editor的outputStream存入本地
            if (downloadUrlToStream(url,outputStream)){
                editor.commit();
            }else{
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url,reqWidth,reqHeight);
    }

    public Bitmap loadBitmapFromDiskCache(String url,int reqWidth,int reqHeight) throws IOException{
        if (Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("can not visit network from UI Thread");
        }
        if (mDiskLruCache == null){
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKeyFormUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(
                    DISK_CACHE_INDEX);
            //获取文件描述符
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampleFromFileDescriptor(fileDescriptor,reqWidth,reqHeight);
            if (bitmap != null){
                addBitmap2MemoryCache(key,bitmap);
            }
        }
        return bitmap;
    }

    private boolean downloadUrlToStream(String urlString ,OutputStream outputStream){
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1){
                out.write(b);
            }
            return true;

        } catch (IOException e) {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            try {
                if (out != null){
                    out.close();
                }
                if (in != null){
                    in.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    //直接从网络上获取图片
    private Bitmap downBitmapFromUrl(String urlString){
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bitmap;
    }


    private String hashKeyFormUrl(String url){
        String cacheKey = "";
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = String.valueOf(url.hashCode());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }


    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    public File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

    private static class LoaderResult{
        public ImageView imageView;
        public String uri;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView,String uri,Bitmap bitmap){
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }


}
