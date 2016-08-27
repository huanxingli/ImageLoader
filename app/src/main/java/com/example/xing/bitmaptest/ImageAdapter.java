package com.example.xing.bitmaptest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by xiaoxingxing on 2016/8/2.
 */
public class ImageAdapter extends BaseAdapter {

    private boolean mIsGridViewIdle = true;
    private ImageLoader imageLoader;

    private List<String> mUrlList;
    private LayoutInflater inflater;
    private Context context;
    private int mImageWidth;

    public ImageAdapter(List<String> list, Context context,int mImageWidth) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        mUrlList = list;
        imageLoader = new ImageLoader(context);
        this.mImageWidth = mImageWidth;
    }

    @Override
    public int getCount() {
        return mUrlList.size();
    }

    @Override
    public String getItem(int position) {
        return mUrlList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null){
            convertView = inflater.inflate(R.layout.image_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image1);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ImageView imageView = viewHolder.imageView;
        final String tag = (String) imageView.getTag();
        final String uri = getItem(position);
        if (!uri.equals(tag)){
            imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
        }

        if (mIsGridViewIdle){
            imageView.setTag(uri);
            imageLoader.bindBitmap(uri,imageView,mImageWidth,mImageWidth);
        }
        return convertView;
    }

    private static class ViewHolder{
        public ImageView imageView;
    }
}
