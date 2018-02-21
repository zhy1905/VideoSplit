#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES uTextureSampler;

float whiteBlack(vec3 vec3Color){
  float ave = (vec3Color.x + vec3Color.y+vec3Color.z)/3.0;
  if(ave>0.255)
    return 1.0;
  else
    return 0.0;
}

void main(){
  vec4 textureColor = texture2D(uTextureSampler,vTextureCoord);
  float finalColor = whiteBlack(textureColor.xyz);
  gl_FragColor = vec4(finalColor,finalColor,finalColor,1.0);
}

