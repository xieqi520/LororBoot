package com.loror.lororboot.bind;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loror.lororUtil.image.ImageUtil;
import com.loror.lororboot.annotation.AppendId;
import com.loror.lororboot.annotation.Bind;
import com.loror.lororboot.annotation.DisableItem;
import com.loror.lororboot.annotation.Visibility;
import com.loror.lororboot.startable.LororActivity;
import com.loror.lororboot.startable.LororDialog;
import com.loror.lororboot.startable.LororFragment;
import com.loror.lororboot.views.BindAbleBannerView;
import com.loror.lororboot.views.BindAblePointView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BindUtils {

    /**
     * 找到BIndAble的BindHolder并初始化
     */
    public static void findBindHoldersAndInit(List<BindHolder> bindHolders, final BindAble bindAble) {
        findBindHolders(bindHolders, bindAble, null);
        initHolders(bindHolders, bindAble, null);
    }

    /**
     * 初始化BindHolder
     */
    public static void initHolders(List<BindHolder> bindHolders, final BindAble bindAble, Object tag) {
        for (BindHolder bindHolder : bindHolders) {
            bindHolder.setTag(tag);
            if (bindHolder.visibility != Visibility.NOTCHANGE) {
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
            specialBinder(bindHolder, bindHolder.getView(), bindAble);
            if (bindAble.onBindFind(bindHolder)) {
                Object volume = getVolume(bindHolder, bindAble);
                if (volume instanceof List) {
                    bindHolder.compareTag = ((List) volume).size();
                } else {
                    bindHolder.compareTag = volume;
                }
            } else {
                firstBinder(bindHolder, bindAble);
            }
        }
    }

    /**
     * 找到BIndAble的BindHolder
     */
    public static void findBindHolders(List<BindHolder> bindHolders, final BindAble bindAble, View parent) {
        bindHolders.clear();
        Field[] fields = bindAble.getClass().getDeclaredFields();
        if (fields != null) {
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                Bind bind = (Bind) field.getAnnotation(Bind.class);
                if (bind != null) {
                    field.setAccessible(true);
                    int id = bind.id();
                    View view = findViewById(id, bindAble, parent);
                    if (view != null) {
                        if (bind.visibility() != Visibility.NOTCHANGE) {
                            view.setVisibility(bind.visibility());
                        }
                        final BindHolder bindHolder = new BindHolder();
                        bindHolder.view = view;
                        bindHolder.field = field;
                        bindHolder.format = bind.format().length() == 0 ? null : bind.format();
                        bindHolder.event = bind.event().length() == 0 ? null : bind.event();
                        bindHolder.empty = bind.empty().length() == 0 ? null : bind.empty();
                        bindHolder.visibility = bind.visibility();
                        bindHolder.imagePlace = bind.imagePlace();
                        bindHolder.imageWidth = bind.imageWidth();
                        bindHolder.onlyEvent = bind.onlyEvent();
                        bindHolder.disableItem = field.getAnnotation(DisableItem.class) != null;
                        bindHolders.add(bindHolder);
                        AppendId appendId = field.getAnnotation(AppendId.class);
                        if (appendId != null) {
                            int[] ids = appendId.id();
                            boolean only = appendId.onlyEvent();
                            int length = ids.length;
                            for (int j = 0; j < length; j++) {
                                view = findViewById(ids[j], bindAble, parent);
                                if (view == null) {
                                    continue;
                                }
                                BindHolder appendBindHolder = bindHolder.cloneOne();
                                appendBindHolder.view = view;
                                appendBindHolder.onlyEvent = only;
                                bindHolders.add(appendBindHolder);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 查找
     */
    private static View findViewById(int id, BindAble bindAble, View parent) {
        View view = null;
        if (parent != null) {
            view = parent.findViewById(id);
        } else if (bindAble instanceof LororActivity) {
            view = ((Activity) bindAble).findViewById(id);
        } else if (bindAble instanceof LororFragment) {
            view = ((LororFragment) bindAble).getView().findViewById(id);
        } else if (bindAble instanceof LororDialog) {
            view = ((LororDialog) bindAble).findViewById(id);
        } else if (bindAble instanceof FindViewAble) {
            view = ((FindViewAble) bindAble).findViewById(id);
        }
        return view;
    }

    /**
     * 处理特定类型view绑定
     */
    protected static void specialBinder(final BindHolder bindHolder, View view, final BindAble bindAble) {
        final Field field = bindHolder.field;
        final int id = view.getId();
        if (view instanceof EditText) {
            Object tag = view.getTag(id);
            if (tag != null && tag instanceof TextWatcher) {
                ((EditText) view).removeTextChangedListener((TextWatcher) tag);
            }
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    String value = bindHolder.format == null ?
                            s.toString() : s.toString().replace(bindHolder.format.replace("%s", ""), "");
                    if (bindHolder.event != null) {
                        bindAble.event(bindHolder, bindHolder.compareTag == null ? null : String.valueOf(bindHolder.compareTag), value);
                    }
                    try {
                        Class<?> type = field.getType();
                        if (type == String.class) {
                            field.set(bindAble, bindHolder.compareTag = value);
                        } else if (type == Integer.class) {
                            field.set(bindAble, bindHolder.compareTag = Integer.parseInt(value));
                        } else if (type == Long.class) {
                            field.set(bindAble, bindHolder.compareTag = Long.parseLong(value));
                        } else if (type == Float.class) {
                            field.set(bindAble, bindHolder.compareTag = Float.parseFloat(value));
                        } else if (type == Double.class) {
                            field.set(bindAble, bindHolder.compareTag = Double.parseDouble(value));
                        } else if (type == CharSequence.class) {
                            field.set(bindAble, bindHolder.compareTag = s);
                        } else {
                            throw new IllegalStateException("EditText为双向绑定，只支持属性为String，CharSequence，Integer，Long，Float，Double类型");
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            };
            ((EditText) view).addTextChangedListener(watcher);
            view.setTag(id, watcher);
        } else if (view instanceof CheckBox) {
            Class<?> type = field.getType();
            if (type != boolean.class && type != Boolean.class) {
                throw new IllegalStateException("CheckBox为双向绑定，只支持绑定Boolean类型");
            }
            ((CheckBox) view).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    try {
                        field.set(bindAble, bindHolder.compareTag = isChecked);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (view instanceof ProgressBar) {
            Class<?> type = field.getType();
            if (type != Integer.class && type != Long.class && type != int.class && type != long.class) {
                throw new IllegalStateException("ProgressBar只支持绑定Integer,Long类型");
            }
        } else if (view instanceof AbsListView) {
            Class<?> type = field.getType();
            if (type == List.class || type == ArrayList.class) {
                List list = null;
                try {
                    list = (List) field.get(bindAble);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (list != null) {
                    BinderAdapter adapter = new BinderAdapter(view.getContext(), list, bindAble);
                    if (bindHolder.disableItem) {
                        adapter.setItemEnable(false);
                    }
                    ((AbsListView) view).setAdapter(adapter);
                    view.setTag(id, adapter);
                    bindHolder.compareTag = list.size();
                    if (view instanceof ListView && bindHolder.empty != null) {
                        adapter.setShowEmpty(true);
                        adapter.setEmptyString(bindHolder.empty);
                    }
                } else {
                    throw new IllegalStateException("AbsListView绑定的List<? extends BindAbleItem>不能为null");
                }
            } else {
                throw new IllegalStateException("AbsListView只支持绑定List<? extends BindAbleItem>类型");
            }
        } else if (view instanceof RecyclerView) {
            Class<?> type = field.getType();
            if (type == List.class || type == ArrayList.class) {
                List list = null;
                try {
                    list = (List) field.get(bindAble);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (list != null) {
                    RecyclerBindAbleAdapter adapter = new RecyclerBindAbleAdapter(view.getContext(), list, bindAble);
                    ((RecyclerView) view).setAdapter(adapter);
                    view.setTag(id, adapter);
                    bindHolder.compareTag = list.size();
                } else {
                    throw new IllegalStateException("RecyclerView绑定的List<? extends BindAbleItem>不能为null");
                }
            } else {
                throw new IllegalStateException("RecyclerView只支持绑定List<? extends BindAbleItem>类型");
            }
        } else if (view instanceof BindAbleBannerView) {
            Class<?> type = field.getType();
            if (type == List.class || type == ArrayList.class) {
                List list = null;
                try {
                    list = (List) field.get(bindAble);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                ViewGroup parent = ((ViewGroup) view.getParent());
                BindAblePointView pointView = null;
                if (parent.getChildCount() > 0) {
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View child = parent.getChildAt(i);
                        if (child instanceof BindAblePointView) {
                            view.setTag(view.getId(), pointView = (BindAblePointView) child);
                            break;
                        }
                    }
                }
                if (list != null) {
                    if (pointView != null) {
                        ((BindAbleBannerView) view).setPointView(pointView);
                    }
                    if (list.size() > 0) {
                        BindAbleBannerAdapter adapter = new BindAbleBannerAdapter(view.getContext(), list, bindHolder.imagePlace, bindHolder.imageWidth);
                        ((BindAbleBannerView) view).setAdapter(adapter);
                    }
                    bindHolder.compareTag = list.size();
                } else {
                    throw new IllegalStateException("BindAbleBannerView绑定的List<?>不能为null");
                }
            } else {
                throw new IllegalStateException("BindAbleBannerViewPager只支持绑定List<?>类型");
            }
        }
    }

    /**
     * 首次找到时绑定显示并初次触发事件
     */
    public static void firstBinder(BindHolder bindHolder, BindAble bindAble) {
        try {
            Object value = bindHolder.field.get(bindAble);
            if (value instanceof List) {
                bindHolder.compareTag = -1;
            } else {
                bindHolder.compareTag = null;
            }
            BindUtils.showBindHolder(bindHolder, bindAble);
            //首次未在showBindHolder中触发事件则主动触发事件
            if (value == null) {
                if (bindHolder.event != null) {
                    bindAble.event(bindHolder, null, null);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检测所有BindHolder变化并更新显示
     */
    public static void showBindHolders(List<BindHolder> bindHolders, BindAble bindAble) {
        for (BindHolder bindHolder : bindHolders) {
            showBindHolder(bindHolder, bindAble);
        }
    }

    protected static Object getVolume(BindHolder bindHolder, BindAble bindAble) {
        Object volume = null;
        try {
            volume = bindHolder.field.get(bindAble);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return volume;
    }

    /**
     * 检测BindHolder变化并更新显示
     */
    public static void showBindHolder(BindHolder bindHolder, BindAble bindAble) {
        Object volume = getVolume(bindHolder, bindAble);
        boolean isList = volume instanceof List;
        if ((!isList && ((bindHolder.compareTag == null && volume != null) || (bindHolder.compareTag != null && !bindHolder.compareTag.equals(volume)))) ||
                (isList && (bindHolder.compareTag == null || (int) bindHolder.compareTag != ((List) volume).size()))) {
            String vol = volume == null ? bindHolder.empty : String.valueOf(volume);
            vol = vol == null ? null :
                    (bindHolder.format == null ? vol : bindHolder.format.replace("%s", vol));
            Object old = bindHolder.compareTag;
            if (isList) {
                bindHolder.compareTag = ((List) volume).size();
            } else {
                bindHolder.compareTag = volume;
            }
            if (!bindHolder.onlyEvent) {
                if (bindHolder.view instanceof CheckBox) {
                    ((CheckBox) bindHolder.view).setChecked(!(volume == null || !(Boolean) volume));
                } else if (bindHolder.view instanceof TextView) {
                    if (bindHolder.field.getType() == CharSequence.class) {
                        ((TextView) bindHolder.view).setText((CharSequence) volume);
                    } else {
                        ((TextView) bindHolder.view).setText(vol);
                    }
                } else if (bindHolder.view instanceof ImageView) {
                    if (vol != null) {
                        ImageView imageView = (ImageView) bindHolder.view;
                        int width = bindHolder.imageWidth;
                        if (width == 0) {
                            imageView.getWidth();
                        }
                        if (width == 0) {
                            width = imageView.getMeasuredWidth();
                        }
                        if (width == 0) {
                            width = 300;
                        } else if (width > 1080) {
                            width = 1080;
                        }
                        ImageUtil imageUtil = ImageUtil.with(imageView.getContext()).from(vol).to(imageView).setWidthLimit(width);
                        if (bindHolder.imagePlace != 0) {
                            imageUtil.setDefaultImage(bindHolder.imagePlace);
                        }
                        imageUtil.loadImage();
                    }
                } else if (bindHolder.view instanceof ProgressBar) {
                    if (volume == null) {
                        ((ProgressBar) bindHolder.view).setProgress(0);
                    } else {
                        ((ProgressBar) bindHolder.view).setProgress((int) Long.parseLong(String.valueOf(volume)));
                    }
                } else if (bindHolder.view instanceof WebView) {
                    if (vol != null) {
                        ((WebView) bindHolder.view).loadUrl(vol);
                    }
                } else if (bindHolder.view instanceof AbsListView) {
                    BinderAdapter adapter = (BinderAdapter) bindHolder.view.getTag(bindHolder.view.getId());
                    adapter.notifyDataSetChanged();
                } else if (bindHolder.view instanceof RecyclerView) {
                    RecyclerBindAbleAdapter adapter = (RecyclerBindAbleAdapter) bindHolder.view.getTag(bindHolder.view.getId());
                    adapter.notifyDataSetChanged();
                } else if (bindHolder.view instanceof BindAbleBannerView) {
                    if (volume == null) {
                        throw new IllegalStateException("BindAbleBannerView绑定的List<?>不能为null");
                    }
                    List list = (List) volume;
                    if (list.size() > 0) {
                        BindAbleBannerAdapter adapter = new BindAbleBannerAdapter(bindHolder.view.getContext(), list, bindHolder.imagePlace, bindHolder.imageWidth);
                        ((BindAbleBannerView) bindHolder.view).setAdapter(adapter);
                    }
                }
            }
            if (bindHolder.event != null) {
                bindAble.event(bindHolder, old == null ? null : String.valueOf(old), volume == null ? null : String.valueOf(volume));
            }
        }
    }

    public static BindHolder findHolderById(List<BindHolder> bindHolders, @IdRes int id) {
        BindHolder bindHolder = null;
        for (BindHolder item : bindHolders) {
            if (item.view.getId() == id) {
                bindHolder = item;
                break;
            }
        }
        return bindHolder;
    }

    public static BindHolder findHolderByName(List<BindHolder> bindHolders, String feildName) {
        BindHolder bindHolder = null;
        for (BindHolder item : bindHolders) {
            if (item.field.getName().equals(feildName)) {
                bindHolder = item;
                break;
            }
        }
        return bindHolder;
    }
}
