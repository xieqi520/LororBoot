package com.loror.lororboot.startable;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.loror.lororboot.bind.BindAble;
import com.loror.lororboot.bind.BindHolder;
import com.loror.lororboot.bind.BindUtils;
import com.loror.lororboot.bind.BinderAdapter;
import com.loror.lororboot.click.ClickUtils;
import com.loror.lororboot.views.BindAbleBannerView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

public class LororFragment extends Fragment implements BindAble {

    private List<BindHolder> bindHolders = new LinkedList<>();
    private WeakReference<List<BindHolder>> weakReferenceList = new WeakReference<>(bindHolders);
    private WeakReference<LororActivity> weakReference;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity instanceof LororActivity) {
            weakReference = new WeakReference<>((LororActivity) activity);
            BindUtils.findBindHolders(bindHolders, this);
            ClickUtils.findAndBindClick(this);
            if (bindHolders.size() > 0) {
                ((LororActivity) activity).registerBinder(this);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        List<BindHolder> bindHolders = weakReferenceList.get();
        if (bindHolders != null) {
            for (BindHolder bindHolder : bindHolders) {
                if (bindHolder.getView() instanceof BindAbleBannerView) {
                    ((BindAbleBannerView) bindHolder.getView()).startScrol();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        List<BindHolder> bindHolders = weakReferenceList.get();
        if (bindHolders != null) {
            for (BindHolder bindHolder : bindHolders) {
                if (bindHolder.getView() instanceof BindAbleBannerView) {
                    ((BindAbleBannerView) bindHolder.getView()).stopScrol();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        LororActivity activity = weakReference == null ? null : weakReference.get();
        if (activity != null) {
            activity.unRegisterBinder(this);
        }
        super.onDestroy();
    }

    @Override
    public final void beginBind(Object tag) {
        LororActivity activity = weakReference == null ? null : weakReference.get();
        if (activity != null) {
            BindUtils.showBindHolders(bindHolders, this);
        }
    }

    public BindHolder findHolderById(@IdRes int id) {
        return BindUtils.findHolderById(bindHolders, id);
    }

    public void notifyListDataChangeById(@IdRes int id) {
        BindHolder bindHolder = BindUtils.findHolderById(bindHolders, id);
        if (bindHolder != null) {
            bindHolder.notifyListChange();
            if (bindHolder.getView() instanceof ListView) {
                BinderAdapter adapter = (BinderAdapter) bindHolder.getView().getTag(bindHolder.getView().getId());
                adapter.setShowEmpty(true);
            }
        }
    }

    @Override
    public void onBindFind(BindHolder holder) {

    }

    @Override
    public void event(BindHolder holder, String oldValue, String newValue) {

    }

    /**
     * 打开dialog
     */
    public void startDialog(Intent intent) {
        try {
            Class classType = Class.forName(intent.getComponent().getClassName());
            Constructor<Dialog> con = classType.getConstructor(Context.class);
            Dialog obj = con.newInstance(this);
            if (obj instanceof LororDialog) {
                ((LororDialog) obj).putIntent(intent);
            } else if (intent.getFlags() != Intent.FLAG_ACTIVITY_NO_USER_ACTION) {
                Toast.makeText(getContext(), "你开启的弹窗不是StartAbleDialog，无法传递intent，如不需传递intent可以设置flags为FLAG_ACTIVITY_NO_USER_ACTION以忽略此信息。", Toast.LENGTH_SHORT).show();
            }
            obj.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "开启弹窗失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开dialog
     */
    public void startDialogForResult(Intent intent, final int requestCode) {
        try {
            Class classType = Class.forName(intent.getComponent().getClassName());
            Constructor<Dialog> con = classType.getConstructor(Context.class);
            Dialog obj = con.newInstance(this);
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
                Toast.makeText(getContext(), "你开启的弹窗不是StartAbleDialog，无法以forResult方式开启。", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "开启弹窗失败", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onDialogResult(int requestCode, int resultCode, Intent data) {

    }
}
