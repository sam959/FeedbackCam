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





vec3 hueShift(vec3 color, float hueAdjust){
    //implemented by mairod

    vec3  kRGBToYPrime = vec3 (0.299, 0.587, 0.114);
    vec3  kRGBToI      = vec3(0.596, -0.275, -0.321);
    vec3  kRGBToQ      = vec3(0.212, -0.523, 0.311);

    vec3  kYIQToR     = vec3(1.0, 0.956, 0.621);
    vec3  kYIQToG     = vec3(1.0, -0.272, -0.647);
    vec3  kYIQToB     = vec3(1.0, -1.107, 1.704);

    float   YPrime  = dot(color, kRGBToYPrime);
    float   I       = dot(color, kRGBToI);
    float   Q       = dot(color, kRGBToQ);
    float   hue     = atan(Q, I);
    float   chroma  = sqrt(I * I + Q * Q);

    //chroma = fract(pow((chroma),power));

    hue += hueAdjust;

    Q = chroma * sin(hue);
    I = chroma * cos(hue);

    vec3    yIQ   = vec3(YPrime, I, Q);
    return vec3(dot(yIQ, kYIQToR), dot(yIQ, kYIQToG), dot(yIQ, kYIQToB));


}







void main() {
    vec4 pix = texture2D(camTexture, v_CamTexCoordinate);
    vec4 feedback = texture2D(bufferTexture, vec2(-v_CamTexCoordinate.y, -v_CamTexCoordinate.x)-vec2(0.003, 0.003));
    float newopacity = opacity * 0.35;
    float fValue = (pix.r *0.29+ pix.g*0.6 + pix.b*0.11);
    float l1 = abs(threshold) - 0.07;
    float l2 = l1 + 0.07;
    //float threshBW = (threshold * 2.0) - 1.0;
    float bw_sel = step(0.0, threshold);
    fValue = smoothstep(max(l1,0.0), min(l2, 1.0), abs(bw_sel - fValue));
    pix.a = fValue;
    float isKeyOpen = step(pix.a, 0.1);
    // final mix needed to make alpha working
    vec4 color = mix(vec4(pix.rgb, 0.0),pix,pix.a);
    color = vec4(mix(color.rgb, feedback.rgb, 1.0 - pix.a - (isKeyOpen * 0.0)),  1.0);
    color = vec4(hueShift(color.xyz, threshold*3.0),1.0);
    gl_FragColor = color;
}
