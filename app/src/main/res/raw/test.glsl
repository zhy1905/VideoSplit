precision lowp float;
varying highp vec2 textureCoordinate;
 
uniform sampler2D uTextureSampler;
uniform sampler2D uTextureFilter;
 
void main(){
     
  vec3 texel = texture2D(uTextureSampler, textureCoordinate).rgb;
     
  texel = vec3(
               texture2D(uTextureFilter, vec2(texel.r, .16666)).r,
               texture2D(uTextureFilter, vec2(texel.g, .5)).g,
               texture2D(uTextureFilter, vec2(texel.b, .83333)).b);
     
  gl_FragColor = vec4(texel, 1.0);
 }
