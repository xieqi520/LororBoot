package com.loror.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.loror.lororUtil.dataBus.DataBusReceiver;
import com.loror.lororUtil.view.Click;
import com.loror.lororUtil.view.Find;
import com.loror.lororUtil.view.ViewUtil;
import com.loror.lororboot.startable.LororActivity;

public class SecondActivity extends LororActivity implements DataBusReceiver {

    @Find
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        ViewUtil.find(this);
    }

    @Click(id = R.id.send)
    public void send(View v) {
        Intent data = new Intent();
        data.putExtra("msg", "打印消息");
        sendDataToBus("toast", data);
    }

    @Override
    //sticky暂不支持跨进程
    public void receiveData(String name, Intent data) {
        if ("SecondActivity.sticky".equals(name)) {
            text.setText(data.getStringExtra("data"));
        }
    }
}
