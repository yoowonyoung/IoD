package iod.app.mobile.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by Kim on 2015-12-11.
 */
public class TimeOutRunnable implements Runnable {
    private Context context;
    private ProgressDialog dialog;

    public TimeOutRunnable(Context context, ProgressDialog dialog){
        this.context = context;
        this.dialog = dialog;
    }

    @Override
    public void run() {
        dialog.dismiss();
        Toast.makeText(context, "응답 시간 초과", Toast.LENGTH_SHORT).show();
    }
}
