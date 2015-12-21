package iod.app.mobile.doggyware;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapText;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.handstudio.android.hzgrapherlib.animation.GraphAnimation;
import com.handstudio.android.hzgrapherlib.graphview.CurveGraphView;
import com.handstudio.android.hzgrapherlib.vo.GraphNameBox;
import com.handstudio.android.hzgrapherlib.vo.curvegraph.CurveGraph;
import com.handstudio.android.hzgrapherlib.vo.curvegraph.CurveGraphVO;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import iod.app.mobile.model.EnvironmentBean;
import iod.app.mobile.tools.TimeOutRunnable;
import iod.app.mobile.tools.global_variables;

public class TemperatureManageActivity extends AppCompatActivity {
    private ViewGroup layoutGraphView;
    ProgressDialog dialog;
    HttpClient httpClient;
    HttpGet httpGet;

    TextView tempMonitor, settedAutoTemp;

    BootstrapButton btnFan, btnMat, btnToggleAuto, btnAutoSetting;

    SharedPreferences mPref;
    SharedPreferences.Editor prefEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_temparature_manage);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPref = getSharedPreferences("tempPref", 0);
        String fanStatus = mPref.getString("fanStatus", "OFF");
        String matStatus = mPref.getString("matStatus", "OFF");
        final int autoTemp = mPref.getInt("autoTemp", -1);
        final boolean isAutoNow = mPref.getBoolean("isAutoNow", false);

        layoutGraphView = (ViewGroup) findViewById(R.id.layoutGraphView);
        tempMonitor = (TextView) findViewById(R.id.tempMonitor);
        settedAutoTemp = (TextView) findViewById(R.id.setted_auto_temp);
        btnFan = (BootstrapButton) findViewById(R.id.button_fan);
        btnMat = (BootstrapButton) findViewById(R.id.button_mat);
        btnToggleAuto = (BootstrapButton) findViewById(R.id.button_toggle_auto);
        btnAutoSetting = (BootstrapButton) findViewById(R.id.button_auto_setting);

        if (autoTemp != -1) {
            settedAutoTemp.setText("현재 설정 : " + Integer.toString(autoTemp) + "℃");
        } else {
            settedAutoTemp.setText("온도가 아직 설정되지 않았습니다.");
            btnToggleAuto.setEnabled(false);
        }

        if (isAutoNow) {
            btnFan.setEnabled(false);
            btnMat.setEnabled(false);
            btnAutoSetting.setEnabled(false);
            btnToggleAuto = setButtonText(btnToggleAuto, "fa_bolt", "자동 제어 끄기");
        } else {

        }

        btnMat.setOnClickListener(new tempManageButtonListener());
        btnFan.setOnClickListener(new tempManageButtonListener());

        btnToggleAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefEdit = mPref.edit();
                if (mPref.getBoolean("isAutoNow", false)) {
                    prefEdit.putString("fanStatus", "OFF");
                    prefEdit.putString("matStatus", "OFF");
                    prefEdit.putBoolean("isAutoNow", false);
                    prefEdit.commit();

                    btnFan.setEnabled(true);
                    btnMat.setEnabled(true);
                    btnAutoSetting.setEnabled(true);

                    btnFan = setButtonText(btnFan, "fa_asterisk", "선풍기 켜기");
                    btnMat = setButtonText(btnMat, "fa_hotel", "매트 켜기");
                    btnToggleAuto = setButtonText(btnToggleAuto, "fa_bolt", "자동 제어 켜기");

                    dialog = ProgressDialog.show(TemperatureManageActivity.this, "서버와 통신 중", "명령 전송 중...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setControl("ALLOFF");
                            } catch (IOException e) {
                                runOnUiThread(new TimeOutRunnable(TemperatureManageActivity.this, dialog));
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    btnFan.setEnabled(false);
                    btnMat.setEnabled(false);
                    btnAutoSetting.setEnabled(false);
                    btnToggleAuto = setButtonText(btnToggleAuto, "fa_bolt", "자동 제어 끄기");


                    prefEdit.putBoolean("isAutoNow", true);
                    prefEdit.commit();

                    dialog = ProgressDialog.show(TemperatureManageActivity.this, "서버와 통신 중", "명령 전송 중...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setControl("AUTO:" + mPref.getInt("autoTemp", -1));
                            } catch (IOException e) {
                                runOnUiThread(new TimeOutRunnable(TemperatureManageActivity.this, dialog));
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });

        btnAutoSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TemperatureManageActivity.this);
                builder.setTitle("자동 제어 온도 설정");
                final View dialogView = getLayoutInflater().inflate(R.layout.dialog_auto, null);
                final NumberPicker tempPicker = (NumberPicker) dialogView.findViewById(R.id.tempPicker);
                tempPicker.setMaxValue(30);
                tempPicker.setMinValue(0);
                tempPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                if (mPref.getInt("autoTemp", -1) != -1) {
                    tempPicker.setValue(mPref.getInt("autoTemp", -1));
                } else {
                    tempPicker.setValue(20);
                }
                builder.setView(dialogView);
                builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefEdit = mPref.edit();
                        prefEdit.putInt("autoTemp", tempPicker.getValue());
                        settedAutoTemp.setText("현재 설정 : " + Integer.toString(tempPicker.getValue()) + "℃");
                        prefEdit.commit();
                    }
                });
                builder.setNegativeButton("취소", null);
                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        if (fanStatus.equals("ON")) {
            btnFan = setButtonText(btnFan, "fa_asterisk", "선풍기 끄기");
        } else {
            btnFan = setButtonText(btnFan, "fa_asterisk", "선풍기 켜기");
        }

        if (matStatus.equals("ON")) {
            btnMat = setButtonText(btnMat, "fa_hotel", "매트 끄기");
        } else {
            btnMat = setButtonText(btnMat, "fa_hotel", "매트 켜기");
        }

        dialog = ProgressDialog.show(TemperatureManageActivity.this, "서버와 통신중", "현재 상태 확인 중...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getHomeStatus();
                } catch (IOException e) {
                    runOnUiThread(new TimeOutRunnable(TemperatureManageActivity.this, dialog));
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getHomeStatus() throws IOException {
        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://"+ global_variables.ip+":6974/iodsc/iodcontrol?action=getHomeStatus";
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String response = httpClient.execute(httpGet, responseHandler);

        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<EnvironmentBean>>() {
        }.getType();
        final ArrayList<EnvironmentBean> homeBeans = gson.fromJson(response, collectionType);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] legendArr = new String[5];
                float[] tempGraph = new float[5];
                for (int i = 0; i < homeBeans.size(); i++) {
                    legendArr[4 - i] = homeBeans.get(i).getTimestamp();
                    tempGraph[4 - i] = Float.parseFloat(homeBeans.get(i).getTemperature());
                }
                setCurveGraph(layoutGraphView, legendArr, tempGraph, "온도", 0xaa66ff33, 30, 10);

                tempMonitor.setText("현재 온도 : " + homeBeans.get(0).getTemperature() + "℃");
                dialog.dismiss();
            }
        });
    }

    private void setCurveGraph(ViewGroup viewGroup, String[] legendArr, float[] graph, String Name, int Color, int maxValue, int increment) {
        CurveGraphVO vo = makeCurveGraphAllSetting(legendArr, graph, Name, Color, maxValue, increment);
        final CurveGraphView cgv = new CurveGraphView(this, vo);
        viewGroup.addView(cgv);
    }

    private CurveGraphVO makeCurveGraphAllSetting(String[] legendArr, float[] graph, String Name, int Color, int maxValue, int increment) {
        //padding
        int paddingBottom = CurveGraphVO.DEFAULT_PADDING;
        int paddingTop = CurveGraphVO.DEFAULT_PADDING;
        int paddingLeft = CurveGraphVO.DEFAULT_PADDING;
        int paddingRight = CurveGraphVO.DEFAULT_PADDING;

        //graph margin
        int marginTop = CurveGraphVO.DEFAULT_MARGIN_TOP;
        int marginRight = CurveGraphVO.DEFAULT_MARGIN_RIGHT;

        //max value

        //increment

        List<CurveGraph> arrGraph = new ArrayList<CurveGraph>();

        arrGraph.add(new CurveGraph(Name, Color, graph));

        CurveGraphVO vo = new CurveGraphVO(
                paddingBottom, paddingTop, paddingLeft, paddingRight,
                marginTop, marginRight, maxValue, increment, legendArr, arrGraph);
        vo.setAnimation(new GraphAnimation(GraphAnimation.LINEAR_ANIMATION, GraphAnimation.DEFAULT_DURATION));
        vo.setGraphNameBox(new GraphNameBox());
        return vo;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_common, menu);
        return true;
    }

    private void setControl(String controlCode) throws IOException {
        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.connection.timeout", 5000);
        String url = "http://"+global_variables.ip+":6974/iodsc/iodcontrol?action=setControl&moduleName=ENVN&controlCode=";
        url += controlCode;
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        httpClient.execute(httpGet, responseHandler);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        });
    }

    private class tempManageButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            BootstrapButton button = (BootstrapButton) v;
            prefEdit = mPref.edit();
            String message = "제어 명령 보내는 중...";
            String buttonText = button.getText().toString();

            showProgressDialog(TemperatureManageActivity.this, message);
            if (buttonText.contains("선풍기 켜기")) {
                prefEdit.putString("fanStatus", "ON");
                button = setButtonText(button, "fa_asterisk", "선풍기 끄기");
                new SendingThread("FANON").start();
            } else if (buttonText.contains("선풍기 끄기")) {
                prefEdit.putString("fanStatus", "OFF");
                button = setButtonText(button, "fa_asterisk", "선풍기 켜기");
                new SendingThread("FANOFF").start();
            } else if (buttonText.contains("매트 켜기")) {
                prefEdit.putString("matStatus", "ON");
                button = setButtonText(button, "fa_hotel", "매트 끄기");
                new SendingThread("HEATON").start();
            } else if (buttonText.contains("매트 끄기")) {
                prefEdit.putString("matStatus", "OFF");
                button = setButtonText(button, "fa_hotel", "매트 켜기");
                new SendingThread("HEATOFF").start();
            }
            prefEdit.commit();
        }
    }

    private class SendingThread extends Thread {
        private String controlCode;

        public SendingThread(String controlCode) {
            this.controlCode = controlCode;
        }

        @Override
        public void run() {
            try {
                setControl(this.controlCode);
            } catch (IOException e) {
                runOnUiThread(new TimeOutRunnable(TemperatureManageActivity.this, dialog));
                e.printStackTrace();
            }
        }
    }

    private void showProgressDialog(Context context, String message) {
        dialog = ProgressDialog.show(context, "서버와 통신 중", message, true);
    }

    private BootstrapButton setButtonText(BootstrapButton button, String icon, String text) {
        button.setBootstrapText(new BootstrapText.Builder(TemperatureManageActivity.this)
                .addFontAwesomeIcon(icon)
                .addText("  " + text).build());
        return button;
    }
}