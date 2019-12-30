package gl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.androidexperiments.shadercam.gl.VideoRenderer;

/**
 * Example renderer that changes colors and tones of camera feed
 * based on touch position.
 */
public class ExampleVideoRenderer extends VideoRenderer {
    private float offsetR = 0.5f;
    private float offsetG = 0.5f;
    private float offsetB = 0.5f;

    /**
     * By not modifying anything, our default shaders will be used in the assets folder of shadercam.
     * <p>
     * Base all shaders off those, since there are some default uniforms/textures that will
     * be passed every time for the camera coordinates and texture coordinates
     */
    public ExampleVideoRenderer(Context context) {
        super(context, "touchcolor.frag.glsl", "touchcolor.vert.glsl");
        Log.i("GlUtil", "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i("GlUtil", "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i("GlUtil", "version : " + GLES20.glGetString(GLES20.GL_VERSION));
        //other setup if need be done here;
    }

    /**
     * we override {@link #setUniformsAndAttribs()} and make sure to call the super so we can add
     * our own uniforms to our shaders here. CameraRenderer handles the rest for us automatically
     */

    @Override
    protected void setUniformsAndAttribs() {
        super.setUniformsAndAttribs();

        int offsetRLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetR");
        int offsetGLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetG");
        int offsetBLoc = GLES20.glGetUniformLocation(mCameraShaderProgram, "offsetB");

       // GLES20.glColorMask(true,false,true,true );
        GLES20.glUniform1f(offsetRLoc, offsetR);
        GLES20.glUniform1f(offsetGLoc, offsetG);
        GLES20.glUniform1f(offsetBLoc, offsetB);
    }


    /**
     * take touch points on that textureview and turn them into multipliers for the color channels
     * of our shader, simple, yet effective way to illustrate how easy it is to integrate app
     * interaction into our glsl shaders
     *
     * @param rawX raw x on screen
     * @param rawY raw y on screen
     */
    public void setTouchPoint(float rawX, float rawY) {
        offsetR = rawX / mSurfaceWidth;
        offsetG = rawY / mSurfaceHeight;
        offsetB = offsetR / offsetG;
    }
}