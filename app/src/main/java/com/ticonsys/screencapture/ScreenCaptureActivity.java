package com.ticonsys.screencapture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.squareup.picasso.Picasso;
import com.ticonsys.screencapture.model.ImageDTO;
import com.ticonsys.screencapture.utils.NetInfo;
import com.ticonsys.screencapture.utils.PMSharedPrefUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.ticonsys.screencapture.adapter.AdapterImageListAWS;
import com.ticonsys.screencapture.utils.print;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScreenCaptureActivity extends Activity {

    private static final int REQUEST_CODE = 100;

    Context context;
    Button btnConfig, fetchData, fetchDataAll;
    TextView tvIP;


    private List<ImageDTO> imageDTOSAWS;
    RecyclerView recyclerViewGridServerImagesAWS;
    private RecyclerView.Adapter imageListAdapterAWS;


    EditText dateStart, dateEnd;
    String d1, d2;

    public static volatile boolean shouldSend = true;
    public static volatile long lastIdleTime = System.currentTimeMillis();

    private ScreenReceiver screenReceiver;

    // BroadcastReceiver to detect screen state changes
    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    // Screen is locked
                    shouldSend = false;
                    print.message("box_print ===ActivityMain > ScreenReceiver() ACTION_SCREEN_OFF");
                    Toast.makeText(context, "Screen Locked", Toast.LENGTH_SHORT).show();
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    // Screen is unlocked
                    shouldSend = true;
                    print.message("box_print ===ActivityMain > ScreenReceiver() ACTION_SCREEN_ON");
                    Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the BroadcastReceiver
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
    }

    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        dateStart = (EditText) findViewById(R.id.dateStart);
        dateEnd = (EditText) findViewById(R.id.dateEnd);

        initGalleryViewsAWS();

        tvIP = findViewById(R.id.tvIP);
        String prevIp = PMSharedPrefUtils.getIP(context, PMSharedPrefUtils.USER_IP);
        String prevHomeApi = PMSharedPrefUtils.getHomeApi(context, PMSharedPrefUtils.HOME_API);
        tvIP.setText(prevIp + "\n" + prevHomeApi);


        // config button
        btnConfig = findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogShowSettings(v);
            }
        });

        // fetchData
        fetchData = findViewById(R.id.fetchData);
        fetchData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetInfo.isOnline(context)) {
                    Toast.makeText(context, "NO INTERNET", Toast.LENGTH_LONG).show();
                } else {
                    d1 = dateStart.getText().toString().trim();
                    d2 = dateEnd.getText().toString().trim();
                    getData("filter");
                }
            }
        });

        fetchDataAll = findViewById(R.id.fetchDataAll);
        fetchDataAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetInfo.isOnline(context)) {
                    Toast.makeText(context, "NO INTERNET", Toast.LENGTH_LONG).show();
                } else {
                    getData("all");
                }
            }
        });

        // start projection
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startProjection();
                startButton.setText("Capture Started");
                shouldSend = true;
            }
        });

        // stop projection
        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                stopProjection();
                startButton.setText("Start Recording");
                shouldSend = false;
            }
        });

        if (!NetInfo.isOnline(context)) {
            Toast.makeText(context, "NO INTERNET", Toast.LENGTH_LONG).show();
        }

        // Register the BroadcastReceiver
        screenReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

        ScreenCaptureActivity.lastIdleTime = System.currentTimeMillis(); // initial set up

    } // onCreate


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startService(com.ticonsys.screencapture.ScreenCaptureService.getStartIntent(this, resultCode, data));
            }
        }
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        startService(com.ticonsys.screencapture.ScreenCaptureService.getStopIntent(this));
    }


    /***********************************
     * initGalleryViewsAWS() @ Started
     ***********************************/
    private void initGalleryViewsAWS() {
        recyclerViewGridServerImagesAWS = (RecyclerView) findViewById(R.id.recyclerViewGridServerImagesAWS);
        imageDTOSAWS = new ArrayList<>();
        imageListAdapterAWS = new AdapterImageListAWS("FragmentProfile", imageDTOSAWS, context, new AdapterImageListAWS.OnItemClickListener() {
            @Override
            public void onItemClick(ImageDTO imageDTO, final int position) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getImageId(position);
                    }
                });
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 1, GridLayoutManager.VERTICAL, false);
        recyclerViewGridServerImagesAWS.setLayoutManager(gridLayoutManager);
        recyclerViewGridServerImagesAWS.setAdapter(imageListAdapterAWS);
    }

    private void getData(String callingFrom) {
        // http://192.168.90.228:8000/api/get-info-by-date-range/?start_date=2024-05-07&end_date=2024-05-08
        // http://192.168.90.228:8000/api/get-all-info/

        String url;
        if (callingFrom.equalsIgnoreCase("all")){
             url = PMSharedPrefUtils.getHomeApi(context, PMSharedPrefUtils.HOME_API) + "get-all-info/";
        } else {
             url = PMSharedPrefUtils.getHomeApi(context, PMSharedPrefUtils.HOME_API) + "get-info-by-date-range/?start_date="+d1 + "&end_date="+d2;
        }
        print.message("box_print ===ActivityMain > getDataCompare() url init = " + url);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String result) {
                print.message("box_print ===ActivityMain > getDataCompare() > result = " + result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
//                    JSONObject jsonObject = new JSONObject(print.responseTV);
                    int status = jsonObject.getInt("status");
                    String message = jsonObject.getString("message");
                    print.message("box_print ===ActivityMain > getDataCompare() > status = " + status);

                    if (status == 200) {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray = jsonObject.getJSONArray("data");
                        print.message("box_print ===ActivityMain > getDataCompare() > jsonArray = " + jsonArray);

                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        imageDTOSAWS.clear();

                        JSONObject jsonObjectHomeVideos;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            jsonObjectHomeVideos = jsonArray.getJSONObject(i);

                            String total_detected_object_count = null, name = null, date = null, time = null, detected_objects = null;
                            name = jsonObjectHomeVideos.getString("name");
                            date = jsonObjectHomeVideos.getString("date");
                            time = jsonObjectHomeVideos.getString("time");
                            detected_objects  = jsonObjectHomeVideos.getString("detected_objects");
                            total_detected_object_count = jsonObjectHomeVideos.getString("total_detected_object_count");

                            print.message("box_print ===ActivityMain > getDataCompare() > ------------------------------------"
                                    + "\n index             = " + i
                                    + "\n total_detected_count = " + total_detected_object_count
                                    + "\n name              = " + name
                                    + "\n date              = " + date
                                    + "\n time              = " + time
                                    + "\n detected_objects  = " + detected_objects);

                            ImageDTO imageDTO = new ImageDTO();
                            imageDTO.setTotal(total_detected_object_count);
                            imageDTO.setImage_name(name);
                            imageDTO.setImage_path(name);
                            imageDTO.setDate(date);
                            imageDTO.setTime(time);
                            imageDTO.setDetected(detected_objects);
                            imageDTOSAWS.add(imageDTO);
                        }
                        imageListAdapterAWS.notifyDataSetChanged();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Data loading failed !", Toast.LENGTH_LONG).show();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        print.message("box_print ===ActivityMain > getDataCompare() volleyError = " + volleyError);
                        Toast.makeText(context, "Data loading failed !", Toast.LENGTH_LONG).show();
                    } // VolleyError : End

                }); // StringRequest : End

        RetryPolicy retryPolicy = new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(stringRequest);
    }

 /*   private void getDataApi() {
        // http://192.168.1.115/getHomeData?startDate=2024-05-07&endDate=2024-05-08
        // /api/get-info-by-date-range/?start_date=2024-05-08&end_date=2024-05-08
        String url = PMSharedPrefUtils.getHomeApi(context, PMSharedPrefUtils.HOME_API) + "?start_date="+d1 + "&end_date="+d2;
        print.message("box_print ===ActivityMain > getDataCompare() url init = " + url);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String result) {
                print.message("box_print ===ActivityMain > getDataCompare() > result = " + result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
//                    JSONObject jsonObject = new JSONObject(print.responseTV);
                    int status = jsonObject.getInt("status");
                    String message = jsonObject.getString("message");
                    print.message("box_print ===ActivityMain > getDataCompare() > status = " + status);

                    if (status == 200) {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray = jsonObject.getJSONArray("data");
                        print.message("box_print ===ActivityMain > getDataCompare() > jsonArray = " + jsonArray);

                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        imageDTOSAWS.clear();

                        JSONObject jsonObjectHomeVideos;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            jsonObjectHomeVideos = jsonArray.getJSONObject(i);

                            String id = null, name = null, upload_date = null, poster = null;
                            id = jsonObjectHomeVideos.getString("id");
                            name = jsonObjectHomeVideos.getString("name");
                            upload_date = jsonObjectHomeVideos.getString("upload_date");
                            poster = jsonObjectHomeVideos.getString("poster");
                            print.message("box_print ===ActivityMain > getDataCompare() > ------------------------------------"
                                    + "\n index         = " + i
                                    + "\n id            = " + id
                                    + "\n name          = " + name
                                    + "\n upload_date   = " + upload_date
                                    + "\n poster        = " + poster);

                            ImageDTO imageDTO = new ImageDTO();
                            imageDTO.setId(id);
                            imageDTO.setImage_name(name);
                            imageDTO.setDate(upload_date);
                            imageDTO.setImage_path(poster);
                            imageDTOSAWS.add(imageDTO);
                        }
                        imageListAdapterAWS.notifyDataSetChanged();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Data loading failed !", Toast.LENGTH_LONG).show();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        print.message("box_print ===ActivityMain > getDataCompare() volleyError = " + volleyError);
                        Toast.makeText(context, "Data loading failed !", Toast.LENGTH_LONG).show();
                    } // VolleyError : End

                }); // StringRequest : End

        RetryPolicy retryPolicy = new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(stringRequest);
    }
*/
    private void getImageId(int position) {
        String imageUrl = imageDTOSAWS.get(position).getImage_path();
        String imageName = imageDTOSAWS.get(position).getImage_name();
        String date = imageDTOSAWS.get(position).getDate();
        if (NetInfo.isOnline(context)) {
            dialogShowImages(imageUrl, imageName, date);
        } else {
            Toast.makeText(context, "NO INTERNET !", Toast.LENGTH_LONG).show();
        }
    }

    private void dialogShowSettings(View root) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        ViewGroup viewGroup = root.findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog_mixed, viewGroup, false);

        Button buttonSubmit = dialogView.findViewById(R.id.buttonSubmit);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        EditText etIP = dialogView.findViewById(R.id.etIP);
        EditText etHomeApi = dialogView.findViewById(R.id.etHomeApi);


        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        String prevIp = PMSharedPrefUtils.getIP(context, PMSharedPrefUtils.USER_IP);
        etIP.setText(prevIp);

        String prevHomeApi = PMSharedPrefUtils.getHomeApi(context, PMSharedPrefUtils.HOME_API);
        etHomeApi.setText(prevHomeApi);


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etIP.getText().toString().trim();
                String homeApi = etHomeApi.getText().toString().trim();

                if (ip.equalsIgnoreCase("")) {
                    Toast.makeText(context, "Enter IP", Toast.LENGTH_SHORT).show();
                } else if (homeApi.equalsIgnoreCase("")) {
                    Toast.makeText(context, "Enter Home Api", Toast.LENGTH_SHORT).show();
                } else {
                    PMSharedPrefUtils.setIP(context, PMSharedPrefUtils.USER_IP, ip);
                    PMSharedPrefUtils.setHomeApi(context, PMSharedPrefUtils.HOME_API, homeApi);

                    Toast.makeText(context, ip, Toast.LENGTH_SHORT).show();

                    tvIP.setText("Submit Api: " + ip + "\nHome Api : " + homeApi);
                    alertDialog.dismiss();
                }
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void dialogShowImages(String imageUrl, String imageName, String date) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.custom_grid_layout_images, viewGroup, false);

        Button buttonSubmit = dialogView.findViewById(R.id.buttonSubmit);
        ImageView imageView = dialogView.findViewById(R.id.imageView);
        TextView textViewName = dialogView.findViewById(R.id.textViewName);
        TextView textViewDate = dialogView.findViewById(R.id.textViewDate);

        textViewName.setText(imageName);
        textViewDate.setText(date);

        builder.setView(dialogView);

        // Load the image using Picasso
        Picasso.get()
                .load(imageUrl)
                .into(imageView);

        final AlertDialog alertDialog = builder.create();

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}