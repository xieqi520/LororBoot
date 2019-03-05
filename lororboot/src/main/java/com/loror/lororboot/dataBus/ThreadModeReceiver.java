package com.loror.lororboot.dataBus;

import android.content.Intent;
import android.os.Looper;

import com.loror.lororUtil.flyweight.ObjectPool;
import com.loror.lororUtil.view.Click;
import com.loror.lororboot.annotation.RunThread;
import com.loror.lororboot.annotation.WitchThread;

import java.lang.reflect.Method;

public class ThreadModeReceiver {
    private DataBusReceiver receiver;
    private @RunThread
    int thread = RunThread.LASTTHREAD;

    public ThreadModeReceiver(DataBusReceiver receiver) {
        this.receiver = receiver;
        if (receiver != null) {
            try {
                Method method = receiver.getClass().getDeclaredMethod("receiveData", String.class, Intent.class);
                if (method != null) {
                    WitchThread runThread = method.getAnnotation(WitchThread.class);
                    if (runThread != null) {
                        this.thread = runThread.value();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void receiveData(final String name, final Intent data) {
        if (receiver != null) {
            switch (thread) {
                case RunThread.MAINTHREAD:
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        receiver.receiveData(name, data);
                    } else {
                        ObjectPool.getInstance().getHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                receiver.receiveData(name, data);
                            }
                        });
                    }
                    break;
                case RunThread.NEWTHREAD:
                    new Thread() {
                        @Override
                        public void run() {
                            receiver.receiveData(name, data);
                        }
                    }.start();
                    break;
                default:
                    receiver.receiveData(name, data);
                    break;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThreadModeReceiver) {
            return receiver == ((ThreadModeReceiver) obj).receiver;
        }
        return false;
    }
}
