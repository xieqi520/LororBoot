package com.loror.lororboot.bind;

import android.view.View;

import com.loror.lororboot.click.ClickUtils;

import java.util.LinkedList;
import java.util.List;

public abstract class BindAbleItem implements BindAble {
    private BindAble outBindAble;

    public BindAble obtainOutBindAble() {
        return outBindAble;
    }

    public abstract int getLayout();

    @Override
    public void onBindFind(BindHolder holder) {

    }

    @Override
    public void event(BindHolder holder, String oldValue, String newValue) {

    }

    @Override
    public final void beginBind(Object tag) {
        BinderAdapter.Mark mark = (BinderAdapter.Mark) tag;
        List<BindHolder> bindHolders;
        if (mark.convertView.getTag() == null) {
            bindHolders = new LinkedList<>();
            BindUtils.findBindHoldersOfItem(bindHolders, this, mark.convertView);
            mark.convertView.setTag(bindHolders);
        } else {
            bindHolders = (List<BindHolder>) mark.convertView.getTag();
        }
        outBindAble = mark.bindAble;
        ClickUtils.findAndBindClickOfAdapter(this, mark.convertView, mark.parent);
        mark.parent = null;
        resetHolder(bindHolders);
        BindUtils.showBindHolders(bindHolders, this);
    }

    /**
     * 刷新显示并触发事件，解决控件复用问题
     */
    private void resetHolder(List<BindHolder> bindHolders) {
        for (BindHolder bindHolder : bindHolders) {
            if (bindHolder.visibility != BindHolder.NOTCHANGE) {
                switch (bindHolder.visibility) {
                    case View.VISIBLE:
                        bindHolder.view.setVisibility(View.VISIBLE);
                        break;
                    case View.INVISIBLE:
                        bindHolder.view.setVisibility(View.INVISIBLE);
                        break;
                    case View.GONE:
                        bindHolder.view.setVisibility(View.GONE);
                        break;
                }
            }
            BindUtils.firstBinder(bindHolder, this);
        }
    }

}
