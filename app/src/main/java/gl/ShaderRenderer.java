package gl;

import android.content.Context;
import android.opengl.GLES20;


import com.androidexperiments.shadercam.gl.VideoRenderer;

public class ShaderRenderer extends VideoRenderer {

    private float offsetR = 0.5f;
    private float offsetG = 0.5f;
    private float offsetB = 0.5f;

    public ShaderRenderer(Context context) {
        super(context, "touchcolor.frag", "touchcolor.vert");
        //setSurfaceTexture();

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
}
