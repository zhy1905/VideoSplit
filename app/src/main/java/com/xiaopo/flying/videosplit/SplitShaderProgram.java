package com.xiaopo.flying.videosplit;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.xiaopo.flying.videosplit.gl.BufferUtil;
import com.xiaopo.flying.videosplit.gl.ShaderProgram;

import java.util.ArrayList;

/**
 * @author wupanjie
 */
public class SplitShaderProgram extends ShaderProgram {
  private static final int COORDS_PER_VERTEX = 2;
  private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;

  private static final int INDEX_POSITION = 0;
  private static final int INDEX_MATRIX = 1;
  private static final int INDEX_TEXTURE_MATRIX = 2;
  private static final int INDEX_TEXTURE_SAMPLER = 3;

  private static final String POSITION_ATTRIBUTE = "aPosition";
  private static final String MATRIX_UNIFORM = "uMatrix";
  private static final String TEXTURE_MATRIX_UNIFORM = "uTextureMatrix";
  private static final String TEXTURE_SAMPLER_UNIFORM = "uTextureSampler";

  private static final String VERTEX_SHADER_CODE = "uniform mat4 uMatrix;\n" +
      "uniform mat4 uTextureMatrix;\n" +
      "attribute vec2 aPosition;\n" +
      "varying vec2 vTextureCoord;\n" +
      "\n" +
      "void main(){\n" +
      "  vec4 pos = vec4(aPosition,0.0,1.0);\n" +
      "  gl_Position = uMatrix * pos;\n" +
      "  vTextureCoord = (uTextureMatrix * pos).xy;\n" +
      "}";

  private static final String FRAGMENT_SHADER_CODE = "#extension GL_OES_EGL_image_external : require\n" +
      "\n" +
      "precision mediump float;\n" +
      "varying vec2 vTextureCoord;\n" +
      "uniform samplerExternalOES uTextureSampler;\n" +
      "\n" +
      "void main(){\n" +
      "  gl_FragColor = texture2D(uTextureSampler,vTextureCoord);\n" +
      "}";

  private static final float vertexCoordinates[] = {
      0, 0, // Fill rectangle
      1, 0,
      0, 1,
      1, 1,
  };
  private int vertexBufferId;

  private float[] projectionMatrix = new float[16];
  private float[] viewMatrix = new float[16];
  private float[] cameraTransformMatrix = new float[16];

  private ArrayList<VideoPiece> videoPieces = new ArrayList<>();

  public void addPiece(final String path) {
    videoPieces.add(new VideoPiece(path));
  }

  @Override
  public String getVertexShader() {
    return VERTEX_SHADER_CODE;
  }

  @Override
  public String getFragmentShader() {
    return FRAGMENT_SHADER_CODE;
  }

  @Override
  public void inflateShaderParameter() {
    inflateAttribute(POSITION_ATTRIBUTE);
    inflateUniform(MATRIX_UNIFORM);
    inflateUniform(TEXTURE_MATRIX_UNIFORM);
    inflateUniform(TEXTURE_SAMPLER_UNIFORM);
  }

  @Override
  public void prepare() {
    vertexBufferId = uploadBuffer(BufferUtil.storeDataInBuffer(vertexCoordinates));

    final int size = videoPieces.size();
    int[] textureIds = new int[size];
    generateTextures(size, textureIds, 0);
    for (int i = 0; i < size; i++) {
      videoPieces.get(i).configOutput(textureIds[i]);
    }

    Matrix.setIdentityM(projectionMatrix, 0);
    Matrix.orthoM(projectionMatrix, 0, 0, getViewportWidth(), 0, getViewportHeight(), -1, 1);

    Matrix.setIdentityM(viewMatrix, 0);
    Matrix.translateM(viewMatrix, 0, 0, getViewportHeight(), 0);
    Matrix.scaleM(viewMatrix, 0, 1, -1, 1);
  }

  @Override
  public void run() {
    GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    GLES20.glViewport(0, 0, getViewportWidth(), getViewportHeight());
    //set shader
    GLES20.glUseProgram(getProgram());

    final int textureHandle = getParameterHandle(INDEX_TEXTURE_SAMPLER);
    final int textureMatrixHandle = getParameterHandle(INDEX_TEXTURE_MATRIX);
    final int matrixHandle = getParameterHandle(INDEX_MATRIX);

    videoPieces.get(0).setUniformsAndAttribs(textureHandle, textureMatrixHandle);
    videoPieces.get(0).setMatrix(540, 0, 1080, 608, matrixHandle, viewMatrix, projectionMatrix);
    drawElements();

    videoPieces.get(1).setUniformsAndAttribs(textureHandle, textureMatrixHandle);
    videoPieces.get(1).setMatrix(-540, 0, 1080, 608, matrixHandle, viewMatrix, projectionMatrix);
    drawElements();
  }

  @Override
  public void release() {
    super.release();
    for (VideoPiece videoPiece : videoPieces) {
      videoPiece.release();
    }
    videoPieces.clear();
  }


  private void drawElements() {
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
    GLES20.glVertexAttribPointer(
        getParameterHandle(INDEX_POSITION),
        2,
        GLES20.GL_FLOAT,
        false,
        VERTEX_STRIDE,
        0);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    GLES20.glEnableVertexAttribArray(getParameterHandle(INDEX_POSITION));
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCoordinates.length);
    GLES20.glDisableVertexAttribArray(getParameterHandle(INDEX_POSITION));
  }


  public void updatePreviewTexture() {
    for (VideoPiece videoPiece : videoPieces) {
      videoPiece.getOutputTexture().updateTexImage();
    }
  }

  public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener) {
    for (VideoPiece videoPiece : videoPieces) {
      videoPiece.getOutputTexture().setOnFrameAvailableListener(onFrameAvailableListener);
    }
  }

  public void play(){
    for (VideoPiece videoPiece : videoPieces) {
      videoPiece.play();
    }
  }
}
