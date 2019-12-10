package com.dongxun.lichunkai.weather.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.NetworkUtils;
import com.dongxun.lichunkai.weather.Adapter.FutureAdapter;
import com.dongxun.lichunkai.weather.Class.FutureInfo;
import com.dongxun.lichunkai.weather.Class.RealtimeInfo;
import com.dongxun.lichunkai.weather.R;
import com.dongxun.lichunkai.weather.Utilities.PermissionUtil;
import com.dongxun.lichunkai.weather.Utilities.ToolHelper;
import com.gyf.immersionbar.ImmersionBar;
import com.jinrishici.sdk.android.JinrishiciClient;
import com.jinrishici.sdk.android.factory.JinrishiciFactory;
import com.jinrishici.sdk.android.listener.JinrishiciCallback;
import com.jinrishici.sdk.android.model.JinrishiciRuntimeException;
import com.jinrishici.sdk.android.model.PoetySentence;
import com.scwang.smart.refresh.footer.BallPulseFooter;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.BezierRadarHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.SpinnerStyle;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.dongxun.lichunkai.weather.Utilities.ToolHelper.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";
    private LocationManager locationManager;
    private String City ;
    private ImageView imageView_back;
    private TextView textView_city;
    private TextView textView_temperature;
    private TextView textView_humidity;
    private TextView textView_info;
    private ImageView imageView_wid;
    private TextView textView_power;
    private TextView textView_aqi;
    private ArrayList<FutureInfo> futureInfos = new ArrayList<>();
    private ListView ListView_future;
    private TextView textView_time;
    private TextView realjinrisiciTextView;
    private TextView textView_loading;
    private ImageView imageView_loading;
    private LinearLayout LinearLayout_message;
    private String newWeatherApiKey;
    private String amapApikey;
    private RealtimeInfo realtimeInfo = new RealtimeInfo();
    private ImageView imageView_location;
    private String locationProvider;
    private IntentFilter intentFilter;
    private NetworkChangeReceiver networkChangeReceiver;
    private RefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, intentFilter);
        //和风天气api
        newWeatherApiKey = getResources().getString(R.string.newapikey);
        //高德地图api
        amapApikey = getResources().getString(R.string.amap_apikey);
        ImmersionBar.with(this).init();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }

    /**
     * 显示信息
     * @param type 0：正在定位，1：正在更新，2：更新成功，3：更新失败，4：网络不可用
     */
    private void showMessage(final int type) {
        //一秒钟显示信息(数据获取成功则显示2秒，失败则一直显示)
        CountDownTimer countDownTimer = new CountDownTimer(1*1000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                switch (type){
                    case 0:
                        imageView_loading.setImageResource(R.drawable.logo_location);
                        textView_loading.setText("正在定位");
                        break;
                    case 1:
                        imageView_loading.setImageResource(R.drawable.logo_loading);
                        textView_loading.setText("正在更新");
                        break;
                    case 2:
                        imageView_loading.setImageResource(R.drawable.logo_succcess);
                        textView_loading.setText("更新成功");
                        refreshLayout.finishRefresh();
                        break;
                    case 3:
                        imageView_loading.setImageResource(R.drawable.logo_fail);
                        textView_loading.setText("更新失败");
                        break;
                    case 4:
                        imageView_loading.setImageResource(R.drawable.logo_fail);
                        textView_loading.setText("网络不可用");
                        break;
                }
            }
            @Override
            public void onFinish() {
                switch (type){
                    case 2:
                        LinearLayout_message.setVisibility(View.GONE);
                        break;
                }
            }
        }.start();
    }

    private void jrscapi(){
        //异步方法
        JinrishiciClient client = JinrishiciClient.getInstance();
        client.getOneSentenceBackground(new JinrishiciCallback() {
            @Override
            public void done(PoetySentence poetySentence) {
                //TODO do something
                //在这里进行你的逻辑处理
                realjinrisiciTextView.setText(poetySentence.getData().getContent());
            }

            @Override
            public void error(JinrishiciRuntimeException e) {
                Log.w(TAG, "error: code = " + e.getCode() + " message = " + e.getMessage());
                //TODO do something else
            }
        });
    }

    /**
     * 设置背景图片和位置
     */
    private void setBack() {
        imageView_back.setImageResource(getBackImg());
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        layoutParams.topMargin = -(height - imageView_back.getHeight())/6;
        layoutParams.bottomMargin = 580;
        imageView_back.setLayoutParams(layoutParams);
    }

    /**
     * 根据季节显示背景图
     */
    private int getBackImg() {
        switch (Integer.parseInt(new SimpleDateFormat("MM").format(new Date()))){
            case 3: case 4:case 5:
                return R.drawable.back_spring;
            case 6: case 7:case 8:
                return R.drawable.back_summer;
            case 9: case 10:case 11:
                return R.drawable.back_autumn;
            case 12: case 1:case 2:
                return R.drawable.back_winter;
        }
        return R.drawable.back_spring;
    }

    /**
     * 初始化组件
     */
    private void initView() {
        imageView_back = findViewById(R.id.imageView_back);
        textView_city = findViewById(R.id.textView_city);
        textView_temperature = findViewById(R.id.textView_temperature);
        textView_humidity = findViewById(R.id.textView_humidity);
        textView_info = findViewById(R.id.textView_info);
        imageView_wid = findViewById(R.id.imageView_wid);
        textView_aqi = findViewById(R.id.textView_aqi);
        textView_power = findViewById(R.id.textView_power);
        ListView_future = findViewById(R.id.ListView_future);
        textView_time = findViewById(R.id.textView_time);
        textView_loading = findViewById(R.id.textView_loading);
        imageView_loading = findViewById(R.id.imageView_loading);
        LinearLayout_message = findViewById(R.id.LinearLayout_message);
        imageView_location = findViewById(R.id.imageView_location);
        imageView_location.setOnClickListener(this);
        realjinrisiciTextView = findViewById(R.id.realjinrisiciTextView);
        refreshLayout = (RefreshLayout)findViewById(R.id.refreshLayout);
    }

    /**
     *
     */
    private void refreshLayout(){

        //设置 Header 为 贝塞尔雷达 样式
        refreshLayout.setRefreshHeader(new BezierRadarHeader(this).setEnableHorizontalDrag(true));
        //设置 Footer 为 球脉冲 样式
        refreshLayout.setRefreshFooter(new BallPulseFooter(this).setSpinnerStyle(SpinnerStyle.Scale));
//        refreshLayout.setRefreshHeader(new ClassicsHeader(this));
        refreshLayout.setRefreshFooter(new ClassicsFooter(this));
        refreshLayout.autoRefresh();
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                if(NetworkUtils.isConnected()){
                    jrscapi();
                    sendRequestWithOkHttp(City,newWeatherApiKey);
                    AirsendRequestWithOkHttp(City,newWeatherApiKey);
                    forecastsendRequestWithOkHttp(City,newWeatherApiKey,5);//加载5天数据
                    refreshlayout.finishRefresh();
                }
                else{
                    refreshLayout.finishRefresh();
                    Toast.makeText(MainActivity.this,"网络不可用",Toast.LENGTH_SHORT).show();
                }

            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                forecastsendRequestWithOkHttp(City,newWeatherApiKey,0);//items==0的时候加载全部7天数据
                refreshLayout.finishLoadMore();
            }
        });
    }

    /**
     * 定位后根据城市查询天气
     */
    private void getDataByCity(String city) {
        //显示信息
        showMessage(1);
        // 发送查询天气请求
        if(NetworkUtils.isConnected()){
            jrscapi();
            sendRequestWithOkHttp(city,newWeatherApiKey);
            AirsendRequestWithOkHttp(city,newWeatherApiKey);
            forecastsendRequestWithOkHttp(city,newWeatherApiKey,5);//加载5天数据
        }
        else{
            Toast.makeText(this,"请打开网络连接",Toast.LENGTH_LONG).show();
        }


    }

    /**
     * 获取权限
     */
    private void getPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.getInstance().requestLocation(this);
        }else {
            getDataByCity(City);
        }
    }

    /**
     * 权限请求
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 504:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getDataByCity(City);
                }else{
                    //跳转应用详情页
                    startActivity(appSetIntent(this));
                }
                break;
            default:break;
        }
    }

    /**
     * 请求天气数据
     */
    private void sendRequestWithOkHttp(final String city,final String apikey){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient();//新建一个OKHttp的对象
                    //和风请求方式
                    Request request = new Request.Builder()
                            .url("https://free-api.heweather.net/s6/weather/now?location="+city+"&key="+newWeatherApiKey+"")
                            .build();//创建一个Request对象
                    //第三步构建Call对象
                    Call call = client.newCall(request);
                    //第四步:异步get请求
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //显示信息
                            showMessage(3);
                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseData = response.body().string();//处理返回的数据
                            try {
                                JSONObject responses = new JSONObject(responseData);
                                String newresponse = responses.getString("HeWeather6");
                                parseJSON(newresponse);//解析JSON
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 请求天气空气质量数据
     */
    private void AirsendRequestWithOkHttp(final String city,final String apikey){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient();//新建一个OKHttp的对象

                    //和风请求方式
                    Request request = new Request.Builder()
                            .url("https://free-api.heweather.net/s6/air/now?location="+city+"&key="+newWeatherApiKey+"")
                            .build();//创建一个Request对象
                    //第三步构建Call对象
                    Call call = client.newCall(request);
                    //第四步:异步get请求
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //显示信息
                            showMessage(3);
                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseData = response.body().string();//处理返回的数据
                            try {
                                JSONObject responses = new JSONObject(responseData);
                                String newresponse = responses.getString("HeWeather6");
                                AirparseJSON(newresponse);//解析JSON
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 请求天气未来7天的天气数据
     */
    private void forecastsendRequestWithOkHttp(final String city, final String apikey, final int items){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client = new OkHttpClient();//新建一个OKHttp的对象

                    //和风请求方式
                    Request request = new Request.Builder()
                            .url("https://free-api.heweather.net/s6/weather/forecast?location="+city+"&key="+newWeatherApiKey+"")
                            .build();//创建一个Request对象
                    //第三步构建Call对象
                    Call call = client.newCall(request);
                    //第四步:异步get请求
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            //显示信息
                            showMessage(3);
                        }
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseData = response.body().string();//处理返回的数据
                            try {
                                JSONObject responses = new JSONObject(responseData);
                                String newresponse = responses.getString("HeWeather6");
                                forecastparseJSON(newresponse,items);//解析JSON
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 解析未来天气JSON（和风API）
     * @param responseData
     */
    private void forecastparseJSON(String responseData,int items) {
        String basic= null,status = null,now= null,daily_forecast = null;
        try {
            JSONArray arr = new JSONArray(responseData);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                basic = temp.getString("basic");
                status = temp.getString("status");
                daily_forecast = temp.getString("daily_forecast");
            }
            if (status.equals("ok")) {
                //当前空气质量信息
                JSONArray jsonObjectnow = new JSONArray(daily_forecast);
                //未来天气信息
                futureInfos.removeAll(futureInfos);
                if(items==0){
                    for (int i = 0;i < jsonObjectnow.length();i++) {
                        JSONObject future = jsonObjectnow.getJSONObject(i);

                        FutureInfo futureInfo = new FutureInfo();
                        futureInfo.setDate(future.getString("date"));
                        futureInfo.setTemperature(future.getString("tmp_max")+"℃"+"/"+future.getString("tmp_min")+"℃");
                        futureInfo.setWeather(future.getString("cond_txt_n"));
                        futureInfo.setWid_day(future.getString("wind_sc"));
//                    futureInfo.setWid_night(future.getString("wind_sc"));
//                    futureInfo.setDirect(future.getString("wind_dir"));
                        futureInfo.setToday(i == 0?true:false);
                        futureInfo.setWeek(ToolHelper.getWeekByDate(future.getString("date")));
                        futureInfo.setWid_img(ToolHelper.getWidImg(ToolHelper.isDay()?future.getString("cond_code_d"):future.getString("cond_code_n"),ToolHelper.isDay()));
                        futureInfos.add(futureInfo);
                    }
                }
                else{
                    for (int i = 0;i < items;i++) {
                        JSONObject future = jsonObjectnow.getJSONObject(i);

                        FutureInfo futureInfo = new FutureInfo();
                        futureInfo.setDate(future.getString("date"));
                        futureInfo.setTemperature(future.getString("tmp_max")+"℃"+"/"+future.getString("tmp_min")+"℃");
                        futureInfo.setWeather(future.getString("cond_txt_n"));
                        futureInfo.setWid_day(future.getString("wind_sc"));
//                    futureInfo.setWid_night(future.getString("wind_sc"));
//                    futureInfo.setDirect(future.getString("wind_dir"));
                        futureInfo.setToday(i == 0?true:false);
                        futureInfo.setWeek(ToolHelper.getWeekByDate(future.getString("date")));
                        futureInfo.setWid_img(ToolHelper.getWidImg(ToolHelper.isDay()?future.getString("cond_code_d"):future.getString("cond_code_n"),false));
                        futureInfos.add(futureInfo);
                    }
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FutureAdapter myAdapter = new FutureAdapter(MainActivity.this,R.layout.future,futureInfos);
                        ListView_future.setAdapter(myAdapter);
                    }});

            }else {
                searchFail("");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析当前天气的空气质量JSON（和风API）
     * @param responseData
     */
    private void AirparseJSON(String responseData) {
        String basic= null,status = null,now= null,air_now_city = null;
        try {
            JSONArray arr = new JSONArray(responseData);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                basic = temp.getString("basic");
                status = temp.getString("status");
                air_now_city = temp.getString("air_now_city");
            }
            if (status.equals("ok")) {
                //当前空气质量信息
                JSONObject jsonObjectnow = new JSONObject(air_now_city);
                   final String air_q = jsonObjectnow.optString("aqi");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //描述太长隐藏”空气“
                        if (ToolHelper.getAqiLevel(Integer.parseInt(air_q)).length()>2){
                            textView_aqi.setText(ToolHelper.getAqiLevel(Integer.parseInt(air_q)) + " " + air_q);
                        }else {
                            textView_aqi.setText("空气" + ToolHelper.getAqiLevel(Integer.parseInt(air_q)) + " " + air_q);
                        }
                    }});
            }else {
                searchFail("");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析JSON（和风API）
     * @param responseData
     */
    private void parseJSON(String responseData) {
        String basic= null,status = null,now= null,air_now_city = null;
        try {
            JSONArray arr = new JSONArray(responseData);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                basic = temp.getString("basic");
                status = temp.getString("status");
                now = temp.getString("now");
            }
            JSONObject jsonObjectbasic = new JSONObject(basic);

            if (status.equals("ok")) {
                //城市
                String city = jsonObjectbasic.optString("location");

                //当前天气信息
                JSONObject jsonObjectnow = new JSONObject(now);
                realtimeInfo.setDirect(jsonObjectnow.optString("wind_dir"));
                realtimeInfo.setHumidity(jsonObjectnow.optString("hum"));
                realtimeInfo.setInfo(jsonObjectnow.optString("cond_txt"));
                realtimeInfo.setPower(jsonObjectnow.optString("wind_sc"));
                realtimeInfo.setTemperature(jsonObjectnow.getString("tmp"));
                realtimeInfo.setWid(jsonObjectnow.getString("cond_code"));

//                }
                searchSuccess("",city,realtimeInfo);
            }else {
                searchFail("");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询天气失败，更新UI
     */
    private void searchFail(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //显示信息
                showMessage(3);
            }
        });
    }

    /**
     * 查询天气成功，更新UI
     * @param reason
     */
    private void searchSuccess(final String reason, final String city, final RealtimeInfo realtimeInfo){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //显示信息
                showMessage(2);
//                textView_reason.setText(reason);
                textView_city.setText(city);
                textView_temperature.setText(realtimeInfo.getTemperature() + "°");
                textView_humidity.setText("湿度 " + realtimeInfo.getHumidity());
                textView_info.setText(realtimeInfo.getInfo());
                imageView_wid.setImageResource(getWidImg(realtimeInfo.getWid(),true));
                textView_power.setText("风力 " + realtimeInfo.getPower());
                textView_time.setText(new SimpleDateFormat("YYYY年MM月dd日 E").format(new Date()));

            }
        });
    }

    /**
     * 返回最小化app
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    /**
     * 处理返回数据
     * @param requestCode 请求码
     * @param resultCode 返回码
     * @param data 返回数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                if (resultCode == RESULT_OK)
                {
                    //获取返回信息并获取数据
                    String resultCity = data.getStringExtra("resultCity");
                    City = resultCity;
                    getDataByCity(resultCity);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.imageView_location:
                Intent intent = new Intent(MainActivity.this,CityActivity.class);
                intent.putExtra("currentCity",City);
                startActivityForResult(intent,1);
                break;
        }
    }

    //根据经纬度查询位置信息以提供天气查询
    @SuppressLint("MissingPermission")
    private void getLocation(Context context){
        //1.获取位置管理器
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //2.获取位置提供器，GPS或是NetWork
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.NETWORK_PROVIDER)){
            //如果是网络定位
            locationProvider = LocationManager.NETWORK_PROVIDER;
        }else if (providers.contains(LocationManager.GPS_PROVIDER)){
            //如果是GPS定位
            locationProvider = LocationManager.GPS_PROVIDER;
        }else {
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return;
        }

        //3.获取上次的位置，一般第一次运行，此值为null
        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location!=null){
            showLocation(location);
        }else{
            // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistace
            locationManager.requestLocationUpdates(locationProvider, 0, 0,mListener);
        }
    }

    private void showLocation(Location location){
        String address = location.getLongitude()+","+location.getLatitude();
        OkHttpClient okHttpClient = new OkHttpClient();//1.定义一个client
        Request request = new Request.Builder().url("https://restapi.amap.com/v3/geocode/regeo?key="+amapApikey+"&location="+address+"&poitype=&radius=&extensions=base&batch=false&roadlevel=0").build();//2.定义一个request
        Call call = okHttpClient.newCall(request);//3.使用client去请求
        call.enqueue(new Callback() {//4.回调方法
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();//5.获得网络数据
                try {
                    JSONObject responses = new JSONObject(result);
                    String results = responses.getString("regeocode");
                    JSONObject regeocode = new JSONObject(results);
                    String res = regeocode.getString("addressComponent");
                    JSONObject addressComponent = new JSONObject(res);
                    String ret = addressComponent.getString("city");
                    City = ret;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                System.out.println(result);
            }
        });
    }

    LocationListener mListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
        // 如果位置发生变化，重新显示
        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }
    };

    //判断网络状态
    class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                getLocation(MainActivity.this);
                JinrishiciFactory.init(MainActivity.this);
                initView();
                setBack();
                getPermission();
                refreshLayout();
            } else {
                Toast.makeText(context, "当前网络不可用", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
