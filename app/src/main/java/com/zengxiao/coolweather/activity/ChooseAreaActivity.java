package com.zengxiao.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zengxiao.coolweather.R;
import com.zengxiao.coolweather.db.CoolWeatherDB;
import com.zengxiao.coolweather.model.City;
import com.zengxiao.coolweather.model.Country;
import com.zengxiao.coolweather.model.Province;
import com.zengxiao.coolweather.util.HttpCallbackListener;
import com.zengxiao.coolweather.util.HttpUtil;
import com.zengxiao.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zx on 2016/3/23.
 */
public class ChooseAreaActivity extends Activity
{
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;
    private int currentLevel;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList=new ArrayList<String>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> countryList;
    private Province selectedProvince;
    private City selectedCity;

    private boolean isFromWeatherActivity;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        isFromWeatherActivity=getIntent().getBooleanExtra(
                "from_weather_activity",false);

        SharedPreferences prefs= PreferenceManager.
                getDefaultSharedPreferences(this);
        if (prefs.getBoolean("city_selected",false)&&!isFromWeatherActivity)
        {
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        listView=(ListView)findViewById(R.id.list_view);
        titleText=(TextView)findViewById(R.id.title_text);
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB=coolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id)
            {
                if (currentLevel==LEVEL_PROVINCE)
                {
                    selectedProvince=provinceList.get(index);
                    queryCities();
                }else if (currentLevel==LEVEL_CITY)
                {
                    selectedCity=cityList.get(index);
                    queryCountries();
                }else if (currentLevel==LEVEL_COUNTRY)
                {
                    String countryCode=countryList.get(index).getCountryCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,
                            WeatherActivity.class);
                    intent.putExtra("country_code",countryCode);
                    startActivity(intent);
                    finish();
                }

            }
        });
        queryProvinces();
    }

    private void queryProvinces()
    {
        provinceList=coolWeatherDB.loadProvinces();
        if (provinceList.size()>0)
        {
            dataList.clear();
            for (Province province:provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel=LEVEL_PROVINCE;
        }else
        {
            queryFromServer(null,"province");
        }
    }
    private void queryCities()
    {
        cityList=coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size()>0)
        {
            dataList.clear();
            for (City city:cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel=LEVEL_CITY;
        }else
        {
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }
    private void queryCountries()
    {
        countryList=coolWeatherDB.loadCountries(selectedCity.getId());
        if (countryList.size()>0)
        {
            for (Country country:countryList)
            {
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel=LEVEL_COUNTRY;
        }else
        {
            queryFromServer(selectedCity.getCityCode(),"country");
        }
    }

    private void queryFromServer(final String code,final String type)
    {
        String address;
        if (!TextUtils.isEmpty(code))
        {
            address="http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else
        {
            address="http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener()
        {
            @Override
            public void onFinish(String response)
            {
                boolean result=false;
                if ("province".equals(type))
                {
                    result= Utility.handleProvincesResponse(coolWeatherDB,response);
                }else if ("city".equals(type))
                {
                    result=Utility.handleCitiesResponse(coolWeatherDB,response,
                            selectedProvince.getId());
                }else if ("country".equals(type))
                {
                    result=Utility.handleCountriesResponse(coolWeatherDB,response,
                            selectedCity.getId());
                }
                if (result)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            closeProgressDialog();
                            if ("province".equals(type))
                            {
                                queryProvinces();
                            }else if ("city".equals(type))
                            {
                                queryCities();
                            }else if ("country".equals(type))
                            {
                                queryCountries();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void showProgressDialog()
    {
        if (progressDialog==null)
        {
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    private void closeProgressDialog()
    {
        if (progressDialog!=null)
        {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (currentLevel==LEVEL_COUNTRY)
        {
            queryCities();
        }else if (currentLevel==LEVEL_CITY)
        {
            queryProvinces();
        }else
        {
            if (isFromWeatherActivity)
            {
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
