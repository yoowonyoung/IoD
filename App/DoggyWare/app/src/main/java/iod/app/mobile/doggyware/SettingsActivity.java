package iod.app.mobile.doggyware;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;

import iod.app.mobile.tools.Recorder;
import iod.app.mobile.tools.TimeOutRunnable;
import iod.app.mobile.tools.global_variables;

public class SettingsActivity extends AppCompatPreferenceActivity {
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        addPreferencesFromResource(R.xml.pref_headers);
        getSupportActionBar().setTitle("어플리케이션 설정");

        findPreference("voiceRecord").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                dialog = ProgressDialog.show(SettingsActivity.this, "녹음 중 입니다.", "5초간 녹음합니다...", true);
                final Recorder recorder = Recorder.getInstanse(false);
                recorder.setOutputFile("/storage/emulated/0/record.wav");
                recorder.prepare();
                recorder.start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    recorder.stop();
                                    recorder.release();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return false;
            }
        });

        findPreference("voiceTransfer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                dialog = ProgressDialog.show(SettingsActivity.this, "서버와 통신 중", "음성을 전송중입니다...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            voiceUpload();
                        } catch (Exception e) {
                            runOnUiThread(new TimeOutRunnable(SettingsActivity.this, dialog));
                            e.printStackTrace();
                        }
                    }
                }).start();
                return false;
            }
        });
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void voiceUpload() throws IOException {
        HttpClient mHttpClient = new DefaultHttpClient();
        mHttpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://"+ global_variables.ip+":6974/iodsc/iodcontrol";
        File file = new File("/storage/emulated/0/record.wav");
        try {
            Log.i("TAG", "upload started");
            HttpPost httppost1 = new HttpPost(url);

            MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            multipartEntity.addPart("image", new FileBody(file));
            httppost1.setEntity(multipartEntity);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            final String response = mHttpClient.execute(httppost1, responseHandler);
            Log.i("UPLOAD", response);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
