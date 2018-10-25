package com.qq.xgdemo1122;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.qq.xgdemo1122.activity.AboutActivity;
import com.qq.xgdemo1122.activity.DeviceActivity;
import com.qq.xgdemo1122.activity.DiagnosisActivity;
import com.qq.xgdemo1122.activity.HelpActivity;
import com.qq.xgdemo1122.activity.MsgInfoActivity;
import com.qq.xgdemo1122.activity.SettingActivity;
import com.qq.xgdemo1122.bean.XGNotification;
import com.qq.xgdemo1122.common.NotificationService;

import com.tencent.android.tpush.XGCustomPushNotificationBuilder;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.android.tpush.common.Constants;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/*
* 初始化信鸽和MTA
* 初始化主界面
*
* */
public class MainActivity extends Activity implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    private LinearLayout bloadLayout;// 加载提示的布局
    private LinearLayout tloadLayout;// 加载提示的布局
    private TextView bloadInfo;// 加载提示
    private TextView tloadInfo;// 加载提示
    private ListView pushListV;// 列表
    private NotificationService notificationService;
    // 获取通知数据服务
    private pushAdapter adapter;// 列表适配器

    private int currentPage = 1;// 默认第一页
    private static final int lineSize = 10;// 每次显示数
    private int allRecorders = 0;// 全部记录数
    private int pageSize = 1;// 默认共1页
    private boolean isLast = false;// 是否最后一条
    private int firstItem;// 第一条显示出来的数据的游标
    private int lastItem;// 最后显示出来数据的游标
    private String id = "";// 查询条件
    private boolean isUpdate = false;
    private MsgReceiver updateListViewReceiver;
    private Message m;
    private Context context;
    private String token;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d("LC", "onCreate");
        setContentView(R.layout.activity_main);
        context = this;

        //清除通知栏消息
        //XGPushManager.cancelAllNotifaction();

        //代码内动态注册access ID
        //XGPushConfig.setAccessId(this,2100250470);

        //开启信鸽的日志输出，线上版本不建议调用
        XGPushConfig.enableDebug(this, true);
        XGPushConfig.setHuaweiDebug(true);
        XGPushConfig.getToken(this);
        XGPushConfig.setMiPushAppId(getApplicationContext(), "2882303761517879441");
        XGPushConfig.setMiPushAppKey(getApplicationContext(), "5881787977441");
        XGPushConfig.setMzPushAppId(this, "116189");
        XGPushConfig.setMzPushAppKey(this, "0629a06e09314a98b076429a1626041b");
        XGPushConfig.enableOtherPush(getApplicationContext(), true);
        //注册数据更新监听器
        updateListViewReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.qq.xgdemo.activity.UPDATE_LISTVIEW");
        registerReceiver(updateListViewReceiver, intentFilter);
        // 1.获取设备Token
        Handler handler = new HandlerExtension(MainActivity.this);
        m = handler.obtainMessage();
        /*
        注册信鸽服务的接口
        如果仅仅需要发推送消息调用这段代码即可
        */
        XGPushManager.registerPush(getApplicationContext(),
                new XGIOperateCallback() {
                    @Override
                    public void onSuccess(Object data, int flag) {
                        Log.w(Constants.LogTag, "+++ register push sucess. token:" + data + "flag" + flag);

                        m.obj = "+++ register push sucess. token:" + data;
                        m.sendToTarget();
                    }

                    @Override
                    public void onFail(Object data, int errCode, String msg) {
                        Log.w(Constants.LogTag,
                                "+++ register push fail. token:" + data
                                        + ", errCode:" + errCode + ",msg:"
                                        + msg);
                        m.obj = "+++ register push fail. token:" + data
                                + ", errCode:" + errCode + ",msg:" + msg;
                        m.sendToTarget();
                    }
                });

        // 获取token
        XGPushConfig.getToken(this);

        //反注册代码，线上版本不能调用
        //XGPushManager.unregisterPush(this);

        intiView();
        //initCustomPushNotificationBuilder(getApplicationContext());

    }


    private void intiView() {
        //绑定列表展示
        notificationService = NotificationService.getInstance(this);
        pushListV = (ListView) this.findViewById(R.id.push_list);
        //点击事件
        pushListV.setOnItemClickListener(this);
        //滑动事件
        pushListV.setOnScrollListener(this);
        //创建一个角标线性布局来显示正在加载
        bloadLayout = new LinearLayout(this);
        bloadLayout.setMinimumHeight(100);
        bloadLayout.setGravity(Gravity.CENTER);
        //定义一个文本显示"正在加载文本"
        bloadInfo = new TextView(this);
        bloadInfo.setTextSize(16);
        bloadInfo.setTextColor(Color.parseColor("#858585"));
        bloadInfo.setText("加载更多...");
        bloadInfo.setGravity(Gravity.CENTER);
        //绑定组件
        bloadLayout.addView(bloadInfo, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        bloadLayout.getBottom();
        //绑定提示到列表底部
        pushListV.addFooterView(bloadLayout);

        // 4. 创建一个角标线性布局来显示正在加载
        tloadLayout = new LinearLayout(this);
        tloadLayout.setGravity(Gravity.CENTER);
        tloadLayout.setBackgroundResource(R.color.fuxk_base_color_white);
        // 定义一个文本显示"正在加载文本"
        tloadInfo = new TextView(this);
        tloadInfo.setTextSize(14);
        tloadInfo.setTextColor(Color.parseColor("#858585"));
        // tloadInfo.setBackgroundResource(R.color.gray);
        tloadInfo.setText("更多新消息...");
        tloadInfo.setGravity(Gravity.CENTER);
        tloadInfo.setHeight(0);
        // 綁定組件
        tloadLayout.addView(tloadInfo, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // 绑定提示到列表底部
        pushListV.addHeaderView(tloadLayout);
        tloadLayout.setVisibility(View.GONE);
        //右侧按钮
        getOverflowMenu();
        // 6.加载数据
        getNotifications(id);
        ImageView img_right = (ImageView) findViewById(R.id.img_right);
        img_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpinner();
            }
        });
    }

    /**
     * 设置通知自定义View，这样在下发通知时可以指定build_id。编号由开发者自己维护,build_id=0为默认设置
     *
     * @param context
     */
    @SuppressWarnings("unused")
    private void initCustomPushNotificationBuilder(Context context) {
        XGCustomPushNotificationBuilder build = new XGCustomPushNotificationBuilder();

        int id = context.getResources().getIdentifier(
                "tixin", "raw", context.getPackageName());
        String uri = "android.resource://"
                + context.getPackageName() + "/" + id;
        build.setSound(Uri.parse(uri));
        // 设置自定义通知layout,通知背景等可以在layout里设置
        build.setLayoutId(R.layout.notification);
        // 设置自定义通知内容id
        build.setLayoutTextId(R.id.content);
        // 设置自定义通知标题id
        build.setLayoutTitleId(R.id.title);
        // 设置自定义通知图片id
        build.setLayoutIconId(R.id.icon);
        // 设置自定义通知图片资源
        build.setLayoutIconDrawableId(R.drawable.logo);
        // 设置状态栏的通知小图标
        //build.setbigContentView()
        build.setIcon(R.drawable.right);
        // 设置时间id
        build.setLayoutTimeId(R.id.time);
        // 若不设定以上自定义layout，又想简单指定通知栏图片资源
        //build.setNotificationLargeIcon(R.drawable.ic_action_search);
        // 客户端保存build_id
        XGPushManager.setPushNotificationBuilder(this, 1, build);
        XGPushManager.setDefaultNotificationBuilder(this, build);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        XGPushClickedResult click = XGPushManager.onActivityStarted(this);
        // click.getCustomContent()
        Log.d("TPush", "onResumeXGPushClickedResult:" + click);
        if (click != null) { // 判断是否来自信鸽的打开方式
            Toast.makeText(this, "通知被点击:" + click.toString(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(updateListViewReceiver);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        XGPushManager.onActivityStoped(this);

    }

    private void showSpinner() {
        View v = LayoutInflater.from(this).inflate(R.layout.menu_item, null);
        final PopupWindow pw = new PopupWindow(v, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pw.setContentView(v);
        pw.setOutsideTouchable(true);
        pw.setFocusable(true);
        pw.setBackgroundDrawable(new BitmapDrawable());
        pw.showAsDropDown(findViewById(R.id.img_right));
        TextView action_device_token = (TextView) v
                .findViewById(R.id.action_device_token);
        TextView action_help_center = (TextView) v
                .findViewById(R.id.action_help_center);
        TextView action_about_us = (TextView) v
                .findViewById(R.id.action_about_us);
        TextView action_clear = (TextView) v.findViewById(R.id.action_clear);
        TextView action_setting = (TextView) v
                .findViewById(R.id.action_setting);
        TextView action_diagnosis = (TextView) v
                .findViewById(R.id.action_diagnosis);
        action_device_token.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                Intent deviceIntent = new Intent();
                deviceIntent.setClass(context, DeviceActivity.class);
                startActivity(deviceIntent);
            }
        });
        action_help_center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                Intent helpIntent = new Intent();
                helpIntent.setClass(context, HelpActivity.class);
                startActivity(helpIntent);
            }
        });
        action_about_us.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                Intent aboutIntent = new Intent();
                aboutIntent.setClass(context, AboutActivity.class);
                startActivity(aboutIntent);
            }
        });
        action_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                adapter.setData(null);
                adapter.notifyDataSetChanged();
                findViewById(R.id.nodata).setVisibility(View.VISIBLE);
                findViewById(R.id.deviceToken).setVisibility(View.VISIBLE);
                findViewById(R.id.deviceTokenHint).setVisibility(View.VISIBLE);
                findViewById(R.id.deviceLine).setVisibility(View.VISIBLE);
                NotificationService.getInstance(context).deleteAll();
            }
        });
        action_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                Intent settingIntent = new Intent();
                settingIntent.setClass(context, SettingActivity.class);
                startActivity(settingIntent);
            }
        });
        action_diagnosis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
                Intent settingIntent = new Intent();
                settingIntent.setClass(context, DiagnosisActivity.class);
                startActivity(settingIntent);
            }
        });

    }

    private class pushAdapter extends BaseAdapter {

        private Activity mActivity;
        private LayoutInflater mInflater;
        List<XGNotification> adapterData;

        public pushAdapter(Activity aActivity) {
            mActivity = aActivity;
            mInflater = LayoutInflater.from(mActivity);
        }

        public List<XGNotification> getData() {
            return adapterData;
        }

        public void setData(List<XGNotification> pushInfoList) {
            adapterData = pushInfoList;
        }

        @Override
        public int getCount() {
            return (null == adapterData ? 0 : adapterData.size());
        }

        @Override
        public Object getItem(int position) {
            return (null == adapterData ? null : adapterData.get(position));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            pushViewHolder aholder = null;
            XGNotification item = adapterData.get(position);
            if (convertView == null) {
                aholder = new pushViewHolder();
                convertView = mInflater.inflate(R.layout.item_push, null);
                aholder.msg_idv = (TextView) convertView
                        .findViewById(R.id.push_msg_id);
                aholder.contentv = (TextView) convertView
                        .findViewById(R.id.push_content);
                aholder.timev = (TextView) convertView
                        .findViewById(R.id.push_time);
                aholder.titlev = (TextView) convertView
                        .findViewById(R.id.push_title);
                convertView.setTag(aholder);
            } else {
                aholder = (pushViewHolder) convertView.getTag();
            }

            aholder.msg_idv.setText("ID:" + item.getMsg_id());
            aholder.titlev.setText(item.getTitle());
            aholder.contentv.setText(item.getContent());
            if (item.getUpdate_time() != null
                    && item.getUpdate_time().length() > 18) {
                String notificationdate = item.getUpdate_time()
                        .substring(0, 10);
                String notificationtime = item.getUpdate_time().substring(11);
                if (new SimpleDateFormat("yyyy-MM-dd").format(
                        Calendar.getInstance().getTime()).equals(
                        notificationdate)) {
                    aholder.timev.setText(notificationtime);
                } else {
                    aholder.timev.setText(notificationdate);
                }
            } else {
                aholder.timev.setText("未知");
            }
            return convertView;
        }
    }

    ;


    private class pushViewHolder {
        TextView msg_idv;
        TextView titlev;
        TextView timev;
        TextView contentv;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        Intent ait = new Intent(this, MsgInfoActivity.class);
        if (index > 0 && index <= lastItem) {
            XGNotification xgnotification = adapter.getData().get(index - 1);
            ait.putExtra("msg_id", xgnotification.getMsg_id());
            ait.putExtra("title", xgnotification.getTitle());
            ait.putExtra("content", xgnotification.getContent());
            ait.putExtra("activity", xgnotification.getActivity());
            ait.putExtra("notificationActionType",
                    xgnotification.getNotificationActionType());
            ait.putExtra("update_time", xgnotification.getUpdate_time());
            this.startActivity(ait);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        firstItem = firstVisibleItem;
        lastItem = totalItemCount - 1;
        if (firstVisibleItem + visibleItemCount == totalItemCount) {
            isLast = true;
        } else {
            isLast = false;
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // 是否到最底部并且数据还没读完
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            if (isLast && currentPage < pageSize) {
                currentPage++;
                // 设置显示位置
                pushListV.setSelection(lastItem);
                // 增加数据
                appendNotifications(id);
            } else if (firstItem == 0) {
                if (isUpdate && tloadInfo.getHeight() >= 50) {
                    isUpdate = false;
                    updateNotifications(id);
                    TranslateAnimation alp = new TranslateAnimation(0, 0, 80, 0);
                    alp.setDuration(1000);
                    alp.setRepeatCount(1);
                    tloadLayout.setAnimation(alp);
                    alp.setAnimationListener(new Animation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {
                            tloadInfo.setText("正在更新...");
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            tloadInfo.setText("更多新消息...");
                            tloadLayout.setVisibility(View.GONE);
                            tloadInfo.setHeight(0);
                            tloadLayout.setMinimumHeight(0);
                        }
                    });
                }
            }
        } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                && firstItem == 0) {
            if (tloadInfo.getHeight() < 50) {
                isUpdate = true;
                tloadInfo.setHeight(50);
                tloadLayout.setMinimumHeight(100);
                tloadLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getNotifications(String id) {
        // 计算总数据条数
        allRecorders = notificationService.getCount();
        getNotificationswithouthint(id);
        Toast.makeText(
                this,
                "共" + allRecorders + "条信息,加载了" + adapter.getData().size()
                        + "条信息", Toast.LENGTH_SHORT).show();
    }

    private void updateNotifications(String id) {
        // 计算总数据条数
        int oldAllRecorders = allRecorders;
        allRecorders = notificationService.getCount();
        getNotificationswithouthint(id);
        Toast.makeText(
                this,
                "共" + allRecorders + "条信息,更新了"
                        + (allRecorders - oldAllRecorders) + "条新信息",
                Toast.LENGTH_SHORT).show();
    }


    private void getNotificationswithouthint(String id) {
        if (allRecorders != 0) {
            this.findViewById(R.id.nodata).setVisibility(View.GONE);
            this.findViewById(R.id.deviceToken).setVisibility(View.GONE);
            this.findViewById(R.id.deviceTokenHint).setVisibility(View.GONE);
            this.findViewById(R.id.deviceLine).setVisibility(View.GONE);
        }

        // 计算总页数
        pageSize = (allRecorders + lineSize - 1) / lineSize;

        // 创建适配器
        adapter = new pushAdapter(this);
        adapter.setData(NotificationService.getInstance(this).getScrollData(
                currentPage = 1, lineSize, id));
        if (allRecorders <= lineSize) {
            bloadLayout.setVisibility(View.GONE);
            bloadInfo.setHeight(0);
            bloadLayout.setMinimumHeight(0);
        } else {
            if (pushListV.getFooterViewsCount() < 1) {
                bloadLayout.setVisibility(View.VISIBLE);
                bloadInfo.setHeight(50);
                bloadLayout.setMinimumHeight(100);
            }
        }
        pushListV.setAdapter(adapter);
    }

    private void appendNotifications(String id) {
        // 计算总数据条数
        allRecorders = notificationService.getCount();
        // 计算总页数
        pageSize = (allRecorders + lineSize - 1) / lineSize;
        int oldsize = adapter.getData().size();
        // 更新适配器
        adapter.getData().addAll(
                NotificationService.getInstance(this).getScrollData(
                        currentPage, lineSize, id));
        // 如果到了最末尾则去掉"正在加载"
        if (allRecorders == adapter.getCount()) {
            bloadInfo.setHeight(0);
            bloadLayout.setMinimumHeight(0);
            bloadLayout.setVisibility(View.GONE);
        } else {
            bloadInfo.setHeight(50);
            bloadLayout.setMinimumHeight(100);
            bloadLayout.setVisibility(View.VISIBLE);
        }
        Toast.makeText(
                this,
                "共" + allRecorders + "条信息,加载了"
                        + (adapter.getData().size() - oldsize) + "条信息",
                Toast.LENGTH_SHORT).show();
        // 通知改变
        adapter.notifyDataSetChanged();
    }

    private static class HandlerExtension extends Handler {
        WeakReference<MainActivity> mActivity;

        HandlerExtension(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity theActivity = mActivity.get();
            if (theActivity == null) {
                theActivity = new MainActivity();
            }
            if (msg != null) {
                Log.d("TPush", msg.obj.toString());
                TextView textView = (TextView) theActivity
                        .findViewById(R.id.deviceToken);
                textView.setText(XGPushConfig.getToken(theActivity));
            }
            // XGPushManager.registerCustomNotification(theActivity,
            // "BACKSTREET", "BOYS", System.currentTimeMillis() + 5000, 0);
        }
    }

    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            allRecorders = notificationService.getCount();
            getNotificationswithouthint(id);
        }
    }

}
