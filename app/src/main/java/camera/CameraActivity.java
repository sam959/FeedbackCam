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
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.androidexperiments.shadercam.fragments.PermissionsHelper;
import com.androidexperiments.shadercam.fragments.VideoFragment;
import com.androidexperiments.shadercam.gl.VideoRenderer;
import com.androidexperiments.shadercam.utils.ShaderUtils;
import com.example.feedbackcam.R;
import com.google.android.material.button.MaterialButton;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import gl.ShaderRenderer;

public class CameraActivity extends FragmentActivity implements PermissionsHelper.PermissionsListener, ShaderRenderer.OnButtonPressedListener, View.OnClickListener {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";
    private static final String TEST_VIDEO_FILE_NAME = "test_video.mp4";
    protected VideoRenderer mVideoRenderer;
    RecordableSurfaceView mRecordableSurfaceView;
    MaterialButton mRecordBtn;
    ImageView swapButton;
    boolean savePicture = false;
    private VideoFragment mVideoFragment;
    private PermissionsHelper mPermissionsHelper;
    private boolean mPermissionsSatisfied;
    private File mOutputFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        mVideoRenderer = new ShaderRenderer(this, this::onButtonPressed);
        mRecordableSurfaceView = new RecordableSurfaceView(this);
        mRecordableSurfaceView = findViewById(R.id.texture_view);
        mRecordBtn = findViewById(R.id.btn_record);
        swapButton = findViewById(R.id.btn_swap_camera);
        swapButton.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        if (PermissionsHelper.isMorHigher())
            setupPermissions();
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
        //pass in a reference to the RecordableSurfaceView - this is important
        //mRecordableSurfaceView.setRenderMode(RecordableSurfaceView.RENDERMODE_CONTINUOUSLY);
        mVideoFragment.setRecordableSurfaceView(mRecordableSurfaceView);
        //Connect your renderer
        mVideoFragment.setVideoRenderer(renderer);
        mVideoFragment.setCameraToUse(VideoFragment.CAMERA_PRIMARY);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mVideoFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupInteraction() {
        mRecordableSurfaceView.setOnTouchListener((v, event) -> {
            if (mVideoFragment.getVideoRenderer() instanceof ShaderRenderer) {
                ((ShaderRenderer) mVideoFragment.getVideoRenderer())
                        .setTouchPoint(event.getRawX(), event.getRawY());
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        setupInteraction();

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

    @Override
    protected void onPause() {
        super.onPause();
        shutdownCamera();
        mRecordableSurfaceView.pause();
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
        if (v.getId() == R.id.btn_record) {
            savePicture = true;
        }
    }

    @Override
    public boolean onButtonPressed() {
        return savePicture;
    }

    public void setSavePicture(boolean savePicture) {
        this.savePicture = savePicture;
    }
}