package com.example.xing.bitmaptest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by xiaoxingxing on 2016/8/2.
 */
public class SquareImageView extends ImageView {
    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
