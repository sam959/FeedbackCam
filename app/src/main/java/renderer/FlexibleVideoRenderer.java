package renderer;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.SparseIntArray;
import com.androidexperiments.shadercam.fragments.VideoFragment;
import com.androidexperiments.shadercam.utils.ShaderUtils;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView.RendererCallbacks;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import fragment.SpecialVideoFragment;

import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;

public class FlexibleVideoRenderer implements RendererCallbacks, OnFrameAvailableListener {
    private static final String TAG = FlexibleVideoRenderer.class.getSimpleName();
    int mNeedsRefreshCount = 0;
    private String DEFAULT_FRAGMENT_SHADER = "vid.frag.glsl";
    private String DEFAULT_VERTEX_SHADER = "vid.vert.glsl";
    private WeakReference<Context> mContextWeakReference;
    protected int mSurfaceWidth;
    protected int mSurfaceHeight;
    protected SurfaceTexture mSurfaceTexture;
    private String vertexShaderCode;
    private String fragmentShaderCode;
    private static float squareSize = 1.0F;
    private static float[] squareCoords;
    private static short[] drawOrder;
    private FloatBuffer textureBuffer;
    private float[] textureCoords = new float[]{0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F};
    protected int mCameraShaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private int textureCoordinateHandle;
    private int positionHandle;
    private static final int MAX_TEXTURES = 16;
    private int[] mTexturesIds = new int[16];
    private int[] mTextureConsts = new int[]{33985, 33986, 33987, 33988, 33989, 33990, 33991, 33992, 33993, 33994, 33995, 33996, 33997, 33998, 33999, 34000};
    private ArrayList<FlexibleVideoRenderer.Texture> mTextureArray;
    private float[] mCameraTransformMatrix = new float[16];
    private FlexibleVideoRenderer.OnRendererReadyListener mOnRendererReadyListener;
    private int mViewportWidth;
    private int mViewportHeight;
    private SpecialVideoFragment mVideoFragment;
    private static final SparseIntArray ORIENTATIONS;
    private String mFragmentShaderPath;
    private String mVertexShaderPath;
    private boolean mFrameAvailableRegistered = false;

    public FlexibleVideoRenderer(Context context) {
        this.init(context, this.DEFAULT_FRAGMENT_SHADER, this.DEFAULT_VERTEX_SHADER);
    }

    public FlexibleVideoRenderer(Context context, String fragPath, String vertPath) {
        this.init(context, fragPath, vertPath);
    }

    private void init(Context context, String fragPath, String vertPath) {
        this.mContextWeakReference = new WeakReference(context);
        this.mFragmentShaderPath = fragPath;
        this.mVertexShaderPath = vertPath;
        this.loadFromShadersFromAssets(this.mFragmentShaderPath, this.mVertexShaderPath);
    }

    private void loadFromShadersFromAssets(String pathToFragment, String pathToVertex) {
        try {
            this.fragmentShaderCode = ShaderUtils.getStringFromFileInAssets((Context)this.mContextWeakReference.get(), pathToFragment);
            this.vertexShaderCode = ShaderUtils.getStringFromFileInAssets((Context)this.mContextWeakReference.get(), pathToVertex);
        } catch (IOException var4) {
            Log.e(TAG, "loadFromShadersFromAssets() failed. Check paths to assets.\n" + var4.getMessage());
        }

    }

    protected void initGLComponents() {
        this.onPreSetupGLComponents();
        this.setupVertexBuffer();
        this.setupTextures();
        this.setupCameraTexture();
        this.setupShaders();
        this.onSetupComplete();
    }

    public void deinitGL() {
        this.deinitGLComponents();
    }

    protected void deinitGLComponents() {
        GLES20.glDeleteTextures(16, this.mTexturesIds, 0);
        GLES20.glDeleteProgram(this.mCameraShaderProgram);
    }

    private void onPreSetupGLComponents() {
    }

    protected void setupVertexBuffer() {
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        this.drawListBuffer = dlb.asShortBuffer();
        this.drawListBuffer.put(drawOrder);
        this.drawListBuffer.position(0);
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        this.vertexBuffer = bb.asFloatBuffer();
        this.vertexBuffer.put(squareCoords);
        this.vertexBuffer.position(0);
    }

    protected void setupTextures() {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(this.textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());
        this.textureBuffer = texturebb.asFloatBuffer();
        this.textureBuffer.put(this.textureCoords);
        this.textureBuffer.position(0);
        GLES20.glGenTextures(16, this.mTexturesIds, 0);
        this.checkGlError("Texture generate");
    }

    protected void setupCameraTexture() {
        Log.e(TAG, "SETUP CAMERA TEXTURE " + this.mTexturesIds[0]);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTexturesIds[0]);
        this.checkGlError("Texture bind");
    }

    public int getCameraTexture() {
        return this.mTexturesIds != null && this.mTexturesIds.length > 0 ? this.mTexturesIds[0] : -1;
    }

    protected void setupShaders() {
        int vertexShaderHandle = GLES20.glCreateShader(35633);
        GLES20.glShaderSource(vertexShaderHandle, this.vertexShaderCode);
        GLES20.glCompileShader(vertexShaderHandle);
        this.checkGlError("Vertex shader compile");
        Log.d(TAG, "vertexShader info log:\n " + GLES20.glGetShaderInfoLog(vertexShaderHandle));
        int fragmentShaderHandle = GLES20.glCreateShader(35632);
        GLES20.glShaderSource(fragmentShaderHandle, this.fragmentShaderCode);
        GLES20.glCompileShader(fragmentShaderHandle);
        this.checkGlError("Pixel shader compile");
        Log.d(TAG, "fragmentShader info log:\n " + GLES20.glGetShaderInfoLog(fragmentShaderHandle));
        this.mCameraShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(this.mCameraShaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(this.mCameraShaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(this.mCameraShaderProgram);
        this.checkGlError("Shader program compile");
        int[] status = new int[1];
        GLES20.glGetProgramiv(this.mCameraShaderProgram, 35714, status, 0);
        if (status[0] != 1) {
            String error = GLES20.glGetProgramInfoLog(this.mCameraShaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

    }

    protected void onSetupComplete() {
        if (this.mOnRendererReadyListener != null) {
            this.mOnRendererReadyListener.onRendererReady();
        }

    }

    public void shutdown() {
        this.mOnRendererReadyListener.onRendererFinished();
    }

    protected void setUniformsAndAttribs() {
        int textureParamHandle = GLES20.glGetUniformLocation(this.mCameraShaderProgram, "camTexture");
        int textureTranformHandle = GLES20.glGetUniformLocation(this.mCameraShaderProgram, "camTextureTransform");
        this.textureCoordinateHandle = GLES20.glGetAttribLocation(this.mCameraShaderProgram, "camTexCoordinate");
        this.positionHandle = GLES20.glGetAttribLocation(this.mCameraShaderProgram, "position");
        GLES20.glEnableVertexAttribArray(this.positionHandle);
        GLES20.glVertexAttribPointer(this.positionHandle, 2, 5126, false, 8, this.vertexBuffer);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTexturesIds[0]);
        GLES20.glUniform1i(textureParamHandle, 0);
        GLES20.glEnableVertexAttribArray(this.textureCoordinateHandle);
        GLES20.glVertexAttribPointer(this.textureCoordinateHandle, 2, 5126, false, 8, this.textureBuffer);
        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, this.mCameraTransformMatrix, 0);
    }

    public int addTexture(int resource_id, String uniformName) {
        int texId = this.mTextureConsts[this.mTextureArray.size()];
        if (this.mTextureArray.size() + 1 >= 16) {
            throw new IllegalStateException("Too many textures! Please don't use so many :(");
        } else {
            Bitmap bmp = BitmapFactory.decodeResource((this.mContextWeakReference.get()).getResources(), resource_id);
            return this.addTexture(texId, bmp, uniformName, true);
        }
    }

    public int addTexture(Bitmap bitmap, String uniformName) {
        int texId = this.mTextureConsts[this.mTextureArray.size()];
        if (this.mTextureArray.size() + 1 >= 16) {
            throw new IllegalStateException("Too many textures! Please don't use so many :(");
        } else {
            return this.addTexture(texId, bitmap, uniformName, true);
        }
    }

    public int addTexture(int texId, Bitmap bitmap, String uniformName, boolean recycle) {
        int num = this.mTextureArray.size() + 1;
        GLES20.glActiveTexture(texId);
        this.checkGlError("Texture generate");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.mTexturesIds[num]);
        this.checkGlError("Texture bind");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        if (recycle) {
            bitmap.recycle();
        }

        FlexibleVideoRenderer.Texture tex = new FlexibleVideoRenderer.Texture(num, texId, uniformName);
        if (!this.mTextureArray.contains(tex)) {
            this.mTextureArray.add(tex);
            Log.d(TAG, "addedTexture() " + this.mTexturesIds[num] + " : " + tex);
        }

        return num;
    }

    public void updateTexture(int texNum, Bitmap drawingCache) {
        GLES20.glActiveTexture(this.mTextureConsts[texNum - 1]);
        this.checkGlError("Texture generate");
        GLES20.glBindTexture(3553, this.mTexturesIds[texNum]);
        this.checkGlError("Texture bind");
        GLUtils.texSubImage2D(3553, 0, 0, 0, drawingCache);
        this.checkGlError("Tex Sub Image");
        drawingCache.recycle();
    }

    protected void setExtraTextures() {
        for(int i = 0; i < this.mTextureArray.size(); ++i) {
            FlexibleVideoRenderer.Texture tex = (FlexibleVideoRenderer.Texture)this.mTextureArray.get(i);
            int imageParamHandle = GLES20.glGetUniformLocation(this.mCameraShaderProgram, tex.uniformName);
            GLES20.glActiveTexture(tex.texId);
            GLES20.glBindTexture(3553, this.mTexturesIds[tex.texNum]);
            GLES20.glUniform1i(imageParamHandle, tex.texNum);
        }

    }

    protected void drawElements() {
        GLES20.glDrawElements(4, drawOrder.length, 5123, this.drawListBuffer);
    }

    protected void onDrawCleanup() {
        GLES20.glDisableVertexAttribArray(this.positionHandle);
        GLES20.glDisableVertexAttribArray(this.textureCoordinateHandle);
    }

    public void checkGlError(String op) {
        int error;
        while((error = GLES20.glGetError()) != 0) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }

    }

    public void setOnRendererReadyListener(FlexibleVideoRenderer.OnRendererReadyListener listener) {
        this.mOnRendererReadyListener = listener;
    }

    public void setVideoFragment(SpecialVideoFragment videoFragment) {
        this.mVideoFragment = videoFragment;
        this.mVideoFragment.setPreviewTexture(this.getCameraTexture());
    }

    public void onSurfaceCreated() {
        this.deinitGL();
        this.mTextureArray = new ArrayList();
        this.initGLComponents();
    }

    public void onSurfaceChanged(int width, int height) {
        this.mSurfaceHeight = height;
        this.mSurfaceWidth = width;
        this.mViewportHeight = height;
        this.mViewportWidth = width;
    }

    public void onSurfaceDestroyed() {
        this.deinitGL();
    }

    public void onContextCreated() {
    }

    public void onPreDrawFrame() {
        if (this.mSurfaceTexture != null) {
            if (!this.mFrameAvailableRegistered) {
                this.mSurfaceTexture.setOnFrameAvailableListener(this);
                this.mFrameAvailableRegistered = true;
            }
        } else if (this.mVideoFragment.getSurfaceTexture() != null) {
            this.mSurfaceTexture = this.mVideoFragment.getSurfaceTexture();
        } else {
            this.mVideoFragment.setPreviewTexture(this.getCameraTexture());
        }

    }

    public void onDrawFrame() {
        if (this.mNeedsRefreshCount > 0) {
            for(int i = 0; i < this.mNeedsRefreshCount; ++i) {
                this.mSurfaceTexture.updateTexImage();
                this.mSurfaceTexture.getTransformMatrix(this.mCameraTransformMatrix);
            }
        }

        GLES20.glViewport(0, 0, this.mViewportWidth, this.mViewportHeight);
        //GLES20.glClearColor(0.329412F, 0.329412F, 0.329412F, 0.0F);
        //GLES20.glClear(16384);
        GLES20.glUseProgram(this.mCameraShaderProgram);
        this.setUniformsAndAttribs();
        this.setExtraTextures();
        this.drawElements();
        this.onDrawCleanup();
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.mSurfaceTexture = surfaceTexture;
        this.mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        ++this.mNeedsRefreshCount;
        if (this.mSurfaceTexture == null && this.mVideoFragment.getSurfaceTexture() != null) {
            this.mSurfaceTexture = this.mVideoFragment.getSurfaceTexture();
        }

    }

    static {
        squareCoords = new float[]{-squareSize, squareSize, squareSize, squareSize, -squareSize, -squareSize, squareSize, -squareSize};
        drawOrder = new short[]{0, 1, 2, 1, 3, 2};
        ORIENTATIONS = new SparseIntArray();
        ORIENTATIONS.append(0, 90);
        ORIENTATIONS.append(1, 0);
        ORIENTATIONS.append(2, 270);
        ORIENTATIONS.append(3, 180);
    }

    public interface OnRendererReadyListener {
        void onRendererReady();

        void onRendererFinished();
    }

    private class Texture {
        public int texNum;
        public int texId;
        public String uniformName;

        private Texture(int texNum, int texId, String uniformName) {
            this.texNum = texNum;
            this.texId = texId;
            this.uniformName = uniformName;
        }

        public String toString() {
            return "[Texture] num: " + this.texNum + " id: " + this.texId + ", uniformName: " + this.uniformName;
        }
    }
}
