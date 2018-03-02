#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES uTextureSampler;

void main(){
  gl_FragColor = texture2D(uTextureSampler,vTextureCoord);
}
