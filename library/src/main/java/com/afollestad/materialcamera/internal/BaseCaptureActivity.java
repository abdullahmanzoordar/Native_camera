package com.afollestad.materialcamera.internal;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.TimeLimitReachedException;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class BaseCaptureActivity extends AppCompatActivity implements BaseCaptureInterface {

    private int mCameraPosition = CAMERA_POSITION_UNKNOWN;
    private boolean mRequestingPermission;
    private long mRecordingStart = -1;
    private long mRecordingEnd = -1;
    private long mLengthLimit = -1;
    private Object mFrontCameraId;
    private Object mBackCameraId;
    private boolean mDidRecord = false;

    public static final int PERMISSION_RC = 69;

    @IntDef({CAMERA_POSITION_UNKNOWN, CAMERA_POSITION_BACK, CAMERA_POSITION_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraPosition {
    }

    public static final int CAMERA_POSITION_UNKNOWN = 0;
    public static final int CAMERA_POSITION_FRONT = 1;
    public static final int CAMERA_POSITION_BACK = 2;

    @Override
    protected final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("camera_position", mCameraPosition);
        outState.putBoolean("requesting_permission", mRequestingPermission);
        outState.putLong("recording_start", mRecordingStart);
        outState.putLong("recording_end", mRecordingEnd);
        outState.putLong(CameraIntentKey.LENGTH_LIMIT, mLengthLimit);
        if (mFrontCameraId instanceof String) {
            outState.putString("front_camera_id_str", (String) mFrontCameraId);
            outState.putString("back_camera_id_str", (String) mBackCameraId);
        } else {
            if (mFrontCameraId != null)
                outState.putInt("front_camera_id_int", (Integer) mFrontCameraId);
            if (mBackCameraId != null)
                outState.putInt("back_camera_id_int", (Integer) mBackCameraId);
        }
    }

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CameraUtil.hasCamera(this)) {
            new MaterialDialog.Builder(this)
                    .title(R.string.mcam_error)
                    .content(R.string.mcam_video_capture_unsupported)
                    .positiveText(android.R.string.ok)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    }).show();
            return;
        }
        setContentView(R.layout.mcam_activity_videocapture);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final int primaryColor = getIntent().getIntExtra(CameraIntentKey.PRIMARY_COLOR, 0);
            final Window window = getWindow();
            window.setStatusBarColor(CameraUtil.darkenColor(primaryColor));
            window.setNavigationBarColor(primaryColor);
        }

        if (null == savedInstanceState) {
            checkPermissions();
            mLengthLimit = getIntent().getLongExtra(CameraIntentKey.LENGTH_LIMIT, -1);
        } else {
            mCameraPosition = savedInstanceState.getInt("camera_position", -1);
            mRequestingPermission = savedInstanceState.getBoolean("requesting_permission", false);
            mRecordingStart = savedInstanceState.getLong("recording_start", -1);
            mRecordingEnd = savedInstanceState.getLong("recording_end", -1);
            mLengthLimit = savedInstanceState.getLong(CameraIntentKey.LENGTH_LIMIT, -1);
            if (savedInstanceState.containsKey("front_camera_id_str")) {
                mFrontCameraId = savedInstanceState.getString("front_camera_id_str");
                mBackCameraId = savedInstanceState.getString("back_camera_id_str");
            } else {
                mFrontCameraId = savedInstanceState.getInt("front_camera_id_int");
                mBackCameraId = savedInstanceState.getInt("back_camera_id_int");
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(getApplicationContext(), "landscape", Toast.LENGTH_LONG).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(getApplicationContext(), "portrait", Toast.LENGTH_LONG).show();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            showInitialRecorder();
            return;
        }
        final boolean videoGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (videoGranted) {
            showInitialRecorder();
        } else {
            final boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            String[] perms;
            if (audioGranted) perms = new String[]{Manifest.permission.CAMERA};
            else perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(this, perms, PERMISSION_RC);
            mRequestingPermission = true;
        }
    }

    @Override
    protected final void onPause() {
        super.onPause();
       /* if (!isFinishing() && !isChangingConfigurations() && !mRequestingPermission)
            finish();*/
    }

    @Override
    public final void onBackPressed() {
        Fragment frag = getFragmentManager().findFragmentById(R.id.container);
        if (frag != null) {
            if (frag instanceof PlaybackVideoFragment && allowRetry()) {
                onRetry(((CameraUriInterface) frag).getOutputUri());
                return;
            } else if (frag instanceof BaseCameraFragment) {
                ((BaseCameraFragment) frag).cleanup();
            }
        }
        finish();
    }

    @NonNull
    public abstract Fragment getFragment();

    public final Fragment createFragment() {
        Fragment frag = getFragment();
        frag.setArguments(getIntent().getExtras());
        return frag;
    }

    @Override
    public void setRecordingStart(long start) {
        mRecordingStart = start;
        if (start > -1 && hasLengthLimit())
            setRecordingEnd(mRecordingStart + getLengthLimit());
        else setRecordingEnd(-1);
    }

    @Override
    public long getRecordingStart() {
        return mRecordingStart;
    }

    @Override
    public void setRecordingEnd(long end) {
        mRecordingEnd = end;
    }

    @Override
    public long getRecordingEnd() {
        return mRecordingEnd;
    }

    @Override
    public long getLengthLimit() {
        return mLengthLimit;
    }

    @Override
    public boolean hasLengthLimit() {
        return getLengthLimit() > -1;
    }

    @Override
    public boolean countdownImmediately() {
        return getIntent().getBooleanExtra(CameraIntentKey.COUNTDOWN_IMMEDIATELY, false);
    }

    @Override
    public void setCameraPosition(int position) {
        mCameraPosition = position;
    }

    @Override
    public void toggleCameraPosition() {
        if (getCurrentCameraPosition() == CAMERA_POSITION_FRONT) {
            // Front, go to back if possible
            if (getBackCamera() != null)
                setCameraPosition(CAMERA_POSITION_BACK);
        } else {
            // Back, go to front if possible
            if (getFrontCamera() != null)
                setCameraPosition(CAMERA_POSITION_FRONT);
        }
    }

    @Override
    public int getCurrentCameraPosition() {
        return mCameraPosition;
    }

    @Override
    public Object getCurrentCameraId() {
        if (getCurrentCameraPosition() == CAMERA_POSITION_FRONT)
            return getFrontCamera();
        else return getBackCamera();
    }

    @Override
    public void setFrontCamera(Object id) {
        mFrontCameraId = id;
    }

    @Override
    public Object getFrontCamera() {
        return mFrontCameraId;
    }

    @Override
    public void setBackCamera(Object id) {
        mBackCameraId = id;
    }

    @Override
    public Object getBackCamera() {
        return mBackCameraId;
    }

    private void showInitialRecorder() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, createFragment())
                .commit();
    }

    @Override
    public final void onRetry(@Nullable String outputUri) {
        if (outputUri != null)
            deleteOutputFile(outputUri);
        if (!shouldAutoSubmit() || restartTimerOnRetry())
            setRecordingStart(-1);
        if (getIntent().getBooleanExtra(CameraIntentKey.RETRY_EXITS, false)) {
            setResult(RESULT_OK, new Intent()
                    .putExtra(MaterialCamera.STATUS_EXTRA, MaterialCamera.STATUS_RETRY));
            finish();
            return;
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.container, createFragment())
                .commit();
    }

    @Override
    public final void onShowPreview(@Nullable final String outputUri, boolean countdownIsAtZero) {
        if ((shouldAutoSubmit() && (countdownIsAtZero || !allowRetry() || !hasLengthLimit())) || outputUri == null) {
            if (outputUri == null) {
                setResult(RESULT_CANCELED, new Intent().putExtra(MaterialCamera.ERROR_EXTRA,
                        new TimeLimitReachedException()));
                finish();
                return;
            }
            useVideo(outputUri);
        } else {
            if (!hasLengthLimit() || !continueTimerInPlayback()) {
                // No countdown or countdown should not continue through playback, reset timer to 0
                setRecordingStart(-1);
            }
            Fragment frag = PlaybackVideoFragment.newInstance(outputUri, allowRetry(),
                    getIntent().getIntExtra(CameraIntentKey.PRIMARY_COLOR, 0));
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, frag)
                    .commit();
        }
    }

    @Override
    public final boolean allowRetry() {
        return getIntent().getBooleanExtra(CameraIntentKey.ALLOW_RETRY, true);
    }

    @Override
    public final boolean shouldAutoSubmit() {
        return getIntent().getBooleanExtra(CameraIntentKey.AUTO_SUBMIT, false);
    }

    private void deleteOutputFile(@Nullable String uri) {
        if (uri != null)
            //noinspection ResultOfMethodCallIgnored
            new File(Uri.parse(uri).getPath()).delete();
    }

    @Override
    protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_RC) showInitialRecorder();
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestingPermission = false;
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            new MaterialDialog.Builder(this)
                    .title(R.string.mcam_permissions_needed)
                    .content(R.string.mcam_video_perm_warning)
                    .positiveText(android.R.string.ok)
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    }).show();
        } else {
            showInitialRecorder();
        }
    }

    @Override
    public final void useVideo(String uri) {
        if (uri != null) {
            setResult(Activity.RESULT_OK, getIntent()
                    .putExtra(MaterialCamera.STATUS_EXTRA, MaterialCamera.STATUS_RECORDED)
                    .setDataAndType(Uri.parse(uri), "video/mp4"));
        }

        finish();//finishing video capture

    }

    @Override
    public void setDidRecord(boolean didRecord) {
        mDidRecord = didRecord;
    }

    @Override
    public boolean didRecord() {
        return mDidRecord;
    }

    @Override
    public boolean restartTimerOnRetry() {
        return getIntent().getBooleanExtra(CameraIntentKey.RESTART_TIMER_ON_RETRY, false);
    }

    @Override
    public boolean continueTimerInPlayback() {
        return getIntent().getBooleanExtra(CameraIntentKey.CONTINUE_TIMER_IN_PLAYBACK, false);
    }

    @Override
    public int videoEncodingBitRate(int defaultVal) {
        return getIntent().getIntExtra(CameraIntentKey.VIDEO_BIT_RATE, defaultVal);
    }

    @Override
    public int audioEncodingBitRate(int defaultVal) {
        return getIntent().getIntExtra(CameraIntentKey.AUDIO_ENCODING_BIT_RATE, defaultVal);
    }

    @Override
    public int videoFrameRate(int defaultVal) {
        return getIntent().getIntExtra(CameraIntentKey.VIDEO_FRAME_RATE, defaultVal);
    }

    @Override
    public float videoPreferredAspect() {
        return getIntent().getFloatExtra(CameraIntentKey.VIDEO_PREFERRED_ASPECT, 4f / 3f);
    }

    @Override
    public int videoPreferredHeight() {
        return getIntent().getIntExtra(CameraIntentKey.VIDEO_PREFERRED_HEIGHT, 720);
    }

    @Override
    public long maxAllowedFileSize() {
        return getIntent().getLongExtra(CameraIntentKey.MAX_ALLOWED_FILE_SIZE, -1);
    }

    @Override
    public int qualityProfile() {
        return getIntent().getIntExtra(CameraIntentKey.QUALITY_PROFILE, CamcorderProfile.QUALITY_HIGH);
    }

    @DrawableRes
    @Override
    public int iconPause() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_PAUSE, R.drawable.evp_action_pause);
    }

    @DrawableRes
    @Override
    public int iconPlay() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_PLAY, R.drawable.evp_action_play);
    }

    @DrawableRes
    @Override
    public int iconRestart() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_RESTART, R.drawable.evp_action_restart);
    }

    @DrawableRes
    @Override
    public int iconRearCamera() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_REAR_CAMERA, R.drawable.mcam_camera_rear);
    }

    @DrawableRes
    @Override
    public int iconFrontCamera() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_FRONT_CAMERA, R.drawable.mcam_camera_front);
    }

    @DrawableRes
    @Override
    public int iconStop() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_STOP, R.drawable.mcam_action_stop);
    }

    @DrawableRes
    @Override
    public int iconRecord() {
        return getIntent().getIntExtra(CameraIntentKey.ICON_RECORD, R.drawable.mcam_action_capture);
    }

    @StringRes
    @Override
    public int labelRetry() {
        return getIntent().getIntExtra(CameraIntentKey.LABEL_RETRY, R.string.mcam_retry);
    }

    @StringRes
    @Override
    public int labelUseVideo() {
        return getIntent().getIntExtra(CameraIntentKey.LABEL_USE_VIDEO, R.string.mcam_use_video);
    }
}