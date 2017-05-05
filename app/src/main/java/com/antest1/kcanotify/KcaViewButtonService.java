package com.antest1.kcanotify;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static com.antest1.kcanotify.KcaAkashiViewService.SHOW_AKASHIVIEW_ACTION;
import static com.antest1.kcanotify.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.KcaConstants.FAIRY_REVERSE_LIST;
import static com.antest1.kcanotify.KcaConstants.FRONT_NONE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_INFO;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_NODE;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_HDMG;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_BATTLE_VIEW_REFRESH;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_DATA;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_LIST;
import static com.antest1.kcanotify.KcaConstants.KCA_MSG_QUEST_VIEW_LIST;
import static com.antest1.kcanotify.KcaConstants.PREF_FAIRY_ICON;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.KcaQuestViewService.SHOW_QUESTVIEW_ACTION;
import static com.antest1.kcanotify.KcaUtils.getContextWithLocale;
import static com.antest1.kcanotify.KcaUtils.getId;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.WindowChangeDetectingService.isAccessibilitySettingsOn;

public class KcaViewButtonService extends Service {
    public static final int FAIRY_NOTIFICATION_ID = 10118;
    public static final String KCA_STATUS_ON = "kca_status_on";
    public static final String KCA_STATUS_OFF = "kca_status_off";
    public static final String FAIRY_VISIBLE = "fairy_visible";
    public static final String FAIRY_INVISIBLE = "fairy_invisible";
    public static final String FAIRY_CHANGE = "fairy_change";
    public static final String RETURN_FAIRY_ACTION = "return_fairy_action";
    public static final String RESET_FAIRY_STATUS_ACTION = "reset_fairy_status_action";
    public static final String REMOVE_FAIRY_ACTION = "remove_fairy_action";
    public static final String SHOW_BATTLE_INFO = "show_battle_info";
    public static final String SHOW_QUEST_INFO = "show_quest_info";
    public static final String ACTIVATE_BATTLEVIEW_ACTION = "activate_battleview";
    public static final String DEACTIVATE_BATTLEVIEW_ACTION = "deactivate_battleview";

    private LocalBroadcastManager broadcaster;
    private BroadcastReceiver battleinfo_receiver;
    private BroadcastReceiver battlehdmg_receiver;
    private BroadcastReceiver battlenode_receiver;
    private View mView;
    private WindowManager mManager;
    private Handler mHandler;
    private Vibrator vibrator;
    private ImageView viewbutton;
    private View menulistbutton;
    private int screenWidth, screenHeight;
    private int buttonWidth, buttonHeight;
    private int menuWidth, menuHeight;
    private boolean battleviewEnabled = false;
    public int viewBitmapId = 0;
    public int viewBitmapSmallId = 0;
    WindowManager.LayoutParams mParams;
    NotificationManagerCompat notificationManager;
    public static JsonObject currentApiData;
    public static int recentVisibility = View.VISIBLE;
    public static int type;
    public static int clickcount;

    public static JsonObject getCurrentApiData() {
        return currentApiData;
    }

    public static int getClickCount() {
        return clickcount;
    }

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else {
            clickcount = 0;
            mHandler = new Handler();
            broadcaster = LocalBroadcastManager.getInstance(this);
            battleinfo_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    broadcaster.sendBroadcast(new Intent(KCA_MSG_BATTLE_VIEW_REFRESH));
                    Log.e("KCA", "KCA_MSG_BATTLE_INFO Received: \n".concat(s));
                }
            };
            battlenode_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    broadcaster.sendBroadcast(new Intent(KCA_MSG_BATTLE_VIEW_REFRESH));
                    Log.e("KCA", "KCA_MSG_BATTLE_NODE Received: \n".concat(s));
                }
            };
            battlehdmg_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(KCA_MSG_DATA);
                    if (s.contains("1")) {
                        ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                                R.color.colorHeavyDmgStateWarn), PorterDuff.Mode.MULTIPLY);
                    } else {
                        ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().clearColorFilter();
                    }
                    Log.e("KCA", "KCA_MSG_BATTLE_HDMG Received");
                }
            };

            LocalBroadcastManager.getInstance(this).registerReceiver((battleinfo_receiver), new IntentFilter(KCA_MSG_BATTLE_INFO));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlenode_receiver), new IntentFilter(KCA_MSG_BATTLE_NODE));
            LocalBroadcastManager.getInstance(this).registerReceiver((battlehdmg_receiver), new IntentFilter(KCA_MSG_BATTLE_HDMG));
            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mView = mInflater.inflate(R.layout.view_button, null);

            // Button (Fairy) Settings
            viewbutton = (ImageView) mView.findViewById(R.id.viewbutton);
            String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
            String fairyPath = "noti_icon_".concat(fairyIdValue);
            viewBitmapId = getId(fairyPath, R.mipmap.class);
            viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
            viewbutton.setImageResource(viewBitmapId);
            int index = Arrays.binarySearch(FAIRY_REVERSE_LIST, Integer.parseInt(fairyIdValue));
            if (index >= 0) viewbutton.setScaleX(-1.0f);
            else viewbutton.setScaleX(1.0f);

            viewbutton.setOnTouchListener(mViewTouchListener);
            viewbutton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidth = viewbutton.getMeasuredWidth();
            buttonHeight = viewbutton.getMeasuredHeight();

            // Menu List Settings
            menulistbutton = mView.findViewById(R.id.viewbutton_menu);
            menulistbutton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            menuWidth = menulistbutton.getMeasuredWidth();
            menuHeight = menulistbutton.getMeasuredHeight();
            menulistbutton.setVisibility(View.GONE);
            menulistbutton.findViewById(R.id.viewbutton_battle).setOnTouchListener(mViewTouchListener);
            menulistbutton.findViewById(R.id.viewbutton_quest).setOnTouchListener(mViewTouchListener);
            menulistbutton.findViewById(R.id.viewbutton_akashi).setOnTouchListener(mViewTouchListener);
            ((TextView) menulistbutton.findViewById(R.id.viewbutton_battle)).setText(getStringWithLocale(R.string.viewmenu_battle));
            ((TextView) menulistbutton.findViewById(R.id.viewbutton_quest)).setText(getStringWithLocale(R.string.viewmenu_quest));
            ((TextView) menulistbutton.findViewById(R.id.viewbutton_akashi)).setText(getStringWithLocale(R.string.viewmenu_akashi));

            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            mParams.gravity = Gravity.TOP | Gravity.START;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            Log.e("KCA", "w/h: "+String.valueOf(screenWidth) + " "  +String.valueOf(screenHeight));

            mParams.y = screenHeight - buttonHeight;
            mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mManager.addView(mView, mParams);

            battleviewEnabled = false;
            TextView battleButton = (TextView) menulistbutton.findViewById(R.id.viewbutton_battle);
            battleButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext())) {
            // Can not draw overlays: pass
            stopSelf();
        } else if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(KCA_STATUS_ON)) {
                Log.e("KCA", KCA_STATUS_ON);
                if (mView != null) mView.setVisibility(recentVisibility);
            }
            if (intent.getAction().equals(KCA_STATUS_OFF)) {
                Log.e("KCA", KCA_STATUS_OFF);
                if (mView != null) mView.setVisibility(View.GONE);
            }
            if (intent.getAction().equals(FAIRY_VISIBLE) || intent.getAction().equals(RETURN_FAIRY_ACTION)) {
                if(mView != null) {
                    mView.setVisibility(View.VISIBLE);
                    recentVisibility = View.VISIBLE;
                }
            }
            if (intent.getAction().equals(FAIRY_INVISIBLE)) {
                if(mView != null) {
                    mView.setVisibility(View.GONE);
                    recentVisibility = View.GONE;
                }
            }
            if (intent.getAction().equals(FAIRY_CHANGE)) {
                String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
                String fairyPath = "noti_icon_".concat(fairyIdValue);
                viewBitmapId = getId(fairyPath, R.mipmap.class);
                viewBitmapSmallId = getId(fairyPath.concat("_small"), R.mipmap.class);
                viewbutton.setImageResource(viewBitmapId);
                int index = Arrays.binarySearch(FAIRY_REVERSE_LIST, Integer.parseInt(fairyIdValue));
                if (index >= 0) viewbutton.setScaleX(-1.0f);
                else viewbutton.setScaleX(1.0f);
            }
            if (intent.getAction().equals(RESET_FAIRY_STATUS_ACTION)) {
                ((ImageView) mView.findViewById(R.id.viewbutton)).getDrawable().clearColorFilter();
            }
            if (intent.getAction().equals(ACTIVATE_BATTLEVIEW_ACTION)) {
                battleviewEnabled = true;
                if(menulistbutton != null) {
                    TextView battleButton = (TextView) menulistbutton.findViewById(R.id.viewbutton_battle);
                    battleButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
            }
            if (intent.getAction().equals(DEACTIVATE_BATTLEVIEW_ACTION)) {
                Log.e("KCA", "Called " + DEACTIVATE_BATTLEVIEW_ACTION);
                battleviewEnabled = false;
                if(menulistbutton != null) {
                    TextView battleButton = (TextView) menulistbutton.findViewById(R.id.viewbutton_battle);
                    battleButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.grey));
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battleinfo_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlenode_receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(battlehdmg_receiver);
        if(mManager != null) mManager.removeView(mView);
        super.onDestroy();
    }

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_DURATION = 200;
        private static final int LONG_CLICK_DURATION = 800;

        private long startClickTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            if (id == menulistbutton.findViewById(R.id.viewbutton_battle).getId()) {
                Log.e("KCA", "viewbutton_battle");
                if (battleviewEnabled) {
                    Intent qintent = new Intent(getBaseContext(), KcaBattleViewService.class);
                    qintent.setAction(KcaBattleViewService.SHOW_BATTLEVIEW_ACTION);
                    startService(qintent);
                }
            } else if (id == menulistbutton.findViewById(R.id.viewbutton_quest).getId()) {
                Log.e("KCA", "viewbutton_quest");
                Intent qintent = new Intent(getBaseContext(), KcaQuestViewService.class);
                qintent.setAction(SHOW_QUESTVIEW_ACTION);
                startService(qintent);
            } else if (id == menulistbutton.findViewById(R.id.viewbutton_akashi).getId()) {
                Log.e("KCA", "viewbutton_akashi");
                Intent qintent = new Intent(getBaseContext(), KcaAkashiViewService.class);
                qintent.setAction(SHOW_AKASHIVIEW_ACTION);
                startService(qintent);
            } else if (id == viewbutton.getId()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchX = event.getRawX();
                        mTouchY = event.getRawY();
                        mViewX = mParams.x;
                        mViewY = mParams.y;
                        Log.e("KCA", String.format("mView: %d %d", mViewX, mViewY));
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        mHandler.postDelayed(mRunnable, LONG_CLICK_DURATION);
                        break;

                    case MotionEvent.ACTION_UP:
                        Log.e("KCA", "Callback Canceled");
                        mHandler.removeCallbacks(mRunnable);
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            clickcount += 1;
                            if (menulistbutton.getVisibility() == View.GONE) {
                                menulistbutton.setVisibility(View.VISIBLE);
                            } else {
                                menulistbutton.setVisibility(View.GONE);
                            }
                        }

                        int totalWidth = buttonWidth;
                        int totalHeight = buttonHeight;
                        if (menulistbutton.getVisibility() == View.VISIBLE) {
                            totalWidth += menuWidth;
                        }

                        if (mParams.x < 0) mParams.x = 0;
                        else if (mParams.x > screenWidth - totalWidth) mParams.x = screenWidth - totalWidth;
                        if (mParams.y < 0) mParams.y = 0;
                        else if (mParams.y > screenHeight - totalHeight) mParams.y = screenHeight - totalHeight;

                        int[] locations = new int[2];
                        mView.getLocationOnScreen(locations);
                        int xx = locations[0];
                        int yy = locations[1];
                        Log.e("KCA", String.format("Coord: %d %d", xx, yy));
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int x = (int) (event.getRawX() - mTouchX);
                        int y = (int) (event.getRawY() - mTouchY);

                        mParams.x = mViewX + x;
                        mParams.y = mViewY + y;
                        mManager.updateViewLayout(mView, mParams);
                        if (Math.abs(x) > 20 || Math.abs(y) > 20) {
                            Log.e("KCA", "Callback Canceled");
                            mHandler.removeCallbacks(mRunnable);
                        }
                        break;
                }
            }
            return true;
        }
    };


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            vibrator.vibrate(100);
            Toast.makeText(getApplicationContext(), getStringWithLocale(R.string.viewbutton_hide), Toast.LENGTH_LONG).show();
            mView.setVisibility(View.GONE);
            recentVisibility = View.GONE;
            //displayNotification(getApplicationContext());
        }
    };
    

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e("KCA", "w/h: "+String.valueOf(screenWidth) + " "  +String.valueOf(screenHeight));

        int totalWidth = buttonWidth;
        int totalHeight = buttonHeight;
        if (menulistbutton.getVisibility() == View.VISIBLE) {
            totalWidth += menuWidth;
        }
        if (mParams.x < 0) mParams.x = 0;
        else if (mParams.x > screenWidth - totalWidth) mParams.x = screenWidth - totalWidth;
        if (mParams.y < 0) mParams.y = 0;
        else if (mParams.y > screenHeight - totalHeight) mParams.y = screenHeight - totalHeight;

        ((TextView) menulistbutton.findViewById(R.id.viewbutton_battle)).setText(getStringWithLocale(R.string.viewmenu_battle));
        ((TextView) menulistbutton.findViewById(R.id.viewbutton_quest)).setText(getStringWithLocale(R.string.viewmenu_quest));
        ((TextView) menulistbutton.findViewById(R.id.viewbutton_akashi)).setText(getStringWithLocale(R.string.viewmenu_akashi));
        super.onConfigurationChanged(newConfig);
    }
}