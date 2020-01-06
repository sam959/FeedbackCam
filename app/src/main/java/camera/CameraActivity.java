/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package camera;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.androidexperiments.shadercam.fragments.PermissionsHelper;
import com.androidexperiments.shadercam.fragments.VideoFragment;
import com.androidexperiments.shadercam.gl.VideoRenderer;
import com.androidexperiments.shadercam.utils.ShaderUtils;
import com.example.feedbackcam.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import renderer.ShaderRenderer;

public class CameraActivity extends FragmentActivity implements PermissionsHelper.PermissionsListener,
        ShaderRenderer.OnTakePicturePressedListener, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";
    private static final String TEST_VIDEO_FILE_NAME = "test_video.mp4";
    protected VideoRenderer mVideoRenderer;
    private boolean savePicture = false;
    private RecordableSurfaceView mRecordableSurfaceView;
    private FloatingActionButton takePictureBtn;
    private FloatingActionButton showMenuButton;
    private FloatingActionButton toggleMirror;
    private ImageView swapButton;
    private View menu;
    private SeekBar seekBar;
    private VideoFragment mVideoFragment;
    private PermissionsHelper mPermissionsHelper;
    private boolean mPermissionsSatisfied;
    private boolean isMenuHidden = false;
    private File mOutputFile;
    private int selectedEffect;

    public int getSelectedEffect() {
        return selectedEffect;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        mVideoRenderer = new ShaderRenderer(this);
        setUpUIComponents();
        if (PermissionsHelper.isMorHigher())
            setupPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shutdownCamera();
        mRecordableSurfaceView.pause();
    }

    private void setUpUIComponents() {
        mRecordableSurfaceView = new RecordableSurfaceView(this);
        mRecordableSurfaceView = findViewById(R.id.texture_view);
        takePictureBtn = findViewById(R.id.btn_take_picture);
        showMenuButton = findViewById(R.id.btn_menu);
        swapButton = findViewById(R.id.btn_swap_camera);
        seekBar = findViewById(R.id.slider);
        toggleMirror = findViewById(R.id.toggle_mirror);
        swapButton.setOnClickListener(this);
        takePictureBtn.setOnClickListener(this);
        showMenuButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        toggleMirror.setOnClickListener(this);
        seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(1));

        setUpMenu();
    }

    private void setUpMenu() {
        menu = findViewById(R.id.custom_menu);

        TextView threshold = findViewById(R.id.threshold);
        TextView hue = findViewById(R.id.hue);
        TextView opacity = findViewById(R.id.opacity);
        TextView dispX = findViewById(R.id.dispX);
        TextView dispY = findViewById(R.id.dispY);
        TextView contrast = findViewById(R.id.contrast);
        TextView saturation = findViewById(R.id.saturation);
        TextView brightness = findViewById(R.id.brightness);
        TextView zoom = findViewById(R.id.zoom);

        threshold.setOnClickListener(this);
        hue.setOnClickListener(this);
        opacity.setOnClickListener(this);
        dispX.setOnClickListener(this);
        dispY.setOnClickListener(this);
        contrast.setOnClickListener(this);
        saturation.setOnClickListener(this);
        brightness.setOnClickListener(this);
        zoom.setOnClickListener(this);
    }

    private void setupPermissions() {
        mPermissionsHelper = PermissionsHelper.attach(this);
        mPermissionsHelper.setRequestedPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    private void setupVideoFragment(VideoRenderer renderer) {
        mVideoFragment = VideoFragment.getInstance();
        //mRecordableSurfaceView.setRenderMode(RecordableSurfaceView.RENDERMODE_CONTINUOUSLY);
        mVideoFragment.setRecordableSurfaceView(mRecordableSurfaceView);
        mVideoFragment.setVideoRenderer(renderer);
        mVideoFragment.setCameraToUse(VideoFragment.CAMERA_PRIMARY);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mVideoFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        ShaderUtils.goFullscreen(this.getWindow());
        if (PermissionsHelper.isMorHigher()) {
            if (mPermissionsHelper.checkPermissions()) {
                if (mVideoRenderer == null) {
                    mVideoRenderer = new VideoRenderer(this);
                }
                setupVideoFragment(mVideoRenderer);
                mRecordableSurfaceView.resume();

                mOutputFile = getVideoFile();
                android.graphics.Point size = new android.graphics.Point();
                getWindowManager().getDefaultDisplay().getRealSize(size);
                try {
                    mRecordableSurfaceView.initRecorder(mOutputFile, size.x, size.y, null, null);
                } catch (IOException ioex) {
                    Log.e(TAG, "Couldn't re-init recording", ioex);
                }
            }
        }
    }

    private File getVideoFile() {
        return new File(Environment.getExternalStorageDirectory(), TEST_VIDEO_FILE_NAME);
    }

    private void shutdownCamera() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(mVideoFragment);
        ft.commit();
        mVideoFragment = null;
    }

    @Override
    public void onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()");
        mPermissionsSatisfied = true;
    }

    @Override
    public void onPermissionsFailed(String[] failedPermissions) {
        Log.e(TAG, "onPermissionsFailed()" + Arrays.toString(failedPermissions));
        mPermissionsSatisfied = false;
        Toast.makeText(this, "shadercam needs all permissions to function, please try again.", Toast.LENGTH_LONG).show();
        this.finish();
    }

    private void showMenu() {
        if (isMenuHidden) {
            menu.setVisibility(View.VISIBLE);
        } else {
            menu.setVisibility(View.INVISIBLE);
        }
        isMenuHidden = !isMenuHidden;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_swap_camera:
                mVideoFragment.swapCamera();
                break;
            case R.id.btn_take_picture:
                savePicture = true;
                break;
            case R.id.btn_menu:
                showMenu();
                break;
            case R.id.threshold:
                selectedEffect = 1;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.opacity:
                selectedEffect = 2;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.hue:
                selectedEffect = 3;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.dispX:
                selectedEffect = 4;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.dispY:
                selectedEffect = 5;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.contrast:
                selectedEffect = 6;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.saturation:
                selectedEffect = 7;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.brightness:
                selectedEffect = 8;
                seekBar.setProgress(((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect));
                delayMenuClosing();
                break;
            case R.id.toggle_mirror:
                ((ShaderRenderer) mVideoRenderer).toggleMirror();
                break;
            case R.id.zoom:
                selectedEffect = 9;
                ((ShaderRenderer) mVideoRenderer).fromEffect(selectedEffect);
                delayMenuClosing();
                break;
        }
    }

    private void delayMenuClosing() {
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        menu.setVisibility(View.INVISIBLE);
        isMenuHidden = true;
    }

    @Override
    public boolean onTakePicturePressed() {
        return savePicture;
    }

    public void setSavePicture(boolean savePicture) {
        this.savePicture = savePicture;
    }

    // SEEKBAR
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ((ShaderRenderer) mVideoRenderer).setEffectAmount(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}