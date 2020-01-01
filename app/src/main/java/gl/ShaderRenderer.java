package gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import com.androidexperiments.shadercam.gl.VideoRenderer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import camera.CameraActivity;

public class ShaderRenderer extends VideoRenderer {

    private static final File FILES_DIR = Environment.getExternalStorageDirectory();
    private ShaderRenderer.OnButtonPressedListener listener;
    private ByteBuffer mPixelBuf;
    private Bitmap bufferBitmap;
    private Bitmap tempBitmap;

    private int mBufferTexId = -1;
    private boolean firstFrame = true;
    private int frameCount = 0;
    private float threshold = 0.5f;
    private float opacity = 0.5f;
    private float offsetB = 0.5f;
    private float effectAmount = 0;
    private Context context;
    private boolean updateIsNeeded;
    private Matrix matrix = new Matrix();

    public ShaderRenderer(Context context, ShaderRenderer.OnButtonPressedListener listener) {
        super(context, "lumakey.frag.glsl", "lumakey.vert.glsl");
        this.context = context;
        this.listener = listener;
        matrix.postScale(1, -1, this.mSurfaceWidth / 2, this.mSurfaceHeight / 2);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
        mPixelBuf = ByteBuffer.allocateDirect(width * height * 4);
        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    protected void setUniformsAndAttribs() {
        super.setUniformsAndAttribs();

        int thresholdLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "threshold");
        int opacityGLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "opacity");
        GLES20.glUniform1f(thresholdLoc, effectAmount);
        GLES20.glUniform1f(opacityGLoc, opacity);
    }

    public void getSeekbarProgressValue(int seekbarValue){
         effectAmount = ((float)seekbarValue) / 100;
         Log.i("seekbar", "effectamout" + effectAmount);
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        onFirstFrame();
        if (listener.onButtonPressed()) {
            ((CameraActivity) context).setSavePicture(false);
            try {
                File outputFile = new File(FILES_DIR,
                        String.format("frame-%02d.png", frameCount));
                saveFrame(outputFile.toString());
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        mPixelBuf.rewind();
        GLES20.glReadPixels(0, 0, this.mSurfaceWidth, this.mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                mPixelBuf);
        tempBitmap = Bitmap.createBitmap(this.mSurfaceWidth, this.mSurfaceHeight, Bitmap.Config.ARGB_8888);
        tempBitmap.copyPixelsFromBuffer(mPixelBuf);
        updateBufferTexture(tempBitmap);
    }

    private void updateBufferTexture(Bitmap tempBitmap){
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

    private void onFirstFrame(){
        if(firstFrame == true){
            bufferBitmap = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
            if(mBufferTexId == -1) {
                mBufferTexId = addTexture(bufferBitmap, "bufferTexture");
            }
            firstFrame = false;
        }
    }

    private void saveFrame(String filename) throws IOException {

        mPixelBuf.rewind();
        GLES20.glReadPixels(0, 0, this.mSurfaceWidth, this.mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                mPixelBuf);

        BufferedOutputStream bufferedOutputStream = null;

        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filename));
            Bitmap bitmap = Bitmap.createBitmap(this.mSurfaceWidth, this.mSurfaceHeight, Bitmap.Config.ARGB_8888);
            //mPixelBuf.rewind();
            bitmap.copyPixelsFromBuffer(mPixelBuf);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, this.mSurfaceWidth, this.mSurfaceHeight, matrix, false);
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 90, bufferedOutputStream);
            bitmap.recycle();
            rotatedBitmap.recycle();
        } finally {
            if (bufferedOutputStream != null) bufferedOutputStream.close();
        }
        frameCount++;
        Log.v("Picture ", "Saved " + this.mSurfaceWidth + "x" + this.mSurfaceHeight + " frame as '" + filename + "'" + "DIRECTORY IS: " + FILES_DIR);
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
