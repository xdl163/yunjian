package com.example.yunjian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class StatusBox {
    private final View dialogView;
    private final PopupWindow popupWindow;
    private final View boxParent;
    private final Timer timer;
    private TimerTask timerTask;
    private final Handler looperHandler;
    private final Handler timerHandler;

    @SuppressLint("ResourceType")
    public StatusBox(Context context, View parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.statusbox, null, false);
        dialogView.setBackgroundResource(R.drawable.statusbox_shape);
        popupWindow = new PopupWindow(dialogView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
        boxParent = parent;

        timer = new Timer();

        timerHandler = new Handler(msg -> {
            if (msg.what == 1 && timerTask != null) {
                timerTask.cancel();
                Message message = looperHandler.obtainMessage();
                looperHandler.sendMessage(message);
            }
            return true;
        });
        looperHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };
    }

    public void show(String message) {
        TextView tvErrorInfo = dialogView.findViewById(R.id.textViewInfo);
        tvErrorInfo.setText(message);
        popupWindow.showAtLocation(boxParent, Gravity.CENTER, 0, 0);

        timerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                timerHandler.sendMessage(message);
            }
        };
        timer.schedule(timerTask, 1000);

        try {
            Looper.getMainLooper();
            Looper.loop();
        } catch (RuntimeException ignored) {
        }
    }

    public void close() {
        popupWindow.dismiss();
    }
}
