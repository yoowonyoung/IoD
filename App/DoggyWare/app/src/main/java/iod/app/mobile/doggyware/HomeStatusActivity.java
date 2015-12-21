package iod.app.mobile.doggyware;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import iod.app.mobile.tools.Recorder;
import iod.app.mobile.model.EnvironmentBean;
import iod.app.mobile.tools.TimeOutRunnable;
import iod.app.mobile.tools.global_variables;

public class HomeStatusActivity extends AppCompatActivity {
    ProgressDialog dialog;
    HttpClient httpClient;
    HttpGet httpGet;

    HttpURLConnection con;
    OutputStream os;

    private String delimiter = "--";
    private String boundary = "SwA" + Long.toString(System.currentTimeMillis()) + "SwA";

    private ViewGroup tempGraphView, humidGraphView, illumGraphView;
    private TextView tempMonitor, humidMonitor, illumMonitor;
    private BootstrapButton bTemperatureManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypefaceProvider.registerDefaultIconSets();

        setContentView(R.layout.activity_home_status);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tempGraphView = (ViewGroup) findViewById(R.id.tempGraphView);
        humidGraphView = (ViewGroup) findViewById(R.id.humidGraphView);
        illumGraphView = (ViewGroup) findViewById(R.id.illumGraphView);

        tempMonitor = (TextView) findViewById(R.id.tempMonitor);
        humidMonitor = (TextView) findViewById(R.id.humidMonitor);
        illumMonitor = (TextView) findViewById(R.id.illumMonitor);



        bTemperatureManage = (BootstrapButton) findViewById(R.id.button_to_temperature_manage);
        bTemperatureManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeStatusActivity.this, TemperatureManageActivity.class);
                startActivity(intent);
            }
        });

        dialog = ProgressDialog.show(HomeStatusActivity.this, "서버와 통신중", "현재 상태 확인 중...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getHomeStatus();
                } catch (IOException e) {
                    runOnUiThread(new TimeOutRunnable(HomeStatusActivity.this, dialog));
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_common, menu);
        return true;
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
                // TODO Home Status Graph Viewing
                String[] legendArr = new String[5];
                float[] tempGraph = new float[5];
                float[] humidGraph = new float[5];
                float[] illumGraph = new float[5];
                for (int i = 0; i < homeBeans.size(); i++) {
                    legendArr[4 - i] = homeBeans.get(i).getTimestamp();

                    tempGraph[4 - i] = Float.parseFloat(homeBeans.get(i).getTemperature());
                    humidGraph[4 - i] = Float.parseFloat(homeBeans.get(i).getHumidity());
                    illumGraph[4 - i] = Float.parseFloat(homeBeans.get(i).getIlluminance());
                }
                setCurveGraph(tempGraphView, legendArr, tempGraph, "온도", 0xaa66ff33, 30, 10);
                setCurveGraph(humidGraphView, legendArr, humidGraph, "습도", 0xaa00ffff, 100, 20);
                setCurveGraph(illumGraphView, legendArr, illumGraph, "조도", 0xaaff0066, 50, 10);

                tempMonitor.setText("현재 온도 : " + homeBeans.get(0).getTemperature() + "℃");
                humidMonitor.setText("현재 습도 : " + homeBeans.get(0).getHumidity() + "%");
                illumMonitor.setText("현재 조도 : " + homeBeans.get(0).getIlluminance());

                String strMonth = homeBeans.get(0).getTimestamp().substring(2, 4);
                String strDay = homeBeans.get(0).getTimestamp().substring(4, 6);
                String strHour = homeBeans.get(0).getTimestamp().substring(6, 8);
                String strMinute = homeBeans.get(0).getTimestamp().substring(8, 10);

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
}
