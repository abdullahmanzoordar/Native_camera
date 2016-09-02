package com.cybussolutions.kluchit.Activities;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cybussolutions.kluchit.Adapters.AndroidMultiPartEntity;
import com.cybussolutions.kluchit.Network.Analytics;
import com.cybussolutions.kluchit.Network.EndPoints;
import com.cybussolutions.kluchit.Network.UploadPreferenceManager;
import com.cybussolutions.kluchit.Network.UploaderService;
import com.cybussolutions.kluchit.R;
import com.google.android.gms.analytics.HitBuilders;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Abdullah Manzoor Dar on 7/13/2016.
 */
public class SocialSharing extends FragmentActivity {


    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public String abc;
    static int t=0;//no worries
    int pos;

    static int p=-1;
    int logo_index=-1;
    static int image_pos=-1;

    GridView grid;
    String filename=null;

    boolean already_uploaded=false;
    public static int flag=0;

    // directory name to store captured images and videos
    private static final String IMAGE_DIRECTORY_NAME = "Kluchit Camera";

    private Uri fileUri=null; // file url to store image/video
    private ImageView imgPreview;
    private VideoView videoPreview;
    private Button btnCapturePicture, btnRecordVideo;
    ProgressDialog ringProgressDialog;
    TextView textview;
    private ProgressBar progressBar;
    private TextView txtPercentage;
    private Button btnUpload;
    private ImageButton cross;
    long totalSize = 0;
    private String filePath = null;
    private static final String TAG = SocialSharing.class.getSimpleName();
    com.google.android.gms.analytics.Tracker tr;
    boolean edited=false;
    public static int resumed=0;
    MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);
    private Button queue;


    final StringRequest request = new StringRequest(Request.Method.POST, "http://demo.cybussolutions.com/kluchitrm/common_controller/imageEntryDatabase",
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    Toast.makeText(SocialSharing.this,response, Toast.LENGTH_SHORT).show();
                    // ringProgressDialog.dismiss();



                }
            }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {


            if(error instanceof NoConnectionError) {

                Toast.makeText(SocialSharing.this, "No internet Connection, Try Again!", Toast.LENGTH_SHORT).show();

            }

            else
            {
                Toast.makeText(SocialSharing.this, error.getMessage().toString(), Toast.LENGTH_SHORT).show();

            }
        }
    }) {
        @Override
        protected Map<String, String> getParams() throws AuthFailureError {

            Map<String, String> params = new HashMap<>();

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            params.put("images",filename);

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



    String[] web = {
            "Top Left",
            "Top Right",
            "Bottom Left",
            "Bottom Right",
            "Middle"

    } ;
    int[] imageId = {
            R.drawable.tl,
            R.drawable.tr,
            R.drawable.bl,
            R.drawable.br,
            R.drawable.mid
    };




    private void createInstagramIntent(String type, String mediaPath){

        // Create the new Intent using the 'Send' action.
        Intent share = new Intent(Intent.ACTION_SEND);

        // Set the MIME type
        share.setType(type);

        // Create the URI from the media
        File media = new File(mediaPath);
        Uri uri = Uri.fromFile(media);

        // Add the URI to the Intent.
        share.putExtra(Intent.EXTRA_STREAM, uri);

        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));
    }

    @Override
    protected void onStart() {
        super.onStart();

        tr.send(new HitBuilders.ScreenViewBuilder().build());
    }
    void hide_editing_controls()
    {
        findViewById(R.id.up).setVisibility(View.INVISIBLE);
        findViewById(R.id.down).setVisibility(View.INVISIBLE);
        findViewById(R.id.left).setVisibility(View.INVISIBLE);
        findViewById(R.id.right).setVisibility(View.INVISIBLE);
        findViewById(R.id.filters).setVisibility(View.INVISIBLE);
        findViewById(R.id.logo_window).setVisibility(View.INVISIBLE);

        findViewById(R.id.t_left).setVisibility(View.INVISIBLE);
        findViewById(R.id.t_right).setVisibility(View.INVISIBLE);
        findViewById(R.id.b_left).setVisibility(View.INVISIBLE);
        findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
        findViewById(R.id.middle).setVisibility(View.INVISIBLE);

    }
    void show_editing_controls()
    {
        findViewById(R.id.down).setVisibility(View.VISIBLE);
        findViewById(R.id.left).setVisibility(View.VISIBLE);
        if (p==0)
            findViewById(R.id.t_left).setVisibility(View.VISIBLE);
        else if (p==1)
            findViewById(R.id.t_right).setVisibility(View.VISIBLE);
        else if (p==2)
            findViewById(R.id.b_left).setVisibility(View.VISIBLE);
        else if (p==3)
            findViewById(R.id.b_right).setVisibility(View.VISIBLE);
        else if (p==4)
            findViewById(R.id.middle).setVisibility(View.VISIBLE);
    }

    void shrink_editing_controls()
    {
        findViewById(R.id.up).setVisibility(View.INVISIBLE);
        findViewById(R.id.down).setVisibility(View.VISIBLE);
        findViewById(R.id.left).setVisibility(View.VISIBLE);
        findViewById(R.id.right).setVisibility(View.INVISIBLE);
        findViewById(R.id.filters).setVisibility(View.INVISIBLE);
        findViewById(R.id.logo_window).setVisibility(View.INVISIBLE);
    }

    void show_logo_chooser()
    {
        findViewById(R.id.left).setVisibility(View.INVISIBLE);
        findViewById(R.id.right).setVisibility(View.VISIBLE);
        findViewById(R.id.logo_window).setVisibility(View.VISIBLE);
    }

    void show_logo_position_selector()
    {
        findViewById(R.id.down).setVisibility(View.INVISIBLE);
        findViewById(R.id.up).setVisibility(View.VISIBLE);
        findViewById(R.id.filters).setVisibility(View.VISIBLE);
    }


    void hide_logo_chooser()
    {
        findViewById(R.id.left).setVisibility(View.VISIBLE);
        findViewById(R.id.right).setVisibility(View.INVISIBLE);
        findViewById(R.id.logo_window).setVisibility(View.INVISIBLE);
    }

    void hide_logo_position_selector()
    {
        findViewById(R.id.down).setVisibility(View.VISIBLE);
        findViewById(R.id.up).setVisibility(View.INVISIBLE);
        findViewById(R.id.filters).setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onResume() {




        super.onResume();
        tr.send(new HitBuilders.ScreenViewBuilder().build());



        if (flag!=0) {

            if (flag == 2) {
                // get the file url
                pos = 0;

                findViewById(R.id.one).findViewById(R.id.nopic).setVisibility(View.INVISIBLE);
                findViewById(R.id.one).findViewById(R.id.img).setVisibility(View.INVISIBLE);
                videoPreview.setVisibility(View.VISIBLE);
                if (abc!=null) {
                    videoPreview.setVideoPath(abc);
                }
                else {
                    if (fileUri != null)
                        videoPreview.setVideoPath(fileUri.getPath());
                }
                videoPreview.start();

            }
            if (flag == 1) {
                imgPreview.setVisibility(View.VISIBLE);
                findViewById(R.id.one).findViewById(R.id.nopic).setVisibility(View.INVISIBLE);
                findViewById(R.id.one).findViewById(R.id.vid).setVisibility(View.INVISIBLE);

                Bitmap bmp=null;

                if (abc!=null)
                    bmp = BitmapFactory.decodeFile(abc);
                else {
                    if (fileUri!=null)
                    bmp = BitmapFactory.decodeFile(fileUri.getPath());

                }
                if (bmp!=null) {
                    //Bitmap temp=bmp.createScaledBitmap(bmp,imgPreview.getMaxWidth(),imgPreview.getMaxHeight(),true);
                    //imgPreview.setImageBitmap(temp);
                    imgPreview.setImageBitmap(bmp);
                }
            }
            if (flag==0)
                hide_editing_controls();
        }

        if (edited==true)
            hide_editing_controls();


    }




    @Override
    public void onBackPressed() {

        super.onBackPressed();
        videoPreview.pause();
        videoPreview.stopPlayback();


        findViewById(R.id.nopic).setVisibility(View.VISIBLE);
        resumed=1;

        fileUri=null;
        flag=0;
        filePath=null;
        abc=null;
        p=-1;
        logo_index=-1;
        image_pos=-1;
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_sharing);


        if (!marshMallowPermission.checkPermissionForCamera()) {
            marshMallowPermission.requestPermissionForCamera();
        }
           /* if (!marshMallowPermission.checkPermissionForExternalStorage()) {
                marshMallowPermission.requestPermissionForExternalStorage();
            }

                if (!marshMallowPermission.checkPermissionForRecord())
                {
                    marshMallowPermission.requestPermissionForRecord();
                }*/
                    File saveDir = null;
                    // Only use external storage directory if permission is granted, otherwise cache directory is used by default
                    saveDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/Kluchit Camera");

                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }





        tr = Analytics.getInstance(this).getDefaultTracker();
        imgPreview = (ImageView) findViewById(R.id.img);
        videoPreview = (VideoView) findViewById(R.id.vid);
        btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
        btnRecordVideo = (Button) findViewById(R.id.btnRecordVideo);
        textview= (TextView) findViewById(R.id.nopic);
        cross=(ImageButton)findViewById(R.id.two).findViewById(R.id.cross);
        txtPercentage = (TextView) findViewById(R.id.txtPercentage);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        queue=(Button)findViewById(R.id.queue);
        /**
         * Capture image button click event
         */
        btnCapturePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // capture picture
                edited=false;
                p=-1;
                logo_index=-1;
                image_pos=-1;

                findViewById(R.id.queue).setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                txtPercentage.setText("Press Button to start uploading...");
                findViewById(R.id.save).setVisibility(View.INVISIBLE);
                captureImage();
            }
        });

        /**
         * Record video button click event
         */
        btnRecordVideo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // record video\\\
                edited=false;
                p=-1;
                logo_index=-1;
                image_pos=-1;
                findViewById(R.id.queue).setVisibility(View.VISIBLE);

                progressBar.setProgress(0);
                txtPercentage.setText("Press Button to start uploading...");
                findViewById(R.id.save).setVisibility(View.INVISIBLE);
                recordVideo();
            }
        });



        cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
            }
        });

        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }

        Button share=(Button) findViewById(R.id.share);




        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (t == MEDIA_TYPE_IMAGE) {

                    String pathofBmp = abc;
                    createInstagramIntent("image/*",pathofBmp);
               /* Uri bmpUri = Uri.parse(pathofBmp);
                  final Intent emailIntent1 = new Intent(Intent.ACTION_SEND);
                  emailIntent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  emailIntent1.putExtra(Intent.EXTRA_STREAM, bmpUri);
                  emailIntent1.setType("image/jpg");
                  startActivity(Intent.createChooser(emailIntent1, "Share Image to"));*/
                    //upload_insta();
                    // upload_photo_video(true);


                } else if (t == MEDIA_TYPE_VIDEO) {

                    //videoPreview.suspend();
                    String path = abc;
                    createInstagramIntent("video/*",path);


                } else {
                    Toast.makeText(SocialSharing.this, "Please Capture an image or video to share", Toast.LENGTH_LONG).show();
                }



            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


              /*  Intent mServiceIntent = new Intent(SocialSharing.this, UploaderService.class);
                mServiceIntent.setData(Uri.parse(fileUri.getPath()));
                startService(mServiceIntent);*/


                // uploading the file to server
                if (already_uploaded==false) {
                    new UploadFileToServer().execute();


                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    /*Intent intent = new Intent(SocialSharing.this, UploaderService.class);
                    SharedPreferences queue=getApplication().getSharedPreferences("Queue",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=queue.edit();
                    editor.putInt("flag",flag);
                    editor.putString("filename",abc);
                    editor.commit();
                    startService(intent);
                    */
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SocialSharing.this);
                    builder.setMessage("File Already Uploaded To Server!").setTitle("Attention")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // do nothing
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        });








        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.hide(getFragmentManager().findFragmentById(R.id.two));
        ft.commit();


        pos=0;

       // textview.setVisibility(View.VISIBLE);
        videoPreview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            public void onPrepared(MediaPlayer mediaPlayer) {

                // close the progress bar and play the video


                //if we have a position on savedInstanceState, the video playback should start from here

                videoPreview.seekTo(pos);

                if (pos == 0) {

                    videoPreview.start();

                } else {

                    //if we come from a resumed activity, video playback will be paused

                    videoPreview.pause();

                }

            }

        });



        /*.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                // Whatever
            }
        });*/

        grid=(GridView)findViewById(R.id.filters).findViewById(R.id.gridView1);
        CustomGrid adapter = new CustomGrid(getApplicationContext(), web, imageId);
        grid.setAdapter(adapter);


        SharedPreferences pref =getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();
        editor.putString("logo_pos", String.valueOf(3));
        // Saving string
        editor.commit();

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                shrink_editing_controls();
                //working
                if (position==0) {
                    p = 0;
                    findViewById(R.id.t_left).setVisibility(View.VISIBLE);
                    findViewById(R.id.t_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.middle).setVisibility(View.INVISIBLE);
                    Toast.makeText(SocialSharing.this,"top left",Toast.LENGTH_LONG).show();

                }
                else if (position==1) {
                    p = 1;
                    findViewById(R.id.t_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.t_right).setVisibility(View.VISIBLE);
                    findViewById(R.id.b_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.middle).setVisibility(View.INVISIBLE);
                    Toast.makeText(SocialSharing.this,"top right",Toast.LENGTH_LONG).show();
                }
                else if (position==2) {
                    p = 2;
                    findViewById(R.id.t_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.t_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_left).setVisibility(View.VISIBLE);
                    findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.middle).setVisibility(View.INVISIBLE);
                    Toast.makeText(SocialSharing.this,"bottom left",Toast.LENGTH_LONG).show();
                }
                else if (position==3) {
                    p = 3;
                    findViewById(R.id.t_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.t_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_right).setVisibility(View.VISIBLE);
                    findViewById(R.id.middle).setVisibility(View.INVISIBLE);
                    Toast.makeText(SocialSharing.this,"bottom right",Toast.LENGTH_LONG).show();
                }
                else if (position==4) {
                    p = 4;
                    findViewById(R.id.t_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.t_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_left).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
                    findViewById(R.id.middle).setVisibility(View.VISIBLE);
                    Toast.makeText(SocialSharing.this,"middle",Toast.LENGTH_LONG).show();
                }
                shrink_editing_controls();

                editor.putString("logo_pos", String.valueOf(position));
                // Saving string
                editor.commit();


                findViewById(R.id.save).setVisibility(View.VISIBLE);
            }
        });



        ListView logo_list=(ListView) findViewById(R.id.logo_window).findViewById(R.id.list);
        logo_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                logo_index=position;
                image_pos=position;
                final int[] imageId = {
                        R.drawable.log_one_a,
                        R.drawable.log_two_a,
                        R.drawable.log_three_a,
                        R.drawable.log_four_a,
                        R.drawable.log_five_a,
                        R.drawable.log_six_a
                };

                findViewById(R.id.t_left).setBackgroundResource(imageId[position]);
                findViewById(R.id.t_right).setBackgroundResource(imageId[position]);
                findViewById(R.id.b_left).setBackgroundResource(imageId[position]);
                findViewById(R.id.b_right).setBackgroundResource(imageId[position]);
                findViewById(R.id.middle).setBackgroundResource(imageId[position]);
                hide_logo_chooser();

                findViewById(R.id.save).setVisibility(View.VISIBLE);
            }
        });

        hide_editing_controls();

        findViewById(R.id.down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_logo_position_selector();
            }
        });

        findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_logo_chooser();
            }
        });

        findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide_logo_chooser();
            }
        });

        findViewById(R.id.up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide_logo_position_selector();
            }
        });


        final Button save=(Button)findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (image_pos==-1 || p==-1 || logo_index==-1)
                {
                    Toast.makeText(SocialSharing.this,"Please select logo position and type of logo",Toast.LENGTH_LONG).show();
                }
                else {
                    edited = true;
                    if (flag == 1) {
                        already_uploaded = false;
                        btnUpload.setVisibility(View.VISIBLE);
                        filePath = abc;

                        Bitmap bmp = BitmapFactory.decodeFile(filePath);
                        bmp = addWatermark(getResources(), bmp);

                        File file = new File(filePath);
                        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayBitmapStream);
                        byte[] b = byteArrayBitmapStream.toByteArray();

                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(b);
                            fos.close();
                            Toast.makeText(SocialSharing.this, "Picture saved to " + file.getPath(), Toast.LENGTH_LONG).show();

                        } catch (FileNotFoundException e) {
                            //  Log.e(TAG, "File not found: " + e.getMessage());
                            e.getStackTrace();
                        } catch (IOException e) {
                            //  Log.e(TAG, "I/O error writing file: " + e.getMessage());
                            e.getStackTrace();
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).show(getFragmentManager().findFragmentById(R.id.two)).commit();
                            }
                        }, 2000);
                        save.setVisibility(View.INVISIBLE);
                        hide_editing_controls();
                        previewCapturedImage();

                    }
                    if (flag == 2) {
                        findViewById(R.id.save).setVisibility(View.INVISIBLE);

                        hide_editing_controls();


                        add_watermark();
                        add_watermark_vid();

                        final File file;
                        if (abc != null)
                            file = new File(abc);
                        else
                            file = new File(fileUri.getPath());

                        GeneralUtils.checkForPermissionsMAndAbove(SocialSharing.this, true);
                        LoadJNI vk = new LoadJNI();
                        try {
                            String workFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                            String filename = getFileName(fileUri);
                            String[] complexCommand = null;

                            //                    complexCommand = new String[] {"ffmpeg", "-y", "-i", imagepath, "-strict", "experimental", "-vf", "movie=/sdcard/Pictures/Kluchit/watermark.PNG [watermark]; [in][watermark] overlay=main_w-overlay_w-10:10 [out]", "-s", "320x240", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/sdcard/Pictures/Kluchit/" + filename};


                            if (abc != null) {
                                if (p == 0)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", abc, "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w-overlay_w-10:10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 1)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", abc, "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=10:10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 2)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", abc, "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w-overlay_w-10:main_h-overlay_h-10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 3)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", abc, "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=10:main_h-overlay_h-10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", abc, "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w/2-overlay_w/2:main_h/2-overlay_h/2 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                            } else {
                                if (p == 0)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", fileUri.getPath(), "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w-overlay_w-10:10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 1)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", fileUri.getPath(), "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=10:10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 2)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", fileUri.getPath(), "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w-overlay_w-10:main_h-overlay_h-10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else if (p == 3)
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", fileUri.getPath(), "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=10:main_h-overlay_h-10 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                                else
                                    complexCommand = new String[]{"ffmpeg", "-y", "-i", fileUri.getPath(), "-strict", "experimental", "-vf", "movie=/storage/emulated/0/Pictures/Kluchit Camera/watermark_vid.PNG , scale=80:80 [watermark]; [in][watermark] overlay=main_w/2-overlay_w/2:main_h/2-overlay_h/2 [out]", "-s", "1024x768", "-r", "30", "-b", "15496k", "-vcodec", "mpeg4", "-ab", "48000", "-ac", "2", "-ar", "22050", "/storage/emulated/0/Pictures/Kluchit Camera/k" + filename};
                            }
                            ringProgressDialog = ProgressDialog.show(SocialSharing.this, "Please wait ...", "Editing Video ...", true);
                            ringProgressDialog.setCancelable(false);
                            ringProgressDialog.show();
                            vk.run(complexCommand, "/storage/emulated/0/Pictures/Kluchit Camera", getApplicationContext());
                            ringProgressDialog.dismiss();
                            Toast.makeText(SocialSharing.this, String.format("Saved to: %s, size: %s", file.getAbsolutePath(), fileSize(file)), Toast.LENGTH_LONG).show();


                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).show(getFragmentManager().findFragmentById(R.id.two)).commit();
                            //filepath = "/sdcard/Pictures/Kluchit/" + getFileName(data.getData());
                            videoPreview.pause();
                            File old_file = new File(abc);
                            file.delete();

                            abc = "/storage/emulated/0/Pictures/Kluchit Camera/" + "k" + filename;
                            File file_ = new File(abc);
                            fileUri = Uri.fromFile(file);
                            videoPreview.setVideoPath(abc);
                            // start playing
                            videoPreview.start();
                        } catch (Throwable e) {
                            Log.e("test", "vk run exception.", e);
                        }


                    }
                }
            }

        });

        save.setVisibility(View.INVISIBLE);

        hide_editing_controls();

        findViewById(R.id.b_right).setVisibility(View.INVISIBLE);


        queue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadPreferenceManager p=new UploadPreferenceManager(getApplicationContext());
                p.add_upload(abc);
                Toast.makeText(getApplicationContext(),
                        "Image Added to Upload Queue", Toast.LENGTH_SHORT)
                        .show();
                findViewById(R.id.queue).setVisibility(View.INVISIBLE);
            }
        });
    }




    /**
     * Checking device has camera hardware or not
     * */
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * Capturing Camera Image will lauch camera app requrest image capture
     */
    private void captureImage() {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                // start the image capture Intent
                startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);

        }



    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation
        // changes

        outState.putParcelable("file_uri", fileUri);
        outState.putBoolean("already_uploaded",already_uploaded);
        outState.putString("filePath",filePath);
        outState.putString("filename",filename);
        outState.putInt("flag",flag);
        outState.putString("abc",abc);
        outState.putBoolean("edited",edited);
        outState.putInt("p",p);
        outState.putInt("logo_index",logo_index);
        outState.putInt("image_pos",image_pos);
        if (findViewById(R.id.queue).getVisibility()==View.VISIBLE)
        outState.putInt("queue",1);
        else
            outState.putInt("queue",0);


        if (flag!=0) {
            if (findViewById(R.id.two).getVisibility()==View.VISIBLE)
                outState.putInt("upload_fragment", 1);
            else
                outState.putInt("upload_fragment", 0);

            if (findViewById(R.id.logo_window).getVisibility()==View.VISIBLE)
                outState.putInt("logo_window", 1);
            else
                outState.putInt("logo_window", 0);

            if (findViewById(R.id.filters).getVisibility()==View.VISIBLE)
                outState.putInt("filters", 1);
            else
                outState.putInt("filters", 0);

        }
        if (flag==2) {
            //videoPreview.pause();
            outState.putInt("Position", videoPreview.getCurrentPosition());
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //re_initalize_listeners();


        fileUri = savedInstanceState.getParcelable("file_uri");
        filePath = savedInstanceState.getParcelable("filePath");
        filename = savedInstanceState.getParcelable("filename");
        already_uploaded = savedInstanceState.getBoolean("already_uploaded");
        flag = savedInstanceState.getInt("flag");
        abc = savedInstanceState.getString("abc");
        edited = savedInstanceState.getBoolean("edited");


        p = -1;
        logo_index = -1;
        image_pos = -1;


        int x;
        if (savedInstanceState.containsKey("logo_window")) {
            x = savedInstanceState.getInt("logo_window");
            if (x == 1) {
                show_logo_chooser();
            } else {
                hide_logo_chooser();
            }
        }

        if (savedInstanceState.containsKey("filters")) {
            x = savedInstanceState.getInt("filters");
            if (x == 1) {
                show_logo_position_selector();
            } else {
                hide_logo_position_selector();
            }
        }


        if (savedInstanceState.containsKey("upload_fragment")) {
            x = savedInstanceState.getInt("upload_fragment");
            if (x == 1) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).show(getFragmentManager().findFragmentById(R.id.two)).commit();
            } else {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
            }
        }
        if (flag == 0)
            hide_editing_controls();

        if (flag == 2) {
            if (abc != null)
                videoPreview.setVideoPath(abc);
            else {
                if (fileUri != null) {
                    videoPreview.setVideoPath(fileUri.getPath());
                    videoPreview.start();
                }
            }
        }
        if (edited == true)
            hide_editing_controls();

        int queue = savedInstanceState.getInt("queue");
        if (queue == 0)
            findViewById(R.id.queue).setVisibility(View.INVISIBLE);
        else
            findViewById(R.id.queue).setVisibility(View.VISIBLE);

    }

    /**
     * Recording video
     */
    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file
        // name

        // start the video capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
    }



    void add_watermark()
    {
        final int[] imageId = {
                R.drawable.log_one_a,
                R.drawable.log_two_a,
                R.drawable.log_three_a,
                R.drawable.log_four_a,
                R.drawable.log_five_a,
                R.drawable.log_six_a
        };

        Bitmap bm = BitmapFactory.decodeResource(getResources(), imageId[logo_index]);
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/Pictures/Kluchit Camera";
        File file = new File(extStorageDirectory, "watermark.PNG");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        try {
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // }
    }

    void add_watermark_vid()
    {

        final int[] imageId = {
                R.drawable.log_one_a,
                R.drawable.log_two_a,
                R.drawable.log_three_a,
                R.drawable.log_four_a,
                R.drawable.log_five_a,
                R.drawable.log_six_a
        };

        Bitmap bm = rotateImage(BitmapFactory.decodeResource(getResources(), imageId[logo_index]),-90);
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/Pictures/Kluchit Camera";
        File file = new File(extStorageDirectory, "watermark_vid.PNG");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        try {
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


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
    /**
     * Receiving activity result method will be called after closing the camera
     * */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Button btn=(Button)findViewById(R.id.share);
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                flag=1;

                findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
                already_uploaded=false;
                previewCapturedImage();
                btn.setVisibility(View.VISIBLE);
                btnUpload.setVisibility(View.VISIBLE);
                abc=fileUri.getPath();


              /*  new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).show(getFragmentManager().findFragmentById(R.id.two)).commit();
                    }
                }, 10);*/


                show_editing_controls();


                findViewById(R.id.b_right).setVisibility(View.INVISIBLE);
            } else if (resultCode == RESULT_CANCELED) {

                flag=0;
                abc=null;
                if (textview.getVisibility() == View.INVISIBLE)
                    textview.setVisibility(View.VISIBLE);

                btn.setVisibility(View.INVISIBLE);
                btnUpload.setVisibility(View.INVISIBLE);
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();

                already_uploaded=false;

                //new Handler().postDelayed(new Runnable() {
                 //   @Override
                   // public void run() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
                 //   }
               // }, 500);

                hide_editing_controls();

            } else {

                flag=0;
                abc=null;
                already_uploaded=false;

                if (textview.getVisibility() == View.INVISIBLE)
                    textview.setVisibility(View.VISIBLE);

                btn.setVisibility(View.INVISIBLE);
                btnUpload.setVisibility(View.INVISIBLE);
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();



               // new Handler().postDelayed(new Runnable() {
                 //   @Override
                 //   public void run() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
              //      }
              //  }, 500);

                hide_editing_controls();

            }
        } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {


                flag=2;
                already_uploaded=false;

                // video successfully recorded
                // preview the recorded video
                btn.setVisibility(View.VISIBLE);
                btnUpload.setVisibility(View.VISIBLE);
                previewVideo();
                abc=fileUri.getPath();

               // new Handler().postDelayed(new Runnable() {
                //    @Override
                //    public void run() {
                   //     FragmentTransaction ft = getFragmentManager().beginTransaction();
                    //    ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).show(getFragmentManager().findFragmentById(R.id.two)).commit();
               //     }
               // }, 500);


                show_editing_controls();

                findViewById(R.id.b_right).setVisibility(View.INVISIBLE);

            } else if (resultCode == RESULT_CANCELED) {

                abc=null;
                flag=0;
                already_uploaded=false;

                if (textview.getVisibility() == View.INVISIBLE)
                    textview.setVisibility(View.VISIBLE);

                btn.setVisibility(View.INVISIBLE);
                btnUpload.setVisibility(View.INVISIBLE);


              //  new Handler().postDelayed(new Runnable() {
                //    @Override
               //     public void run() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
                //    }
              //  }, 500);

                // user cancelled recording
                Toast.makeText(getApplicationContext(),
                        "User cancelled video recording", Toast.LENGTH_SHORT)
                        .show();

                hide_editing_controls();
            } else {

                flag=0;
                already_uploaded=false;
                abc=null;


                if (textview.getVisibility() == View.INVISIBLE)
                    textview.setVisibility(View.VISIBLE);


                btn.setVisibility(View.INVISIBLE);
                btnUpload.setVisibility(View.INVISIBLE);


            //    new Handler().postDelayed(new Runnable() {
               //     @Override
              //      public void run() {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit, R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit).hide(getFragmentManager().findFragmentById(R.id.two)).commit();
               //     }
             //   }, 500);

                // failed to record video
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to record video", Toast.LENGTH_SHORT)
                        .show();

                hide_editing_controls();
            }
        }
    }
    private String readableFileSize(long size) {
        if (size <= 0) return size + " B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String fileSize(File file) {
        return readableFileSize(file.length());
    }

    /**
     * Display image from a path to ImageView
     */
    private void previewCapturedImage() {
        try {
            // hide video preview
            if (textview.getVisibility() == View.VISIBLE)
                textview.setVisibility(View.INVISIBLE);


            videoPreview.setVisibility(View.GONE);

            imgPreview.setVisibility(View.VISIBLE);


            Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath());


            Matrix matrix = new Matrix();



            ExifInterface ei = new ExifInterface(fileUri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            Bitmap rotatedBitmap=bitmap;

            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap=rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap=rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap=rotateImage(bitmap, 270);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    break;
            }



            File file = new File(fileUri.getPath());
            filename=file.getName();
            ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayBitmapStream);
            byte[] b = byteArrayBitmapStream.toByteArray();

            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(b);
                fos.close();
                Toast.makeText(SocialSharing.this,"Picture saved to "+file.getPath(),Toast.LENGTH_LONG).show();

            } catch (FileNotFoundException e) {
                //  Log.e(TAG, "File not found: " + e.getMessage());
                e.getStackTrace();
            } catch (IOException e) {
                //  Log.e(TAG, "I/O error writing file: " + e.getMessage());
                e.getStackTrace();
            }


            //Bitmap temp=rotatedBitmap.createScaledBitmap(rotatedBitmap,imgPreview.getMaxWidth(),imgPreview.getMaxHeight(),true);

            imgPreview.setImageBitmap(rotatedBitmap);


        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Bitmap scaleBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();
        m.setScale((float) wantedWidth / bitmap.getWidth(), (float) wantedHeight / bitmap.getHeight());
        canvas.drawBitmap(bitmap, m, new Paint());

        return output;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    /**
     * Previewing recorded video
     */
    private void previewVideo() {
        try {
            // hide image preview

          //  textview.setVisibility(textview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (textview.getVisibility() == View.VISIBLE)
                textview.setVisibility(View.INVISIBLE);


            imgPreview.setVisibility(View.GONE);

            videoPreview.setVisibility(View.VISIBLE);
            videoPreview.setVideoPath(abc);
            // start playing
            videoPreview.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ------------ Helper Methods ----------------------
     * */

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            t=MEDIA_TYPE_IMAGE;
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            t=MEDIA_TYPE_VIDEO;
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        } else {
            t=0;
            return null;
        }

        return mediaFile;
    }


    void upload_insta()
    {
        String upload= EndPoints.BASE_URL+"upload_insta.php";


        StringRequest stringRequest = new StringRequest(Request.Method.POST, upload,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        ringProgressDialog.dismiss();
                        Toast.makeText(SocialSharing.this,s,Toast.LENGTH_LONG).show();

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



                Bitmap bitmap = BitmapFactory.decodeFile(abc);
                Matrix matrix = new Matrix();

                matrix.postRotate(90);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth(),bitmap.getHeight(),true);

                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);

                String encodedImage=getStringImage(rotatedBitmap);
                //Adding parameters
                params.put("userImage", encodedImage);
                String timeStamp = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())).toString();

                params.put("filename",timeStamp+".jpeg");

                //returning parameters
                return params;
            }
        };


        ringProgressDialog = ProgressDialog.show(SocialSharing.this, "Please wait ...",	"Checking Credentials ...", true);
        //ringProgressDialog.setCancelable(true);
        ringProgressDialog.show();
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);

    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (flag==2)
        videoPreview.pause();
    }

    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero

            SocialSharing.this.setRequestedOrientation(SocialSharing.this.getResources().getConfiguration().orientation);

            progressBar.setProgress(0);
            txtPercentage.setText("Press Button to start uploading...");

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            progressBar.setVisibility(View.VISIBLE);

            // updating progress bar value
            progressBar.setProgress(progress[0]);

            // updating percentage value
            txtPercentage.setText(String.valueOf(progress[0]) + "%");
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

                File sourceFile=null;
                if (abc!=null)
                    sourceFile = new File(abc);
                else
                {
                    if (fileUri!=null)
                    sourceFile=new File(fileUri.getPath());
                }

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
            Log.e(TAG, "Response from server: " + result);

            // showing the server response in an alert dialog
            showAlert(result);
            SocialSharing.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            super.onPostExecute(result);
        }

    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Response from Servers")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        if (message.contains("successfully"))
            already_uploaded=true;

       // new Handler().postDelayed(new Runnable() {
        //    @Override
         //   public void run() {

                txtPercentage.setText("Upload completed");

        //    }
      //  }, 500);

        if (flag==1)
        send_image_db_request();


        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

    }


    void send_image_db_request()
    {
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }


    public static Bitmap addWatermark(Resources res, Bitmap source) {
        int w, h;
        Canvas c;
        Paint paint;
        Bitmap bmp, watermark;

        Matrix matrix;
        float scale;
        RectF r;

        w = source.getWidth();
        h = source.getHeight();

        // Create the new bitmap
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

        // Copy the original bitmap into the new one
        c = new Canvas(bmp);
        c.drawBitmap(source, 0, 0, paint);

        // Load the watermark
        final int[] imageId = {
                R.drawable.log_one_a,
                R.drawable.log_two_a,
                R.drawable.log_three_a,
                R.drawable.log_four_a,
                R.drawable.log_five_a,
                R.drawable.log_six_a
        };


        watermark = BitmapFactory.decodeResource(res, imageId[image_pos]);
        // Scale the watermark to be approximately 10% of the source image height
        scale = (float) (((float) h * 0.1) / (float) watermark.getHeight());

        // Create the matrix
        matrix = new Matrix();
        matrix.postScale(scale, scale);
        // Determine the post-scaled size of the watermark
        r = new RectF(0, 0, watermark.getWidth(), watermark.getHeight());
        matrix.mapRect(r);




        if (p==0)//wrong
            matrix.postTranslate(w-r.width(),0);
            // Move the watermark to the top left corner
        else if (p==1)
            // Move the watermark to the top right corner
        matrix.postTranslate(0,0);

        else if (p==2)//wrong
            // Move the watermark to the bottom left corner
            matrix.postTranslate(w - r.width(), h - r.height());
        else if (p==3)
            // Move the watermark to the bottom right corner
            matrix.postTranslate(0, h - r.height());
        else if (p==4)
            // Move the watermark to the middle
            matrix.postTranslate(w/2 - r.width()/2,h/2 - r.height()/2);







        // Draw the watermark
        c.drawBitmap(watermark, matrix, paint);
        // Free up the bitmap memory
        watermark.recycle();

        return bmp;
    }

    public class CustomGrid extends BaseAdapter {
        private Context mContext;
        private final String[] web;
        private final int[] Imageid;

        public CustomGrid(Context c,String[] web,int[] Imageid ) {
            mContext = c;
            this.Imageid = Imageid;
            this.web = web;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return 5;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            View grid;
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {

                grid = new View(mContext);
                grid = inflater.inflate(R.layout.icon, null);
                ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
                imageView.setImageResource(Imageid[position]);
            } else {
                grid = (View) convertView;
            }

            return grid;
        }
    }

}
