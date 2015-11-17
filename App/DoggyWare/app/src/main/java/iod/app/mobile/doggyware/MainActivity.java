package iod.app.mobile.doggyware;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

import iod.app.mobile.model.ListData;
import iod.app.mobile.model.ModuleBean;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    ProgressDialog dialog;
    HttpClient httpClient;
    HttpGet httpGet;

    ListView lModuleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        lModuleList = (ListView) findViewById(R.id.module_list);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dialog = ProgressDialog.show(MainActivity.this, "서버와 통신중", "모듈 상태 확인 중...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO implements getModuleStatus Thread.
                try {
                    getModuleStatus();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent intent = new Intent(MainActivity.this, StreamingViewActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_status) {

        } else if (id == R.id.nav_feeding) {

        } else if (id == R.id.nav_temp) {
            Intent intent = new Intent(MainActivity.this, TemperatureViewActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_calendar) {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void getModuleStatus() throws IOException {
        httpClient = new DefaultHttpClient();
        String url = "http://201310491.iptime.org:6974/iodsc/iodcontrol?action=getModuleStatus";
        httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String response = httpClient.execute(httpGet, responseHandler);

        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<ModuleBean>>() {
        }.getType();
        final ArrayList<ModuleBean> moduleBeans = gson.fromJson(response, collectionType);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListViewAdapter adapter = new ListViewAdapter(MainActivity.this);
                for (int i = 0; i < moduleBeans.size(); i++) {
                    ModuleBean bean = moduleBeans.get(i);
                    adapter.addItem(new ListData(bean.getModuleName(), bean.getModuleStatus()));
                }
                lModuleList.setAdapter(adapter);
                dialog.dismiss();
            }
        });
    }

    private class ViewHolder {
        public ImageView mModule;
        public TextView mModuleNameAndStatus;
    }

    private class ListViewAdapter extends BaseAdapter {
        private Context mContext = null;
        private ArrayList<ListData> mListData = new ArrayList<ListData>();

        public ListViewAdapter(Context mContext) {
            super();
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return mListData.size();
        }

        @Override
        public Object getItem(int position) {
            return mListData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.module_status_list_item, null);

                holder.mModule = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.mModuleNameAndStatus = (TextView) convertView.findViewById(R.id.listSiteName);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ListData mData = mListData.get(position);
            switch (mData.mModuleName) {
                case "ENVN":
                    if (mData.mModuleStatus.equals("FINE")) {
                        holder.mModule.setImageResource(R.drawable.envn_fine);
                        holder.mModuleNameAndStatus.setText("환경 모듈 : 정상");
                    } else {
                        holder.mModule.setImageResource(R.drawable.envn_dead);
                        holder.mModuleNameAndStatus.setText("환경 모듈 : 작동 불가");
                    }
                    break;
                case "MESR":
                    if (mData.mModuleStatus.equals("FINE")) {
                        holder.mModule.setImageResource(R.drawable.mesr_fine);
                        holder.mModuleNameAndStatus.setText("측정 모듈 : 정상");
                    } else {
                        holder.mModule.setImageResource(R.drawable.mesr_dead);
                        holder.mModuleNameAndStatus.setText("측정 모듈 : 작동 불가");
                    }
                    break;
                case "FEED":
                    if (mData.mModuleStatus.equals("FINE")) {
                        holder.mModule.setImageResource(R.drawable.feed_fine);
                        holder.mModuleNameAndStatus.setText("피딩 모듈 : 정상");
                    } else {
                        holder.mModule.setImageResource(R.drawable.feed_dead);
                        holder.mModuleNameAndStatus.setText("피딩 모듈 : 작동 불가");
                    }
                    break;
            }
            return convertView;
        }

        public void addItem(ListData listData) {
            mListData.add(listData);
        }

        public void removeAll() {
            mListData.clear();
        }
    }
}
