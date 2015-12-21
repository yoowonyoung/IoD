package iod.app.mobile.doggyware;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.handstudio.android.hzgrapherlib.animation.GraphAnimation;
import com.handstudio.android.hzgrapherlib.graphview.CircleGraphView;
import com.handstudio.android.hzgrapherlib.vo.circlegraph.CircleGraph;
import com.handstudio.android.hzgrapherlib.vo.circlegraph.CircleGraphVO;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import iod.app.mobile.tools.TimeOutRunnable;
import iod.app.mobile.tools.global_variables;

public class FeedingActivity extends AppCompatActivity {
    private ViewGroup feedGraphView;
    private BootstrapButton btnFeed, btnAutoFeedSetting;
    private Context context = this;

    ProgressDialog dialog;
    HttpGet httpGet;
    HttpClient httpClient;

    SharedPreferences mPref;
    SharedPreferences.Editor prefEdit;

    TextView txtCanBeFedMonitor, txtFedMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_feeding);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        feedGraphView = (ViewGroup) findViewById(R.id.feedGraphView);

        txtCanBeFedMonitor = (TextView) findViewById(R.id.can_be_fed_monitor);
        txtFedMonitor = (TextView) findViewById(R.id.fed_monitor);

        mPref = getSharedPreferences("feedPref", 0);
        setCircleGraph(mPref.getInt("canBeFed", 0), mPref.getInt("fed", 6));

        txtFedMonitor.setText(Integer.toString(mPref.getInt("fed", 6)) + "회 급식이 완료되었으며,");
        txtCanBeFedMonitor.setText(Integer.toString(mPref.getInt("canBeFed", 0)) + "회 남았습니다.");

        btnFeed = (BootstrapButton) findViewById(R.id.button_feed);
        btnFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefEdit = mPref.edit();
                if (mPref.getInt("canBeFed", 0) > 0 && mPref.getInt("fed", 6) < 6) {
                    dialog = ProgressDialog.show(FeedingActivity.this, "서버와 통신중", "급식 명령 전송 중...", true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setControl("ONCE");
                            } catch (IOException e) {
                                runOnUiThread(new TimeOutRunnable(FeedingActivity.this, dialog));
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    Toast.makeText(FeedingActivity.this, "사료가 다 떨어졌습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAutoFeedSetting = (BootstrapButton) findViewById(R.id.button_auto_feed_setting);
        btnAutoFeedSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FeedingActivity.this);
                builder.setTitle("자동 급식 설정");
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_feed, null);

                final Switch switchAutoFeed = (Switch) dialogView.findViewById(R.id.switch_auto_feed);
                final TimePicker breakfastPicker = (TimePicker) dialogView.findViewById(R.id.breakfast_picker);
                final TimePicker dinnerPicker = (TimePicker) dialogView.findViewById(R.id.dinner_picker);

                final boolean isAutoNow = mPref.getBoolean("isAutoNow", false);
                final int breakfast_hour = mPref.getInt("br_hr", 0);
                int breakfast_minute = mPref.getInt("br_mi", 0);
                int dinner_hour = mPref.getInt("dn_hr", 0);
                int dinner_minute = mPref.getInt("dn_mi", 0);

                switchAutoFeed.setChecked(isAutoNow);
                breakfastPicker.setCurrentHour(breakfast_hour);
                breakfastPicker.setCurrentMinute(breakfast_minute);

                dinnerPicker.setCurrentHour(dinner_hour);
                dinnerPicker.setCurrentMinute(dinner_minute);

                builder.setView(dialogView);
                builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if ((breakfastPicker.getCurrentHour() >= 5 && breakfastPicker.getCurrentHour() <= 10)
                                && (dinnerPicker.getCurrentHour() >= 15 && dinnerPicker.getCurrentHour() <= 20)) {
                            prefEdit = mPref.edit();
                            prefEdit.putBoolean("isAutoNow", switchAutoFeed.isChecked());
                            prefEdit.putInt("br_hr", breakfastPicker.getCurrentHour());
                            prefEdit.putInt("br_mi", breakfastPicker.getCurrentMinute());
                            prefEdit.putInt("dn_hr", dinnerPicker.getCurrentHour());
                            prefEdit.putInt("dn_mi", dinnerPicker.getCurrentMinute());

                            prefEdit.commit();

                            Date dateBreakfastToday = new Date(System.currentTimeMillis());
                            dateBreakfastToday.setHours(breakfastPicker.getCurrentHour());
                            dateBreakfastToday.setMinutes(breakfastPicker.getCurrentMinute());
                            dateBreakfastToday.setSeconds(0);

                            Date dateBreakfastTommorow = new Date(dateBreakfastToday.getTime()+24*60*60*1000);

                            Date dateDinnerToday = new Date(System.currentTimeMillis());
                            dateDinnerToday.setHours(dinnerPicker.getCurrentHour());
                            dateDinnerToday.setMinutes(dinnerPicker.getCurrentMinute());
                            dateDinnerToday.setSeconds(0);

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                            long currentTime = System.currentTimeMillis();
                            long settedTime = 0;

                            if (currentTime < dateBreakfastToday.getTime()) {
                                settedTime = dateBreakfastToday.getTime();
                            } else if ((currentTime > dateBreakfastToday.getTime()) && (currentTime < dateDinnerToday.getTime())) {
                                settedTime = dateDinnerToday.getTime();
                            } else if (currentTime > dateDinnerToday.getTime()) {
                                settedTime = dateBreakfastTommorow.getTime();
                            }

                            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            Intent intent = new Intent(context, FeedReceiver.class);
                            PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

                            if (switchAutoFeed.isChecked()) {
                                am.set(AlarmManager.RTC_WAKEUP, settedTime, sender);
                                Toast.makeText(FeedingActivity.this, sdf.format(settedTime)+"에 알람 설정됌.", Toast.LENGTH_SHORT).show();
                            } else {
                                am.cancel(sender);
                                Toast.makeText(FeedingActivity.this, "알람 해제됌", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(FeedingActivity.this, "잘못된 값이 입력되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("취소", null);
                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void setControl(String controlCode) throws IOException {
        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://"+ global_variables.ip+":6974/iodsc/iodcontrol?action=setControl&moduleName=FEED&controlCode=";
        url += controlCode;
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String response = httpClient.execute(httpGet, responseHandler);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                if (response.contains("NULL")) {
                    prefEdit.putInt("canBeFed", mPref.getInt("canBeFed", 0) - 1);
                    prefEdit.putInt("fed", mPref.getInt("fed", 6) + 1);
                    prefEdit.commit();

                    finish();
                    Intent intent = new Intent(FeedingActivity.this, FeedingActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(FeedingActivity.this, "아직 명령이 전달되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void setCircleGraph(int canBeFed, int fed) {
        CircleGraphVO vo = makeLineGraphAllSetting(canBeFed, fed);
        feedGraphView.addView(new CircleGraphView(this, vo));
    }

    private CircleGraphVO makeLineGraphAllSetting(int canBeFed, int fed) {
        int paddingBottom = CircleGraphVO.DEFAULT_PADDING;
        int paddingTop = CircleGraphVO.DEFAULT_PADDING;
        int paddingLeft = CircleGraphVO.DEFAULT_PADDING;
        int paddingRight = CircleGraphVO.DEFAULT_PADDING;

        int marginTop = CircleGraphVO.DEFAULT_MARGIN_TOP;
        int marginRight = CircleGraphVO.DEFAULT_MARGIN_RIGHT;

        int radius = 130;

        List<CircleGraph> arrGraph = new ArrayList<CircleGraph>();

        arrGraph.add(new CircleGraph("can be fed", Color.BLUE, canBeFed));
        arrGraph.add(new CircleGraph("fed", Color.GRAY, fed));

        CircleGraphVO vo = new CircleGraphVO(paddingBottom, paddingTop, paddingLeft, paddingRight, marginTop, marginRight, radius, arrGraph);

        vo.setLineColor(Color.TRANSPARENT);

        vo.setTextColor(Color.WHITE);
        vo.setTextSize(0);

        vo.setCenterX(0);
        vo.setCenterY(0);

        vo.setAnimation(new GraphAnimation(GraphAnimation.LINEAR_ANIMATION, 2000));
        return vo;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.ACTIONBAR_REFILL) {
            mPref = getSharedPreferences("feedPref", 0);
            prefEdit = mPref.edit();

            prefEdit.putInt("canBeFed", 6);
            prefEdit.putInt("fed", 0);
            prefEdit.commit();

            finish();
            Intent intent = new Intent(FeedingActivity.this, FeedingActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
