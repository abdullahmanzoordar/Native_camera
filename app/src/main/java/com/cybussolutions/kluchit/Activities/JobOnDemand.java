package com.cybussolutions.kluchit.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cybussolutions.kluchit.Network.EndPoints;
import com.cybussolutions.kluchit.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Abdul on 22/08/2016.
 *
 * use EditText.setFocusable(false) to disable editing
 EditText.setFocusableInTouchMode(true) to enable editing;


 */
public class JobOnDemand extends AppCompatActivity {


    Button btn;
    TextView s_date,e_date;
    String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    ProgressDialog ringProgressDialog;
    EditText job_title;
    EditText job_description;
    EditText venue;
    Button submit;
    boolean created;
    String userId=null,userCategory;
    private Toolbar toolbar;
    private static final int MY_SOCKET_TIMEOUT_MS = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_on_demand);


        toolbar = (Toolbar) findViewById(R.id.app_bar);
        toolbar.setTitle("Job On Demand");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);

        SharedPreferences pref_ = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        if (!pref_.contains("user_id"))
        {
            new AlertDialog.Builder(JobOnDemand.this)
                    .setTitle("Job Creation Error")
                    .setMessage("You are not logged in, Click Ok to Return to main screen")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            //should have an api of closing date
                        }
                    }).create().show();
        }




        btn=(Button)findViewById(R.id.btn);
        s_date=(TextView)findViewById(R.id.start_dob);
        e_date=(TextView)findViewById(R.id.end_dob);
        job_title=(EditText) findViewById(R.id.SetTitleHere);
        job_description=(EditText) findViewById(R.id.description);
        venue=(EditText)findViewById(R.id.venue);
        submit=(Button)findViewById(R.id.submit);





        final SharedPreferences pref = getApplicationContext().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);


        if (pref!=null && pref.contains("Status"))
        {

            String status=pref.getString("Status",null);
            if (status.equals("active")) {

                created = true;
                job_title.setText(pref.getString("job_title", null));
                job_description.setText(pref.getString("job_description", null));
                venue.setText(pref.getString("venue", null));
                s_date.setText(pref.getString("start_date",null));
                e_date.setText("When you wish to!");
                submit.setBackgroundResource(R.drawable.closejob);
                job_description.setFocusable(false);
                job_title.setFocusable(false);
                venue.setFocusable(false);
            }
            else
            {
                created = false;
                btn.setVisibility(View.INVISIBLE);
                s_date.setText(timeStamp);
                e_date.setText("When you wish to!");
            }
        }

        else {

            created = false;
            btn.setVisibility(View.INVISIBLE);
            s_date.setText(timeStamp);
            e_date.setText("When you wish to!");

        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (created == false) {
                    ringProgressDialog = ProgressDialog.show(JobOnDemand.this, "Please wait ...", "Creating job ...", true);
                    ringProgressDialog.setCancelable(false);
                    ringProgressDialog.show();
                    if (check_compulsory_fields_for_submission())
                        json_send();
                    else
                        ringProgressDialog.dismiss();
                } else {
                    new AlertDialog.Builder(JobOnDemand.this)
                            .setTitle("End Job Confirmation Dialog:")
                            .setMessage("Are you sure about closing this job?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences.Editor editor=pref.edit();
                                    editor.putString("Status","inactive");
                                    editor.commit();
                                    close_job();
                                    //should have an api of closing date
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).setCancelable(false)
                            .create().show();
                }
            }
        });


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(JobOnDemand.this,SocialSharing.class);
                startActivity(intent);

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    boolean check_compulsory_fields_for_submission()
    {

        if (job_title.getText().toString().isEmpty())
        {
            job_title.setError("Please mention job title");
            return false;
        }
        if (venue.getText().toString().isEmpty())
        {
            venue.setError("Please enter a venue");
            return false;
        }
        if (job_description.getText().toString().isEmpty())
        {
            job_description.setError("Please enter description");
            return false;
        }
        return true;
    }



    void json_send()
    {
        String upload_job= EndPoints.DEMAND_JOB;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, upload_job,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        ringProgressDialog.dismiss();
                        Toast.makeText(JobOnDemand.this,s,Toast.LENGTH_LONG).show();
                        btn.setVisibility(View.VISIBLE);
                        submit.setBackgroundResource(R.drawable.closejob);
                        created=true;


                        job_title.setFocusable(false);
                        job_description.setFocusable(false);
                        venue.setFocusable(false);


                        SharedPreferences pref = getApplicationContext().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("job_id", s);
                        editor.commit();

                        save_OnDemandJob_prefernce(s);


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //Dismissing the progress dialog

                        //Showing toast
                        //result+=volleyError.toString();
                        ringProgressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), volleyError.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //Converting Bitmap to String


                //Creating parameters
                Map<String, String> params = new Hashtable<String, String>();


                String name=job_title.getText().toString();
                params.put("name",name);
                params.put("start_date",timeStamp);
                params.put("end_date",timeStamp);
                params.put("venue",venue.getText().toString());
                params.put("description",job_description.getText().toString());

                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                userId = pref.getString("user_id", null);
                userCategory = pref.getString("user_cat",null);
                params.put("category",userCategory);
                params.put("added_by",userId);
                String date_modified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                params.put("date_modified",date_modified);



                /*params.put("job_status","1");
                params.put("date_added",timeStamp);
                params.put("date_modified",timeStamp);*/


                //returning parameters
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    void save_OnDemandJob_prefernce(String s) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("Status", "active");
        editor.putString("job_id", s);
        editor.putString("job_description", job_description.getText().toString());
        editor.putString("job_title", job_title.getText().toString());
        editor.putString("user_id", userId);
        editor.putString("venue", venue.getText().toString());
        editor.putString("start_date", timeStamp);
        editor.commit();

        pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        editor = pref.edit();
        editor.putString("job_id", s);
        editor.commit();

    }


    void close_job()
    {
        String close_job= EndPoints.CLOSE_JOB;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, close_job,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        ringProgressDialog.dismiss();
                        Toast.makeText(JobOnDemand.this,s,Toast.LENGTH_LONG).show();
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //Dismissing the progress dialog

                        //Showing toast
                        //result+=volleyError.toString();
                        ringProgressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), volleyError.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //Converting Bitmap to String


                //Creating parameters
                Map<String, String> params = new Hashtable<String, String>();


                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                params.put("job_id",pref.getString("job_id",null));
                params.put("user_id",userId);
                params.put("is_open","1");
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
            MY_SOCKET_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


        ringProgressDialog = ProgressDialog.show(JobOnDemand.this, "Please wait ...", "Closing job ...", true);
        ringProgressDialog.setCancelable(false);
        ringProgressDialog.show();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);

    }

}
