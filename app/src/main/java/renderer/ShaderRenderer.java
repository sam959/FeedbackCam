package renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import com.example.feedbackcam.R;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import camera.CameraActivity;
import saveimage.SaveImageRunnable;

public class ShaderRenderer extends FlexibleVideoRenderer {

    private static final File FILES_DIR = Environment.getExternalStorageDirectory();
    private Context context;
    private OnTakePicturePressedListener onButtonPressedListener;
    private Bitmap bufferBitmap;
    private ByteBuffer onDrawPixelBuffer;
    private Bitmap tempBitmap;

    private int mBufferTexId = -1;
    private int photoNumber = 0;
    private int paramSelector;

    private float threshold = 0.5f;
    private float opacity = 0.0f;
    private float hue = 0.5f;
    private float dispX = 0.0f;
    private float dispY = 0.0f;
    private float contrast = 0.5f;
    private float saturation = 0.5f;
    private float brightness = 0.5f;
    private float effectAmount = 0;
    private float zoom = 1.f;
    private float isToggled;

    private boolean updateIsNeeded;
    private boolean firstFrame = true;
    private boolean toggle = true;

    public ShaderRenderer(Context context) {
        super(context, "mirror.frag.glsl", "lumakey.vert.glsl");
        this.context = context;
        this.onButtonPressedListener = (OnTakePicturePressedListener) context;
    }

    @Override
    protected void setUniformsAndAttribs() {
        super.setUniformsAndAttribs();
        if (firstFrame) {
            effectAmount = 0.5f;

        }
        int paramLoc;
        paramSelector = ((CameraActivity) context).getSelectedEffect();
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
            case 4:
                dispX = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "dispX");
                GLES20.glUniform1f(paramLoc, dispX);
                break;
            case 5:
                dispY = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "dispY");
                GLES20.glUniform1f(paramLoc, dispY);
                break;
            case 6:
                contrast = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "contrast");
                GLES20.glUniform1f(paramLoc, contrast);
                break;
            case 7:
                saturation = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "saturation");
                GLES20.glUniform1f(paramLoc, saturation);
                break;
            case 8:
                brightness = effectAmount;
                paramLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "brightness");
                GLES20.glUniform1f(paramLoc, brightness);
                break;
            case 9:
                zoom = effectAmount;
                int zoomLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "zoom");
                GLES20.glUniform1f(zoomLoc, effectAmount);
                break;
            default:
                threshold = effectAmount;
                int defloc = GLES20.glGetUniformLocation(mCameraShaderProgram, "threshold");
                GLES20.glUniform1f(defloc, threshold);
        }
        int mirrorLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "mirrorActive");
        GLES20.glUniform1f(mirrorLoc, isToggled);
        paramSelector = 0;
    }

    public void setEffectAmount(int effectAmount) {
        this.effectAmount = ((float) effectAmount) / 100;
    }

    public void toggleMirror() {
        if (toggle) {
            isToggled = 1.f;
        } else {
            isToggled = 0.f;
        }
        toggle = !toggle;
    }

    public int fromEffectAmount(int effect) {
        int lastSetEffectAmount = 0;
        switch (effect) {
            case 1:
                lastSetEffectAmount = (int) (threshold * 100);
                break;
            case 2:
                lastSetEffectAmount = (int) (opacity * 100);
                break;
            case 3:
                lastSetEffectAmount = (int) (hue * 100);
                break;
            case 4:
                lastSetEffectAmount = (int) (dispX * 100);
                break;
            case 5:
                lastSetEffectAmount = (int) (dispY * 100);
                break;
            case 6:
                lastSetEffectAmount = (int) (contrast * 100);
                break;
            case 7:
                lastSetEffectAmount = (int) (saturation * 100);
                break;
            case 8:
                lastSetEffectAmount = (int) (brightness * 100);
                break;
            case 9:
                lastSetEffectAmount = (int) (zoom * 100);
                break;
        }
        return lastSetEffectAmount;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        onFirstFrame();
        if (onButtonPressedListener.onTakePicturePressed()) {
            ((CameraActivity) context).setSavePicture(false);
            saveImage();
        }
        onDrawPixelBuffer.rewind();
        GLES20.glReadPixels(0, 0, this.mSurfaceWidth, this.mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                onDrawPixelBuffer);

        tempBitmap = Bitmap.createBitmap(this.mSurfaceWidth, this.mSurfaceHeight, Bitmap.Config.ARGB_8888);
        tempBitmap.copyPixelsFromBuffer(onDrawPixelBuffer);
        updateBufferTexture(tempBitmap);


    }

    private void saveImage() {
        File outputFile = new File(FILES_DIR, String.format(context.getString(R.string.image_title), photoNumber));
        ByteBuffer bufferToSave = ByteBuffer.allocateDirect(mSurfaceWidth * mSurfaceHeight * 4);
        bufferToSave.order(ByteOrder.LITTLE_ENDIAN);
        bufferToSave.rewind();
        GLES20.glReadPixels(0, 0, mSurfaceWidth, mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                bufferToSave);
        photoNumber++;
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
        if (firstFrame) {
            bufferBitmap = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
            onDrawPixelBuffer = ByteBuffer.allocateDirect(mSurfaceWidth * mSurfaceHeight * 4);

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

    public interface OnTakePicturePressedListener {
        boolean onTakePicturePressed();
    }

}
