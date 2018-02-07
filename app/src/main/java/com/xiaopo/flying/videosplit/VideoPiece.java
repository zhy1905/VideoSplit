package com.xiaopo.flying.videosplit;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import static com.xiaopo.flying.videosplit.gl.ShaderProgram.MATRIX_SIZE;

/**
 * @author wupanjie
 */
class VideoPiece implements VideoCodec.OnVideoInfoListener {
  private static final String TAG = "VideoPiece";
  private final String path;
  private final float[] positionMatrix = new float[MATRIX_SIZE];
  private final float[] textureMatrix = new float[MATRIX_SIZE];
  private VideoCodec player;
  private SurfaceTexture outputTexture;
  private int textureId;
  private RectF displayArea = new RectF();
  private RectF textureArea = new RectF(0, 0, 1, 1);

  private int videoWidth;
  private int videoHeight;

  VideoPiece(String path) {
    this.path = path;
  }

  void configOutput(int textureId) {
    this.textureId = textureId;
    outputTexture = new SurfaceTexture(textureId);
    player = new VideoCodec(new Surface(outputTexture), path);
    player.setOnVideoInfoListener(this);
  }

  SurfaceTexture getOutputTexture() {
    return outputTexture;
  }

  void release() {
    outputTexture.release();
    player.destroy();

    outputTexture = null;
    player = null;
  }

  void play() {
    player.play();
  }

  void setDisplayArea(RectF area) {
    setDisplayArea(area.left, area.top, area.right, area.bottom);
  }

  private void setDisplayArea(float left, float top, float right, float bottom) {
    displayArea.set(left, top, right, bottom);

    float scale;

    float displayWidth = displayArea.width();
    float displayHeight = displayArea.height();

    if (videoWidth * displayHeight > displayWidth * videoHeight) {
      scale = (displayHeight) / videoHeight;
    } else {
      scale = (displayWidth) / videoWidth;
    }
    float scaleWidth = videoWidth * scale;
    float scaleHeight = videoHeight * scale;

    float offsetW = (scaleWidth - displayWidth) / 2;
    float offsetH = (scaleHeight - displayHeight) / 2;

    textureArea.set(offsetW, offsetH, scaleWidth - offsetW, scaleHeight - offsetH);
    normalize(textureArea, scaleWidth, scaleHeight);

//    Log.d(TAG, "width:" + videoWidth + ",height:" + videoHeight + ",scale:" + scale);
//    Log.d(TAG, "scaleWidth:" + scaleWidth + ",scaleHeight:" + scaleHeight);
//    Log.d(TAG, "setDisplayArea: texture area:" + textureArea.toShortString());

  }

  private void normalize(RectF textureArea, float scaleWidth, float scaleHeight) {
    textureArea.left = textureArea.left / scaleWidth;
    textureArea.top = textureArea.top / scaleHeight;
    textureArea.right = textureArea.right / scaleWidth;
    textureArea.bottom = textureArea.bottom / scaleHeight;
  }

  void setTexture(final int textureHandle, final int textureMatrixHandle) {
    outputTexture.getTransformMatrix(textureMatrix);

    textureMatrix[0] = textureArea.width();
    textureMatrix[5] = textureArea.height();
    textureMatrix[12] = textureArea.left;
    textureMatrix[13] = textureArea.top;

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
    GLES20.glUniform1i(textureHandle, 0);

    GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, textureMatrix, 0);
  }

  void setMatrix(int matrixHandle, float[] viewMatrix, float[] projectionMatrix) {
    final float x = displayArea.left;
    final float y = displayArea.top;
    final float width = displayArea.width();
    final float height = displayArea.height();
    Matrix.translateM(positionMatrix, 0, viewMatrix, 0, x, y, 0f);
    Matrix.scaleM(positionMatrix, 0, width, height, 1f);
    Matrix.multiplyMM(positionMatrix, 0, projectionMatrix, 0, positionMatrix, 0);
    GLES20.glUniformMatrix4fv(matrixHandle, 1, false, positionMatrix, 0);
  }

  @Override
  public void onVideoInfoParsed(int width, int height, float time) {
    this.videoWidth = width;
    this.videoHeight = height;
  }
}
