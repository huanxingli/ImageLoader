package com.example.xing.bitmaptest;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

/**
 * Created by xiaoxingxing on 2016/8/1.
 */
public class ImageResizer {
    public ImageResizer(){}

    public Bitmap decodeSampleFromResource(Resources res,int resId,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        //此时，BitmapFactory只会解析图片的原始高度和宽度
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res,resId,options);

        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        //此时就会根据inSample来加载
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeResource(res,resId,options);

    }

    public Bitmap decodeSampleFromFileDescriptor(FileDescriptor fd,int reqWidth,int reqHeight){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd,null,options);
        options.inJustDecodeBounds = false;
        return  BitmapFactory.decodeFileDescriptor(fd,null,options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        //原图片的真实高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        //当图片高度或者宽度大的话，就要进行压缩
        if (height>reqHeight || width>reqWidth){
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            while ((halfWidth / inSampleSize)>=reqWidth && (halfHeight / inSampleSize)>=reqHeight){
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
