package com.cntt2.flashcard.ui.animation;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        // Khi position == 0: Trang ở giữa, không thay đổi
        float scale = Math.max(0.85f, 1 - Math.abs(position));
        float alpha = Math.max(0.5f, 1 - Math.abs(position));
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setAlpha(alpha);
    }
}
