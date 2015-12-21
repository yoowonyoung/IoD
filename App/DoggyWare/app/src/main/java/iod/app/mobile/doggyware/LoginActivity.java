package iod.app.mobile.doggyware;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

import iod.app.mobile.tools.TimeOutRunnable;
import iod.app.mobile.tools.global_variables;

public class LoginActivity extends AppCompatActivity {

    private BootstrapButton loginButton;
    private EditText editRegId;
    private TextView clickText;

    ProgressDialog dialog;

    HttpClient httpClient;
    HttpGet httpGet;

    SharedPreferences pref;
    SharedPreferences.Editor edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        pref = getSharedPreferences("loginPref",0);
        String ip = pref.getString("ip", "0.0.0.0");
        if(!ip.equals("0.0.0.0")){
            global_variables.ip = ip;
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }


        loginButton = (BootstrapButton)findViewById(R.id.button_login);
        editRegId = (EditText) findViewById(R.id.edit_reg_id);
        clickText = (TextView) findViewById(R.id.qrtext);

        clickText.setText(Html.fromHtml("<U>"+clickText.getText().toString()+"</U>"));
        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                dialog = ProgressDialog.show(LoginActivity.this, "서버와 통신중", "기기 등록 중...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getIPAddress(editRegId.getText().toString());
                        } catch (Exception e){
                            e.printStackTrace();
                            runOnUiThread(new TimeOutRunnable(LoginActivity.this, dialog));
                        }
                    }
                }).start();
            }
        });
    }

    private void getIPAddress(String prodNum) throws IOException {
        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://wy.iptime.org/getIPByID.php?prodNum="+prodNum;
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String response = httpClient.execute(httpGet, responseHandler);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(response.equals("\n")){
                    Toast.makeText(LoginActivity.this, "존재하지 않는 기기 번호입니다.", Toast.LENGTH_SHORT).show();
                } else {
                    String address = response.substring(0, response.length()-1);

                    edit = pref.edit();
                    edit.putString("ip", address);
                    edit.commit();

                    Toast.makeText(LoginActivity.this, "성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show();

                    global_variables.ip = address;
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                dialog.dismiss();
            }
        });
    }


}
