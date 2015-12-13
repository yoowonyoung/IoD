package iod.app.mobile.doggyware;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.Calendar;

public class FeedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Feeding", Toast.LENGTH_SHORT).show();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        intent = new Intent(context, FeedReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3 * 1000, sender);
    }
}
