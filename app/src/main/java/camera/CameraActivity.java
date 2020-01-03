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
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.SeekBar;
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

import renderer.ShaderRenderer;

public class CameraActivity extends FragmentActivity implements PermissionsHelper.PermissionsListener, ShaderRenderer.OnButtonPressedListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";
    private static final String TEST_VIDEO_FILE_NAME = "test_video.mp4";
    protected VideoRenderer mVideoRenderer;
    private boolean savePicture = false;
    private RecordableSurfaceView mRecordableSurfaceView;
    private FloatingActionButton takePictureBtn;
    private FloatingActionButton showMenuButton;
    private ImageView swapButton;
    private SeekBar seekBar;
    private PopupMenu popup;
    private VideoFragment mVideoFragment;
    private PermissionsHelper mPermissionsHelper;
    private boolean mPermissionsSatisfied;
    private File mOutputFile;
    private int paramSelector;

    public int getParamSelector() {
        return paramSelector;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        mVideoRenderer = new ShaderRenderer(this, this);
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
        swapButton.setOnClickListener(this);
        takePictureBtn.setOnClickListener(this);
        showMenuButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        createPopupMenu();
    }

    private void createPopupMenu() {
        popup = new PopupMenu(this, showMenuButton);
        popup.getMenuInflater().inflate(R.menu.activity_main_drawer, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.threshold:
                    paramSelector = 1;
                    break;
                case R.id.opacity:
                    paramSelector = 2;
                    break;
                case R.id.hue:
                    paramSelector = 3;
                    break;
                case R.id.dispX:
                    paramSelector = 4;
                    break;
                case R.id.dispY:
                    paramSelector = 5;
                    break;
            }
            seekBar.setProgress(((ShaderRenderer) mVideoRenderer).getEffectAmount(paramSelector));
            return true;
        });
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_swap_camera) {
            mVideoFragment.swapCamera();
        }
        if (v.getId() == R.id.btn_take_picture) {
            savePicture = true;
        }
        if (v.getId() == R.id.btn_menu) {
            Log.i("popup", "gravity" + popup.getGravity());
            popup.show();
        }
    }

    @Override
    public boolean onButtonPressed() {
        return savePicture;
    }

    public void setSavePicture(boolean savePicture) {
        this.savePicture = savePicture;
    }

    // SEEKBAR
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ((ShaderRenderer) mVideoFragment.getVideoRenderer()).getSeekbarProgressValue(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}