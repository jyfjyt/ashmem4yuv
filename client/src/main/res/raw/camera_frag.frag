#extension GL_OES_EGL_image_external : require
//必须 写的 固定的  意思   用采样器
//所有float类型数据的精度是lowp
precision lowp float;

varying vec2 fTextureCoord;
//采样器  uniform=static
uniform sampler2D  fSampleY;
uniform sampler2D  fSampleUV;

const mat3 yuv2rgb = mat3(1, 0, 1.2802, 1, -0.214821, -0.380589, 1, 2.127982, 0);

void main(){
    //Opengl 自带函数

    vec3 yuv = vec3(1.1643 * (texture2D(fSampleY, fTextureCoord).r - 0.0625)
    , texture2D(fSampleUV, fTextureCoord).r - 0.5
    , texture2D(fSampleUV, fTextureCoord).a - 0.5);

    vec3 rgb =yuv* yuv2rgb;

    gl_FragColor = vec4(rgb, 1.0);
}