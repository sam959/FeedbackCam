//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.androidexperiments.shadercam.gl.VideoRenderer.OnRendererReadyListener;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView.RendererCallbacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import renderer.FlexibleVideoRenderer;

public class SpecialVideoFragment extends Fragment implements OnRendererReadyListener {
    private static final String TAG = "SpecialVideoFragment";
    private static SpecialVideoFragment __instance;
    private RecordableSurfaceView mRecordableSurfaceView;
    private FlexibleVideoRenderer mVideoRenderer;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private int mPreviewTexture;
    private Size mPreviewSize;
    private Size mVideoSize;
    private Builder mPreviewBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private SurfaceTexture mSurfaceTexture;
    public static final int CAMERA_PRIMARY = 0;
    public static final int CAMERA_FORWARD = 1;
    protected int mCameraToUse = 0;
    private SpecialVideoFragment.OnViewportSizeUpdatedListener mOnViewportSizeUpdatedListener;
    private float mVideoSizeAspectRatio;
    private float mPreviewSurfaceAspectRatio;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mCameraIsOpen = false;
    private StateCallback mCaptureSessionStateCallback = new StateCallback() {
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            SpecialVideoFragment.this.mPreviewSession = cameraCaptureSession;
            Log.e("SpecialVideoFragment", "CaptureSession Configured: " + cameraCaptureSession);
            SpecialVideoFragment.this.updatePreview();
        }

        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Activity activity = SpecialVideoFragment.this.getActivity();
            Log.e("SpecialVideoFragment", "config failed: " + cameraCaptureSession);
            if (null != activity) {
                Toast.makeText(activity, "CaptureSession Config Failed", Toast.LENGTH_SHORT).show();
            }

        }

        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.e("SpecialVideoFragment", "onClosed: " + session);
        }
    };
    private android.hardware.camera2.CameraDevice.StateCallback mStateCallback = new android.hardware.camera2.CameraDevice.StateCallback() {
        public void onOpened(CameraDevice cameraDevice) {
            SpecialVideoFragment.this.mCameraOpenCloseLock.release();
            SpecialVideoFragment.this.mCameraDevice = cameraDevice;
            SpecialVideoFragment.this.mCameraIsOpen = true;
            SpecialVideoFragment.this.startPreview();
        }

        public void onDisconnected(CameraDevice cameraDevice) {
            SpecialVideoFragment.this.mCameraOpenCloseLock.release();
            cameraDevice.close();
            SpecialVideoFragment.this.mCameraDevice = null;
            SpecialVideoFragment.this.mCameraIsOpen = false;
            Log.e("SpecialVideoFragment", "DISCONNECTED FROM CAMERA");
        }

        public void onError(CameraDevice cameraDevice, int error) {
            SpecialVideoFragment.this.mCameraOpenCloseLock.release();
            cameraDevice.close();
            SpecialVideoFragment.this.mCameraDevice = null;
            SpecialVideoFragment.this.mCameraIsOpen = false;
            Log.e("SpecialVideoFragment", "CameraDevice.StateCallback onError() " + error);
            Activity activity = SpecialVideoFragment.this.getActivity();
            if (null != activity) {
                activity.finish();
            }

        }
    };
    private List<Surface> mSurfaces;
    private CaptureCallback mCaptureCallback = new CaptureCallback() {
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    public SpecialVideoFragment() {
    }

    public static SpecialVideoFragment getInstance() {
        if (__instance == null) {
            __instance = new SpecialVideoFragment();
            __instance.setRetainInstance(true);
        }

        return __instance;
    }

    public void onResume() {
        super.onResume();
        this.startBackgroundThread();
    }

    public void onPause() {
        super.onPause();
        this.closeCamera();
        this.stopBackgroundThread();
        this.mRecordableSurfaceView.setRendererCallbacks((RendererCallbacks) null);
    }

    public void setVideoRenderer(FlexibleVideoRenderer videoRenderer) {
        this.mVideoRenderer = videoRenderer;
        if (this.mVideoRenderer != null) {
            this.mVideoRenderer.setVideoFragment(getInstance());
            this.mVideoRenderer.setOnRendererReadyListener(this);
            this.mRecordableSurfaceView.setRendererCallbacks(this.mVideoRenderer);
            this.mVideoRenderer.onSurfaceChanged(this.mRecordableSurfaceView.getWidth(), this.mRecordableSurfaceView.getHeight());
        }
    }

    private void startBackgroundThread() {
        this.mBackgroundThread = new HandlerThread("CameraBackground");
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        Log.e("SpecialVideoFragment", "RELEASE TEXTURE");
        if (this.mSurfaceTexture != null) {
            this.mSurfaceTexture.release();
            this.mSurfaceTexture = null;
            this.mSurfaces.clear();
        }

    }

    public void swapCamera() {
        this.closeCamera();
        if (this.mCameraToUse == 1) {
            this.mCameraToUse = 0;
        } else {
            this.mCameraToUse = 1;
        }

        this.openCamera();
    }

    public void openCamera() {
        Activity activity = this.getActivity();
        if (null != activity && !activity.isFinishing()) {
            if (this.mCameraDevice == null || !this.mCameraIsOpen) {
                CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

                try {
                    if (!this.mCameraOpenCloseLock.tryAcquire(2500L, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }

                    String[] cameraList = manager.getCameraIdList();
                    if (this.mCameraToUse >= cameraList.length) {
                        this.mCameraToUse = 0;
                    }

                    String cameraId = cameraList[this.mCameraToUse];
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    this.mVideoSize = this.getOptimalPreviewSize(streamConfigurationMap.getOutputSizes(MediaRecorder.class), this.mRecordableSurfaceView.getWidth(), this.mRecordableSurfaceView.getHeight());
                    this.mPreviewSize = this.getOptimalPreviewSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), this.mRecordableSurfaceView.getWidth(), this.mRecordableSurfaceView.getHeight());
                    this.updateViewportSize(this.mVideoSizeAspectRatio, this.mPreviewSurfaceAspectRatio);
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    Activity#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
                        return;
                    }
                    manager.openCamera(cameraId, this.mStateCallback, (Handler) null);
                } catch (CameraAccessException var7) {
                    Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
                    activity.finish();
                } catch (NullPointerException var8) {
                    var8.printStackTrace();
                    (new com.androidexperiments.shadercam.fragments.CameraFragment.ErrorDialog()).show(this.getFragmentManager(), "dialog");
                } catch (InterruptedException var9) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.");
                }

            }
        }
    }

    private int checkSelfPermission(String camera) {
        //TODO
        return PackageManager.PERMISSION_GRANTED;
    }

    public void updateViewportSize(float videoAspect, float surfaceAspect) {
        int sw = this.mRecordableSurfaceView.getWidth();
        int sh = this.mRecordableSurfaceView.getHeight();
        int vpW;
        int vpH;
        if (videoAspect == surfaceAspect) {
            vpW = sw;
            vpH = sh;
        } else {
            float ratio;
            if (videoAspect < surfaceAspect) {
                ratio = (float)sw / (float)this.mVideoSize.getHeight();
                vpW = (int)((float)this.mVideoSize.getHeight() * ratio);
                vpH = (int)((float)this.mVideoSize.getWidth() * ratio);
            } else {
                ratio = (float)sw / (float)this.mVideoSize.getWidth();
                vpW = (int)((float)this.mVideoSize.getWidth() * ratio);
                vpH = (int)((float)this.mVideoSize.getHeight() * ratio);
            }
        }

        if (this.mOnViewportSizeUpdatedListener != null) {
            this.mOnViewportSizeUpdatedListener.onViewportSizeUpdated(vpW, vpH);
        }

    }

    public void closeCamera() {
        try {
            this.mCameraOpenCloseLock.acquire();
            if (null != this.mCameraDevice) {
                this.mPreviewSession.stopRepeating();
                this.mCameraDevice.close();
                this.mCameraDevice = null;
                this.mCameraIsOpen = false;
            }
        } catch (InterruptedException var6) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } catch (CameraAccessException var7) {
            Log.e("SpecialVideoFragment", "Failed to stop repeaing preview request", var7);
        } finally {
            this.mCameraOpenCloseLock.release();
        }

    }

    private void startPreview() {
        if (null != this.mCameraDevice && null != this.mPreviewSize) {
            try {
                this.mPreviewBuilder = this.mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                if (this.mSurfaces == null) {
                    this.mSurfaces = new ArrayList();
                }

                if (this.mPreviewTexture == -1) {
                    this.mPreviewTexture = this.mVideoRenderer.getCameraTexture();
                }

                assert this.mPreviewTexture != -1;

                this.mSurfaceTexture = new SurfaceTexture(this.mPreviewTexture);
                this.mVideoRenderer.setSurfaceTexture(this.mSurfaceTexture);
                if (this.mSurfaces.size() != 0) {
                    Iterator var1 = this.mSurfaces.iterator();

                    while(var1.hasNext()) {
                        Surface sf = (Surface)var1.next();
                        sf.release();
                    }

                    this.mSurfaces.clear();
                }

                Surface previewSurface = new Surface(this.mSurfaceTexture);
                this.mSurfaces.add(previewSurface);
                this.mPreviewBuilder.addTarget(previewSurface);
                this.mSurfaceTexture.setDefaultBufferSize(this.mRecordableSurfaceView.getHeight(), this.mRecordableSurfaceView.getWidth());
                this.mCameraDevice.createCaptureSession(this.mSurfaces, this.mCaptureSessionStateCallback, this.mBackgroundHandler);
            } catch (CameraAccessException var3) {
                var3.printStackTrace();
            }

        }
    }

    private void updatePreview() {
        if (null != this.mCameraDevice) {
            try {
                this.mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, 1);
                this.mPreviewSession.setRepeatingRequest(this.mPreviewBuilder.build(), this.mCaptureCallback, this.mBackgroundHandler);
                this.mSurfaceTexture.setOnFrameAvailableListener(this.mVideoRenderer);
            } catch (CameraAccessException var2) {
                var2.printStackTrace();
            }

        }
    }

    private Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        double ASPECT_TOLERANCE = 0.1D;
        double targetRatio = (double)h / (double)w;
        if (sizes == null) {
            return null;
        } else {
            Size optimalSize = null;
            double minDiff = 1.7976931348623157E308D;
            int targetHeight = h;
            Size[] var12 = sizes;
            int var13 = sizes.length;

            int var14;
            Size size;
            for(var14 = 0; var14 < var13; ++var14) {
                size = var12[var14];
                double ratio = (double)size.getWidth() / (double)size.getHeight();
                if (Math.abs(ratio - targetRatio) <= 0.1D && (double)Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = (double)Math.abs(size.getHeight() - targetHeight);
                }
            }

            if (optimalSize == null) {
                minDiff = 1.7976931348623157E308D;
                var12 = sizes;
                var13 = sizes.length;

                for(var14 = 0; var14 < var13; ++var14) {
                    size = var12[var14];
                    if ((double)Math.abs(size.getHeight() - targetHeight) < minDiff) {
                        optimalSize = size;
                        minDiff = (double)Math.abs(size.getHeight() - targetHeight);
                    }
                }
            }

            return optimalSize;
        }
    }

    public void setRecordableSurfaceView(RecordableSurfaceView rsv) {
        this.mRecordableSurfaceView = rsv;
    }

    public Size getVideoSize() {
        return this.mVideoSize;
    }

    public int getCurrentCameraType() {
        return this.mCameraToUse;
    }

    public void setCameraToUse(int camera_id) {
        this.mCameraToUse = camera_id;
    }

    public void setPreviewTexture(int previewSurface) {
        this.mPreviewTexture = previewSurface;
    }

    public void setOnViewportSizeUpdatedListener(SpecialVideoFragment.OnViewportSizeUpdatedListener listener) {
        this.mOnViewportSizeUpdatedListener = listener;
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.mSurfaceTexture = surfaceTexture;
    }

    public void onRendererReady() {
        this.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                SpecialVideoFragment.this.openCamera();
            }
        });
    }

    public void onRendererFinished() {
    }

    public FlexibleVideoRenderer getVideoRenderer() {
        return this.mVideoRenderer;
    }

    public static class ErrorDialog extends DialogFragment {
        public ErrorDialog() {
        }

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = this.getActivity();
            return (new android.app.AlertDialog.Builder(activity)).setMessage("This device doesn't support Camera2 API.").setPositiveButton(17039370, new OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity.finish();
                }
            }).create();
        }
    }

    public interface OnViewportSizeUpdatedListener {
        void onViewportSizeUpdated(int var1, int var2);
    }
}
