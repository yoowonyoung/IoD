package iod.app.mobile.doggyware;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FeedReceiver extends BroadcastReceiver {
    HttpClient httpClient;
    HttpGet httpGet;
    SharedPreferences mPref;
    SharedPreferences.Editor prefEdit;
    Context context;

    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        mPref = context.getSharedPreferences("feedPref", 0);

        int breakfast_hour = mPref.getInt("br_hr", 0);
        int breakfast_minute = mPref.getInt("br_mi", 0);
        int dinner_hour = mPref.getInt("dn_hr", 0);
        int dinner_minute = mPref.getInt("dn_mi", 0);

        Toast.makeText(context, sdf.format(System.currentTimeMillis()), Toast.LENGTH_SHORT).show();

        if (date.getHours() == breakfast_hour && date.getMinutes() == breakfast_minute) {
            // 이제 저녁을 줘야지
            date.setHours(dinner_hour);
            date.setMinutes(dinner_minute);
            date.setSeconds(0);
        } else if (date.getHours() == dinner_hour && date.getMinutes() == dinner_minute) {
            date.setHours(breakfast_hour);
            date.setMinutes(breakfast_minute);
            date.setSeconds(0);
            date = new Date(date.getTime() + 24 * 60 * 60 * 1000);
            Toast.makeText(context, sdf.format(date), Toast.LENGTH_SHORT).show();
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        intent = new Intent(context, FeedReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, date.getTime(), sender);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    setControl("ONCE", context);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setControl(String controlCode, Context context) throws IOException {
        prefEdit = context.getSharedPreferences("feedPref",0).edit();

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://201310491.iptime.org:6974/iodsc/iodcontrol?action=setControl&moduleName=FEED&controlCode=";
        url += controlCode;
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String response = httpClient.execute(httpGet, responseHandler);
        String message = "";

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.feed_icon);
        if (response.contains("NULL")) {
            if(mPref.getInt("canBeFed",0)>0){
                prefEdit.putInt("canBeFed", mPref.getInt("canBeFed", 0) - 1);
                prefEdit.putInt("fed", mPref.getInt("fed", 6) + 1);
                prefEdit.commit();
                message = "밥을 줬습니다.";
            } else {
                message = "밥이 부족합니다.";
            }
        } else {
            message = "처리 과정에서 오류가 있었습니다.";
        }
        mBuilder.setContentTitle("자동 급식 알림").setContentText(message);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }
}