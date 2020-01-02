package renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import com.androidexperiments.shadercam.gl.VideoRenderer;
import com.example.feedbackcam.R;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import camera.CameraActivity;
import worker.SaveImageRunnable;

public class ShaderRenderer extends VideoRenderer {

    private static final File FILES_DIR = Environment.getExternalStorageDirectory();
    private Context context;
    private ShaderRenderer.OnButtonPressedListener listener;
    private Bitmap bufferBitmap;

    private int mBufferTexId = -1;
    private int frameCount = 0;
    private int paramSelector;

    private float threshold = 0.5f;
    private float opacity = 0.5f;
    private float hue = 0.5f;
    private float effectAmount = 0;
    private boolean updateIsNeeded;
    private boolean firstFrame = true;

    public ShaderRenderer(Context context, ShaderRenderer.OnButtonPressedListener listener) {
        super(context, "lumakey.frag.glsl", "lumakey.vert.glsl");
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
    }

    @Override
    protected void setUniformsAndAttribs() {
        super.setUniformsAndAttribs();
        int paramLoc;
        paramSelector = ((CameraActivity) context).getParamSelector();
        switch (paramSelector) {
            case 1:
                threshold = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "threshold");
                GLES20.glUniform1f(paramLoc, threshold);
                break;
            case 2:
                opacity = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "opacity");
                GLES20.glUniform1f(paramLoc, opacity);
                break;
            case 3:
                hue = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "hue");
                GLES20.glUniform1f(paramLoc, hue);
                break;
            default:
                threshold = effectAmount;
                int loc = GLES20.glGetUniformLocation(mCameraShaderProgram, "threshold");
                GLES20.glUniform1f(loc, threshold);
        }
    }

    public void getSeekbarProgressValue(int seekbarValue) {
        effectAmount = ((float) seekbarValue) / 100;
    }

    public int getEffectAmount(int effectNumber) {
        int seekbarAmount = 0;
        switch (effectNumber) {
            case 1:
                seekbarAmount = (int) (threshold * 100);
                break;
            case 2:
                seekbarAmount = (int) (opacity * 100);
                break;
            case 3:
                seekbarAmount = (int) (hue * 100);
                break;
        }
        return seekbarAmount;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        onFirstFrame();
        if (listener.onButtonPressed()) {
            ((CameraActivity) context).setSavePicture(false);
            saveImage();
        }
        ByteBuffer onDrawPixelBuffer = ByteBuffer.allocateDirect(mSurfaceWidth * mSurfaceHeight * 4);
        onDrawPixelBuffer.rewind();
        GLES20.glReadPixels(0, 0, this.mSurfaceWidth, this.mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                onDrawPixelBuffer);

        Bitmap tempBitmap = Bitmap.createBitmap(this.mSurfaceWidth, this.mSurfaceHeight, Bitmap.Config.ARGB_8888);
        tempBitmap.copyPixelsFromBuffer(onDrawPixelBuffer);
        updateBufferTexture(tempBitmap);
    }

    private void saveImage() {
        File outputFile = new File(FILES_DIR, String.format(context.getString(R.string.image_title), frameCount));
        ByteBuffer bufferToSave = ByteBuffer.allocateDirect(mSurfaceWidth * mSurfaceHeight * 4);
        bufferToSave.order(ByteOrder.LITTLE_ENDIAN);
        bufferToSave.rewind();
        GLES20.glReadPixels(0, 0, mSurfaceWidth, mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                bufferToSave);
        frameCount++;
        new Thread(new SaveImageRunnable(bufferToSave, outputFile.toString(), mSurfaceWidth, mSurfaceHeight))
                .start();
        Log.v("Picture ", "Saved " + this.mSurfaceWidth + "x" + this.mSurfaceHeight + " frame as '" + outputFile.toString() + "'" + "DIRECTORY IS: " + FILES_DIR);
    }

    private void updateBufferTexture(Bitmap tempBitmap) {
        bufferBitmap = tempBitmap;
        updateIsNeeded = true;
    }

    @Override
    protected void setExtraTextures() {
        if (updateIsNeeded) {
            try {
                updateTexture(mBufferTexId, bufferBitmap);
            } catch (IllegalArgumentException e) {
                //first run and awful way to hope this fails
                Log.e("FROM setExtraTextures()", "ILLEGAL", e);
            }
            updateIsNeeded = false;
        }
        super.setExtraTextures();
    }

    private void onFirstFrame() {
        if (firstFrame == true) {
            bufferBitmap = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
            if (mBufferTexId == -1) {
                mBufferTexId = addTexture(bufferBitmap, "bufferTexture");
            }
            firstFrame = false;
        }
    }

    @Override
    protected void deinitGLComponents() {
        mBufferTexId = -1;
        super.deinitGLComponents();
    }

    public interface OnButtonPressedListener {
        boolean onButtonPressed();
    }

}
