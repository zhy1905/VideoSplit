package com.xiaopo.flying.videosplit.gl;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * @author wupanjie
 */
public abstract class ShaderProgram {
  private static final String TAG = "ShaderProgram";
  public static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;
  public static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;
  public static final int BYTE_SIZE = Byte.SIZE;
  public static final int MATRIX_SIZE = 16;

  public static void checkError(String op) {
    int error = GLES20.glGetError();
    if (error != GLES20.GL_NO_ERROR) {
      String msg = op + ": glError 0x" + Integer.toHexString(error);
      Log.e(TAG, msg);
      throw new RuntimeException(msg);
    }
  }

  private int[] linkStatus = new int[1];
  private int[] tempId = new int[1];
  private int program = -1;
  private int vertexShader = -1;
  private int fragmentShader = -1;
  private ArrayList<ShaderParameter> shaderParameters = new ArrayList<>();
  private ArrayList<Integer> vboIds = new ArrayList<>();
  private ArrayList<Integer> textureIds = new ArrayList<>();

  private int viewportWidth;
  private int viewportHeight;

  public int getProgram() {
    return program;
  }

  public int getViewportWidth() {
    return viewportWidth;
  }

  public int getViewportHeight() {
    return viewportHeight;
  }

  public void setViewport(int width, int height) {
    this.viewportWidth = width;
    this.viewportHeight = height;
  }

  public abstract String getVertexShader();

  public abstract String getFragmentShader();

  public abstract void inflateShaderParameter();

  protected int inflateUniform(final String name) {
    shaderParameters.add(ShaderParameter.uniform(name));
    return shaderParameters.size() - 1;
  }

  protected int inflateAttribute(final String name) {
    shaderParameters.add(ShaderParameter.attribute(name));
    return shaderParameters.size() - 1;
  }

  protected int getParameterHandle(int index) {
    return shaderParameters.get(index).getHandle();
  }

  public void assemble() {
    vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader());
    fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader());
    program = assembleProgram(vertexShader, fragmentShader, linkStatus);

    shaderParameters.clear();
    inflateShaderParameter();
    loadHandle();
  }

  public abstract void prepare();

  public abstract void run();

  public void release() {
    int[] bufferIds = new int[vboIds.size()];
    for (int i = 0; i < bufferIds.length; i++) {
      bufferIds[i] = vboIds.get(i);
    }
    GLES20.glDeleteBuffers(bufferIds.length, bufferIds, 0);

    int[] texIds = new int[textureIds.size()];
    for (int i = 0; i < texIds.length; i++) {
      texIds[i] = textureIds.get(i);
    }
    GLES20.glDeleteTextures(texIds.length, texIds, 0);

    GLES20.glDeleteShader(vertexShader);
    GLES20.glDeleteShader(fragmentShader);
    GLES20.glDeleteProgram(program);

    vertexShader = -1;
    fragmentShader = -1;
    program = -1;
    shaderParameters.clear();
    vboIds.clear();
    textureIds.clear();
  }

  // return vboId
  protected int uploadBuffer(FloatBuffer buffer) {
    return uploadBuffer(buffer, FLOAT_SIZE);
  }

  // return vboId
  protected int uploadBuffer(ShortBuffer buffer) {
    return uploadBuffer(buffer, SHORT_SIZE);
  }

  // return vboId
  protected int uploadBuffer(ByteBuffer buffer) {
    return uploadBuffer(buffer, BYTE_SIZE);
  }

  private int uploadBuffer(Buffer buffer, int elementSize) {
    GLES20.glGenBuffers(1, tempId, 0);
    checkError("gen buffer");
    int bufferId = tempId[0];
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
    checkError("bind buffer");
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * elementSize, buffer,
        GLES20.GL_STATIC_DRAW);
    checkError("buffer data");
    vboIds.add(bufferId);
    return bufferId;
  }

  protected int generateTexture() {
    GLES20.glGenTextures(1, tempId, 0);
    textureIds.add(tempId[0]);
    return tempId[0];
  }

  protected void generateTextures(int size, int[] ids, int offset) {
    GLES20.glGenTextures(size, ids, offset);
    for (int i = 0; i < size; i++) {
      textureIds.add(ids[i]);
    }
  }

  private int loadShader(int type, String shaderCode) {
    int shader = GLES20.glCreateShader(type);

    GLES20.glShaderSource(shader, shaderCode);
    checkError("shader source");
    GLES20.glCompileShader(shader);
    checkError("compile shader");

    return shader;
  }

  private int assembleProgram(int vertexShader, int fragmentShader, int[] linkStatus) {
    int program = GLES20.glCreateProgram();
    checkError("create program");
    if (program == 0) {
      throw new RuntimeException("Cannot create GL program: " + GLES20.glGetError());
    }
    GLES20.glAttachShader(program, vertexShader);
    checkError("attach vertex shader");
    GLES20.glAttachShader(program, fragmentShader);
    checkError("attach fragment shader");
    GLES20.glLinkProgram(program);
    checkError("link program");
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
    if (linkStatus[0] != GLES20.GL_TRUE) {
      Log.e(TAG, "Could not link program: ");
      Log.e(TAG, GLES20.glGetProgramInfoLog(program));
      GLES20.glDeleteProgram(program);
      program = 0;
    }
    return program;
  }


  protected void loadHandle() {
    for (ShaderParameter shaderParameter : shaderParameters) {
      shaderParameter.loadHandle(program);
    }
  }
}
