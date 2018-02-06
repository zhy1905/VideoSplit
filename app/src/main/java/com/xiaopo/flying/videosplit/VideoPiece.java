package com.xiaopo.flying.videosplit;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import static com.xiaopo.flying.videosplit.gl.ShaderProgram.MATRIX_SIZE;

/**
 * @author wupanjie
 */
public class VideoPiece {

  private final String path;
  private final float[] positionMatrix = new float[MATRIX_SIZE];
  private final float[] textureMatrix = new float[MATRIX_SIZE];
  private VideoPlayer player;
  private SurfaceTexture outputTexture;
  private int textureId;

  public VideoPiece(String path) {
    this.path = path;
  }

  public void configOutput(int textureId) {
    this.textureId = textureId;
    outputTexture = new SurfaceTexture(textureId);
    player = new VideoPlayer(new Surface(outputTexture), path);
  }


  public float[] getPositionMatrix() {
    return positionMatrix;
  }

  public float[] getTextureMatrix() {
    return textureMatrix;
  }

  public SurfaceTexture getOutputTexture() {
    return outputTexture;
  }

  public void release() {
    outputTexture.release();
    player.destroy();

    outputTexture = null;
    player = null;
  }

  public void play() {
    player.play();
  }

  void setUniformsAndAttribs(final int textureHandle, final int textureMatrixHandle) {
    outputTexture.getTransformMatrix(textureMatrix);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
    GLES20.glUniform1i(textureHandle, 0);

    GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0);
  }

  void setMatrix(float x, float y, float width, float height, int matrixHandle, float[] baseMatrix, float[] projectionMatrix) {
    Matrix.translateM(positionMatrix, 0, baseMatrix, 0, x, y, 0f);
    Matrix.scaleM(positionMatrix, 0, width, height, 1f);
    Matrix.multiplyMM(positionMatrix, 0, projectionMatrix, 0, positionMatrix, 0);
    GLES20.glUniformMatrix4fv(matrixHandle, 1, false, positionMatrix, 0);
  }
}
