uniform mat4 uMatrix;
uniform mat4 uTextureMatrix;
attribute vec2 aPosition;
varying vec2 vTextureCoord;

void main(){
  vec4 pos = vec4(aPosition,0.0,1.0);
  gl_Position = uMatrix * pos;
  vTextureCoord = (uTextureMatrix * pos).xy;
}
