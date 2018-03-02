package com.xiaopo.flying.videosplit.filter;

import android.graphics.Bitmap;

import com.xiaopo.flying.videosplit.gl.Shader;
import com.xiaopo.flying.videosplit.gl.ShaderParameter;

/**
 * @author wupanjie
 */
public abstract class AbstractShaderFilter implements ShaderFilter{
  private Shader shader;

  @Override
  public void prepare() {
    shader = new Shader.Builder()
        .attachVertex(getVertexShaderCode())
        .attachFragment(getFragmentShaderCode())
        .inflate(ShaderParameter.attribute(POSITION_ATTRIBUTE))
        .inflate(ShaderParameter.uniform(MATRIX_UNIFORM))
        .inflate(ShaderParameter.uniform(TEXTURE_MATRIX_UNIFORM))
        .inflate(ShaderParameter.uniform(TEXTURE_SAMPLER_UNIFORM))
        .assemble();
  }


  @Override
  public int getParameterHandle(final String name) {
    return shader.getParameterHandle(name);
  }

  @Override
  public void activate() {
    shader.activate();
  }

  @Override
  public void release() {
    shader.release();
  }
}
