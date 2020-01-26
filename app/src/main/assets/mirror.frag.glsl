#extension GL_OES_EGL_image_external : require
//SOFT LUMA KEY
//"CREDIT": "by IMIMOT (ported from http://www.memo.tv/)"
precision highp float;
uniform samplerExternalOES camTexture;
uniform sampler2D bufferTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_zoomedCoords;
varying vec2 v_TexCoordinate;

uniform float mirrorActive;

uniform float threshold;
uniform float opacity;
uniform float hue;
uniform float contrast;
uniform float saturation;
uniform float brightness;
uniform float dispX;
uniform float dispY;

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

vec3 conSatBri(vec3 color, float brt, float sat, float con)
{
    // Increase or decrease theese values to adjust r, g and b color channels seperately
    const float AvgLumR = 0.5;
    const float AvgLumG = 0.5;
    const float AvgLumB = 0.5;

    const vec3 LumCoeff = vec3(0.2125, 0.7154, 0.0721);

    vec3 AvgLumin  = vec3(AvgLumR, AvgLumG, AvgLumB);
    vec3 brtColor  = color * brt;
    vec3 intensity = vec3(dot(brtColor, LumCoeff));
    vec3 satColor  = mix(intensity, brtColor, sat);
    vec3 conColor  = mix(AvgLumin, satColor, con);

    return conColor;
}

void main() {
    // Mirror
    float isAfterHalfWidth = step(0.5, v_CamTexCoordinate.x);
    float isAfterHalfHeight = step(v_CamTexCoordinate.y, 0.5);

    float subX = ((v_zoomedCoords.x - 0.5) * 2.) * isAfterHalfWidth;
    float subY = ((v_zoomedCoords.y - 0.5)*2.) * isAfterHalfHeight;

    vec2 mirroredPixelCoords = vec2(v_zoomedCoords.x, v_zoomedCoords.y - (subY * mirrorActive));

    // Displacement
    float scaledDispX = (dispX * 0.06) - 0.03;
    float scaledDispY = (dispY * 0.06)- 0.03;

    // Feedback
    //vec2 feedbackCoord = vec2(-v_CamTexCoordinate.y, -v_CamTexCoordinate.x);
    vec2 feedbackCoord = mirroredPixelCoords.xy;
    //vec2 zoomCoord = (feedbackCoord - 0.5) * (dispX*4.) + (0.5);

    vec4 pix = texture2D(camTexture, v_CamTexCoordinate.xy);
    vec4 feedback = texture2D(bufferTexture, feedbackCoord.xy - vec2(scaledDispX, scaledDispY));

    float newopacity = opacity * 0.15;
    float fValue = (pix.r *0.29+ pix.g*0.6 + pix.b*0.11);
    float l1 = abs(threshold) - 0.07;
    float l2 = l1 + 0.07;

    //float threshBW = (threshold * 2.0) - 1.0;
    float bw_sel = step(threshold, 0.0);
    fValue = smoothstep(max(l1, 0.0), min(l2, 1.0), abs(bw_sel - fValue));
    pix.a = fValue;
    float isKeyOpen = step(pix.a, 0.1);
    // final mix needed to make alpha working
    vec4 color = mix(vec4(pix.rgb, 0.0), pix, pix.a);
    feedback.xyz = hueShift(feedback.xyz, hue*3.0);
    feedback.xyz = conSatBri(feedback.xyz, contrast+0.5, saturation + 0.5, brightness + 0.5);
    color = vec4(mix(color.rgb, feedback.rgb, 1.0 - pix.a - (isKeyOpen * newopacity)), 1.0);
    gl_FragColor = color;
}
