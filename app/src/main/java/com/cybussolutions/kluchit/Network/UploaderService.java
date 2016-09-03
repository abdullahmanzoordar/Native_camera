package com.cybussolutions.kluchit.Network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cybussolutions.kluchit.Adapters.AndroidMultiPartEntity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Abdul on 25/08/2016.
 */
public class UploaderService extends Service {

    UploadPreferenceManager myUploadManager;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.

        myUploadManager=new UploadPreferenceManager(this);

        if (!myUploadManager.is_done()) {
            if (!myUploadManager.is_empty()) {
                Toast.makeText(this, "Remaining images are being uploaded!", Toast.LENGTH_LONG).show();
                new UploadFileToServer().execute();

            }
            else
            {
                stopSelf();
            }
        }
        else
        {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }



    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        long totalSize=0;
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero


            //progressBar.setProgress(0);
            //txtPercentage.setText("Press Button to start uploading...");

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            //progressBar.setVisibility(View.VISIBLE);

            // updating progress bar value
            //progressBar.setProgress(progress[0]);

            // updating percentage value
            //txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString = null;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://demo.cybussolutions.com/kluchitrm/vidupload.php");

            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new AndroidMultiPartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });

                File sourceFile=new File(myUploadManager.get_current_url());

                // Adding file data to http body
                entity.addPart("image", new FileBody(sourceFile));

                // Extra parameters if you want to pass to server
                entity.addPart("website",
                        new StringBody("www.androidhive.info"));
                entity.addPart("email", new StringBody("abc@gmail.com"));

                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);




                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;

        }

        @Override
        protected void onPostExecute(String result) {
            Log.e("", "Response from server: " + result);

            // showing the server response in an alert dialog

            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(request);



            super.onPostExecute(result);
        }

    }


    final StringRequest request = new StringRequest(Request.Method.POST, "http://demo.cybussolutions.com/kluchitrm/common_controller/imageEntryDatabase",
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    Toast.makeText(getApplicationContext(),response, Toast.LENGTH_SHORT).show();
                    // ringProgressDialog.dismiss();
                    myUploadManager.is_uploaded();

                    if (!myUploadManager.is_done()) {
                        if (!myUploadManager.is_empty()) {
                            new UploadFileToServer().execute();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "All job pictures and videos uploaded!", Toast.LENGTH_LONG).show();
                            stopSelf();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "All job pictures and videos uploaded!", Toast.LENGTH_LONG).show();
                        stopSelf();
                    }



                }
            }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {


            if(error instanceof NoConnectionError) {

                Toast.makeText(getApplicationContext(), "No internet Connection, Try Again!", Toast.LENGTH_SHORT).show();

            }

            else
            {
                Toast.makeText(getApplicationContext(), error.getMessage().toString(), Toast.LENGTH_SHORT).show();

            }
        }
    }) {
        @Override
        protected Map<String, String> getParams() throws AuthFailureError {

            Map<String, String> params = new HashMap<>();

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            File file=new File(myUploadManager.get_current_url());
            params.put("images",getFileName(Uri.fromFile(file)));

            SharedPreferences pref=getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
            String job_id=pref.getString("job_id",null);
            String person_id=pref.getString("user_id",null);


            params.put("job_id",job_id);
            params.put("uploaded_by",person_id);
            params.put("date_added", timeStamp);//done
            params.put("date_modified", timeStamp);//done
            return params;

        }
    };

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}