//position
attribute vec4 position;

//camera transform and texture
uniform mat4 camTextureTransform;
uniform mat4 uMVPMatrix;
uniform float zoom;

attribute vec4 camTexCoordinate;

//tex coords
varying vec2 v_CamTexCoordinate;
varying vec2 v_zoomedCoords;

void main()
{
    float scaledZoom = zoom * 0.1 + 0.95;
    //camera texcoord needs to be manipulated by the transform given back from the system
    v_CamTexCoordinate = (camTextureTransform * camTexCoordinate).xy;
    v_zoomedCoords = (position.xy * scaledZoom) * 0.5 + 0.5;
    gl_Position =  position;
}