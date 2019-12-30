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
    private int frameCount = 0;
    private float offsetR = 0.5f;
    private float offsetG = 0.5f;
    private float offsetB = 0.5f;
    private Context context;

    public ShaderRenderer(Context context, ShaderRenderer.OnButtonPressedListener listener) {
        super(context, "touchcolor.frag.glsl", "touchcolor.vert.glsl");
        this.context = context;
        this.listener = listener;
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

        int offsetRLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetR");
        int offsetGLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetG");
        int offsetBLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetB");
        GLES20.glUniform1f(offsetRLoc, offsetR);
        GLES20.glUniform1f(offsetGLoc, offsetG);
        GLES20.glUniform1f(offsetBLoc, offsetB);
    }

    public void setTouchPoint(float rawX, float rawY) {
        offsetR = rawX / mSurfaceWidth;
        offsetG = rawY / mSurfaceHeight;
        offsetB = offsetR / offsetG;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
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
    }

    private void saveFrame(String filename) throws IOException {
        mPixelBuf.rewind();
        GLES20.glReadPixels(0, 0, this.mSurfaceWidth, this.mSurfaceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                mPixelBuf);

        BufferedOutputStream bufferedOutputStream = null;
        Matrix matrix = new Matrix();
        matrix.postScale(1, -1, this.mSurfaceWidth / 2, this.mSurfaceHeight / 2);

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

    public interface OnButtonPressedListener {
        boolean onButtonPressed();
    }
}
