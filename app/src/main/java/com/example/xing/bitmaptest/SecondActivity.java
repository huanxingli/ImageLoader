package com.example.xing.bitmaptest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

/**
 * Created by xiaoxingxing on 2016/8/4.
 */
public class SecondActivity extends Activity {

    private DiskLruCacheImageLoader diskLruCacheImageLoader;
    private static final String URL = "http://img.my.csdn.net/uploads/201407/26/1406383242_3127.jpg";
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        imageView = (ImageView) findViewById(R.id.image2);
        diskLruCacheImageLoader = new DiskLruCacheImageLoader(this);
        diskLruCacheImageLoader.add2DiskLruCache(URL);
        imageView.setImageBitmap(diskLruCacheImageLoader.getDiskLruCache(URL));
    }
}
