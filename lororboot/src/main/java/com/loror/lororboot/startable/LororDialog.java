package com.loror.lororboot.startable;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.Window;
import android.widget.Toast;

import com.loror.lororUtil.view.ViewUtil;
import com.loror.lororboot.annotation.RunThread;
import com.loror.lororboot.autoRun.AutoRunAble;
import com.loror.lororboot.bind.BindHolder;
import com.loror.lororboot.bind.BindUtils;
import com.loror.lororboot.bind.DataChangeAble;
import com.loror.lororboot.dataBus.DataBus;
import com.loror.lororboot.dataChange.DataChangeUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

public class LororDialog extends AlertDialog implements StartDilogAble, DataChangeAble, AutoRunAble {

    protected static final int RESULT_OK = -1;
    protected static final int RESULT_CANCEL = 0;

    private ForResult result;
    private int requestCode;
    private int resultCode = RESULT_CANCEL;
    private Intent data;
    protected Context context;
    private Intent intent;

    private List<BindHolder> bindHolders = new LinkedList<>();
    private WeakReference<LororActivity> weakReference;
    private Decorater decorater;

    public LororDialog(@NonNull Context context) {
        this(context, 0);
    }

    public LororDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (result != null) {
                    result.result(requestCode, resultCode, data);
                }
                LororDialog.this.onDismiss();
                if (intent != null) {
                    LororDialog.this.onDestroy();
                }
            }
        });
        decorater = new Decorater(context, this);
        decorater.onCreate();
    }

    @Override
    protected void onStart() {
        decorater.onStart();
        LororActivity activity = weakReference == null ? null : weakReference.get();
        if (activity != null) {
            if (bindHolders.size() > 0) {
                activity.registerBinder(this);
            }
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        decorater.onStop();
        super.onStop();
    }

    protected void onDestroy() {
        bindHolders.clear();
        decorater.onDestroy();
    }

    protected void onDismiss() {
        LororActivity activity = weakReference == null ? null : weakReference.get();
        if (activity != null) {
            activity.unRegisterBinder(this);
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (context instanceof LororActivity) {
            weakReference = new WeakReference<>((LororActivity) context);
            BindUtils.findBindHoldersAndInit(bindHolders, this);
            ViewUtil.click(this);
            if (bindHolders.size() > 0) {
                ((LororActivity) context).registerBinder(this);
            }
        }
    }

    public void sendDataToBus(String name, Intent data) {
        DataBus.notifyReceivers(name, data, context);
    }

    @Override
    public void beginBind(Object tag) {
        LororActivity activity = weakReference == null ? null : weakReference.get();
        if (activity != null) {
            BindUtils.showBindHolders(bindHolders, this);
        }
    }

    public BindHolder findHolderById(@IdRes int id) {
        return BindUtils.findHolderById(bindHolders, id);
    }

    @Override
    public boolean onBindFind(BindHolder holder) {
        return false;
    }

    @Override
    public void event(BindHolder holder, String oldValue, String newValue) {

    }

    @Override
    public void setData(int id, Object value) {
        DataChangeUtils.setData(id, value, null, bindHolders, this);
    }

    @Override
    public void setData(String fieldName, Object value) {
        DataChangeUtils.setData(fieldName, value, null, bindHolders, this);
    }

    public void notifyListDataChangeById(@IdRes int id) {
        DataChangeUtils.notifyListDataChangeById(id, null, bindHolders, this);
    }

    public void putIntent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent == null ? intent = new Intent() : intent;
    }

    public void forResult(int requestCode, ForResult result) {
        this.result = result;
        this.requestCode = requestCode;
    }

    protected void setResult(int resultCode) {
        setResult(resultCode, null);
    }

    protected void setResult(int resultCode, Intent data) {
        this.resultCode = resultCode;
        this.data = data;
    }

    public ForResult getResult() {
        return result;
    }

    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public void startDialog(Intent intent) {
        try {
            Class classType = Class.forName(intent.getComponent().getClassName());
            Constructor<Dialog> con = classType.getConstructor(Context.class);
            Dialog obj = con.newInstance(context);
            if (obj instanceof LororDialog) {
                ((LororDialog) obj).putIntent(intent);
            } else if (intent.getFlags() != Intent.FLAG_ACTIVITY_NO_USER_ACTION) {
                Toast.makeText(context, "你开启的弹窗不是LororDialog，无法传递intent，如不需传递intent可以设置flags为FLAG_ACTIVITY_NO_USER_ACTION以忽略此信息。", Toast.LENGTH_SHORT).show();
            }
            obj.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "开启弹窗失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void startDialogForResult(Intent intent, final int requestCode) {
        try {
            Class classType = Class.forName(intent.getComponent().getClassName());
            Constructor<Dialog> con = classType.getConstructor(Context.class);
            Dialog obj = con.newInstance(context);
            if (obj instanceof LororDialog) {
                ((LororDialog) obj).putIntent(intent);
                ((LororDialog) obj).forResult(requestCode, new LororDialog.ForResult() {
                    @Override
                    public void result(int requestCode, int resultCode, Intent data) {
                        onDialogResult(requestCode, resultCode, data);
                    }
                });
                obj.show();
            } else {
                Toast.makeText(context, "你开启的弹窗不是LororDialog，无法以forResult方式开启。", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "开启弹窗失败", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onDialogResult(int requestCode, int resultCode, Intent data) {

    }

    public interface ForResult {
        void result(int requestCode, int resultCode, Intent data);
    }

    @Override
    public void runUserAutoRun(String methodName) {
        decorater.runUserAutoRun(methodName);
    }

    @Override
    public void run(@RunThread int thread, Runnable runnable) {
        decorater.run(thread, runnable, new Handler());
    }
}
