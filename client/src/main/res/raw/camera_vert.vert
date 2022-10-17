// 把顶点坐标给这个变量， 确定要画画的形状
//字节定义的  4个   数组  矩阵
attribute vec4 vPosition;

//cpu
//接收纹理坐标，接收采样器采样图片的坐标  camera
attribute vec2 vCoord;

//传给片元着色器 像素点
varying vec2 fTextureCoord;

void main(){
    //gpu需要渲染图像的形状
    gl_Position=vPosition;
    fTextureCoord=vCoord;
}