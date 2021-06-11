package com.example.room.utils;

import android.view.View;

/**
 *  防止多次点击事件发生
 *  使用示例
 *  btnLogin.setOnClickListener(new NoMultiClickListener() {
 *             @Override
 *             public void onNoMultiClick(View v) {
 *                 if (TextUtils.isEmpty(editTextPhone.getText().toString())) {
 *                     showToast("输入的手机号不能为空！");
 *                 }
 *             }
 *         });
 */
public abstract class NoMultiClickListener implements View.OnClickListener {

    // 两次点击按钮之间的最小点击间隔时间(单位:ms)
    private static final int MIN_CLICK_DELAY_TIME = 1000;
    // 最后一次点击的时间
    private long lastClickTime;

    @Override
    public void onClick(View v) {// 限制多次点击
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {// 两次点击的时间间隔大于最小限制时间，则触发点击事件
            lastClickTime = currentTime;
            onNoMultiClick(v);
        }
    }

    /**
     * 点击事件(相当于@link{android.view.View.OnClickListener})
     *
     * @param v 使用该限制点击的View
     */
    public abstract void onNoMultiClick(View v);

}
