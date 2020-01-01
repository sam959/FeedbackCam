#extension GL_OES_EGL_image_external : require
//SOFT LUMA KEY
//"CREDIT": "by IMIMOT (ported from http://www.memo.tv/)"

precision mediump float;
uniform samplerExternalOES camTexture;
uniform sampler2D bufferTexture;


varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

uniform float threshold;
uniform float opacity;

void main() {
    vec4 pix = texture2D(camTexture, v_CamTexCoordinate);
    vec4 feedback = texture2D(bufferTexture, vec2(-v_CamTexCoordinate.y, -v_CamTexCoordinate.x)-vec2(0.004, 0.004));
    float newopacity = opacity * 0.35;
    float fValue = (pix.r *0.29+ pix.g*0.6 + pix.b*0.11);
    float l1 = abs(threshold) - 0.07;
    float l2 = l1 + 0.07;
    float bw_sel = step(threshold, 0.0);
    fValue = smoothstep(max(l1,0.0), min(l2, 1.0), abs(bw_sel - fValue));

    pix.a = fValue;

    float isKeyOpen = step(pix.a, 0.1);

    // final mix needed to make alpha working
    vec4 color = mix(vec4(pix.rgb, 0.0),pix,pix.a);
    gl_FragColor = vec4(mix(color.rgb, feedback.rgb, 1.0 - pix.a - (isKeyOpen * newopacity)),  1.0);
}
