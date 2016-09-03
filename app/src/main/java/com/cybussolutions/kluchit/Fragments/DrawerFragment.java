package com.cybussolutions.kluchit.Fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cybussolutions.kluchit.Activities.Job_History;
import com.cybussolutions.kluchit.Activities.Login_activity;
import com.cybussolutions.kluchit.Activities.MainActivity;
import com.cybussolutions.kluchit.Activities.User_profile;
import com.cybussolutions.kluchit.Adapters.Drawer_Addapter;
import com.cybussolutions.kluchit.Network.EndPoints;
import com.cybussolutions.kluchit.R;
import com.facebook.login.LoginManager;

import java.util.Hashtable;
import java.util.Map;


public class DrawerFragment extends Fragment {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    ProgressDialog ringProgressDialog;

    Drawer_Addapter drawer_addapter = null;

    String[] nameArray = new String[] {"Home","Profile","Work History","Logout"};
    int[] images =  new int[] {R.drawable.arowblue,R.drawable.arowgreen,R.drawable.arowyellowe,R.drawable.arowred};
    ListView listView;

    public DrawerFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_drawer, container, false);
        v.setClickable(true);

        listView = (ListView) v.findViewById(R.id.listView1);

        drawer_addapter = new Drawer_Addapter(getActivity(),nameArray,images);

        listView.setAdapter(drawer_addapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition     = position;

                // ListView Clicked item value
                String  itemValue    = (String) listView.getItemAtPosition(position);

                if (position == 0 )
                {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    getActivity().startActivity(intent);
                    getActivity().finish();



                }
                if (position == 1 )
                    {
                        Intent intent = new Intent(getActivity(), User_profile.class);
                        getActivity().startActivity(intent);



                    }

                if (position == 2 )
                {
                    Intent intent = new Intent(getActivity(), Job_History.class);
                    getActivity().startActivity(intent);



                }

                if (position == 3 )
                {

                    SharedPreferences pref = getActivity().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);
                    //SharedPreferences.Editor editor = pref.edit();

                    if (pref.contains("Status")) {

                        String status = pref.getString("Status", null);
                        if (status.equals("active")) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("End Job And Logout Confirmation Dialog:")
                                    .setMessage("Logging out would remove your created job and images in upload queue?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                            SharedPreferences pref=getActivity().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor=pref.edit();
                                            editor.clear();
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
                        else
                        {
                            //here
                            pref=getActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor=pref.edit();
                            editor.clear();
                            editor.commit();


                            LoginManager.getInstance().logOut();

                            Intent intent = new Intent(getActivity(), Login_activity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        }
                    }
                    else
                    {
                        pref=getActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor=pref.edit();
                        editor.clear();
                        editor.commit();


                        LoginManager.getInstance().logOut();

                        Intent intent = new Intent(getActivity(), Login_activity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    }

                }
            }
        });

        return v;
    }

    void close_job()
    {
        String close_job= EndPoints.CLOSE_JOB;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, close_job,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        ringProgressDialog.dismiss();
                        Toast.makeText(getActivity(),s,Toast.LENGTH_LONG).show();
                        //finish();


                        SharedPreferences pref=getActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor=pref.edit();
                        editor.clear();
                        editor.commit();


                        pref = getActivity().getSharedPreferences("JobOnDemand", Context.MODE_PRIVATE);
                        editor = pref.edit();


                        if (pref.contains("current"))
                            editor.remove("current");
                        if (pref.contains("total"))
                            editor.remove("total");
                        if (pref.contains("completed"))
                            editor.remove("completed");


                        editor.commit();


                        LoginManager.getInstance().logOut();

                        Intent intent = new Intent(getActivity(), Login_activity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //Dismissing the progress dialog

                        //Showing toast
                        //result+=volleyError.toString();
                        ringProgressDialog.dismiss();
                        Toast.makeText(getActivity(), volleyError.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //Converting Bitmap to String


                //Creating parameters
                Map<String, String> params = new Hashtable<String, String>();


                SharedPreferences pref = getActivity().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                params.put("job_id",pref.getString("job_id",null));
                params.put("user_id",pref.getString("user_id",null));
                params.put("is_open","1");
                return params;
            }
        };

        ringProgressDialog = ProgressDialog.show(getActivity(), "Please wait ...", "Closing job ...", true);
        ringProgressDialog.setCancelable(false);
        ringProgressDialog.show();
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.add(stringRequest);

    }


    public void setup(DrawerLayout dawerLayout ,Toolbar toolbar)


    {


        mDrawerLayout = dawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, toolbar, R.string.draweropem, R.string.drawerclose) {
            @Override
            public void onDrawerOpened(View drawerView) {

                super.onDrawerOpened(drawerView);



               // getActivity().supportInvalidateOptionsMenu();

            }

            @Override
            public void onDrawerClosed(View drawerView) {

                super.onDrawerClosed(drawerView);
              //  getActivity().supportInvalidateOptionsMenu();


            }
        };



    }

}
