package com.xiaopo.flying.videosplit;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.xiaopo.flying.videosplit.gl.EglCore;
import com.xiaopo.flying.videosplit.gl.WindowSurface;

import java.lang.ref.WeakReference;

public class SpiltVideoRenderer extends Thread implements SurfaceTexture.OnFrameAvailableListener {
  private static final String TAG = SpiltVideoRenderer.class.getSimpleName();
  private static final String THREAD_NAME = "CameraRendererThread";

  protected Context context;

  protected int surfaceWidth;
  protected int surfaceHeight;

  protected float surfaceAspectRatio;

  private SurfaceTexture surfaceTexture;

  private EglCore eglCore;
  private WindowSurface windowSurface;

  private RenderHandler handler;

  private OnRendererReadyListener onRendererReadyListener;

  private SplitShaderProgram shaderProgram;

  public SpiltVideoRenderer(Context context, SurfaceTexture texture, int width, int height, SplitShaderProgram shaderProgram) {
    init(context, texture, width, height, shaderProgram);
  }

  private void init(Context context, SurfaceTexture texture, int width, int height, SplitShaderProgram shaderProgram) {
    this.setName(THREAD_NAME);

    this.context = context;
    this.surfaceTexture = texture;

    this.surfaceWidth = width;
    this.surfaceHeight = height;
    this.surfaceAspectRatio = (float) width / height;

    this.shaderProgram = shaderProgram;
  }

  private void initialize() {
    setViewport(surfaceWidth, surfaceHeight);
  }


  /**
   * Initialize all necessary components for GLES rendering, creating window surfaces for drawing
   * the preview as well as the surface that will be used by MediaRecorder for recording
   */
  public void initGL() {
    eglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

    //create preview surface
    windowSurface = new WindowSurface(eglCore, surfaceTexture);
    windowSurface.makeCurrent();

    initGLComponents();
  }

  protected void initGLComponents() {
    shaderProgram.assemble();
    shaderProgram.prepare();
    shaderProgram.setOnFrameAvailableListener(this);

    onSetupComplete();
  }

  public void deinitGL() {
    shaderProgram.release();
    windowSurface.release();
    eglCore.release();
  }

  /**
   * called when all setup is complete on basic GL stuffs
   * override for adding textures and other shaders and make sure to call
   * super so that we can let them know we're done
   */
  protected void onSetupComplete() {
    onRendererReadyListener.onRendererReady();
  }

  @Override
  public synchronized void start() {
    initialize();

    if (onRendererReadyListener == null)
      throw new RuntimeException("OnRenderReadyListener is not set! Set listener prior to calling start()");

    super.start();
  }

  @Override
  public void run() {
    Looper.prepare();

    handler = new RenderHandler(this);

    initGL();

    Looper.loop();

    deinitGL();

    onRendererReadyListener.onRendererFinished();
  }

  public void shutdown() {
    Looper.myLooper().quit();
  }

  public void play(){
    shaderProgram.play();
  }

  @Override
  public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    boolean swapResult;

    synchronized (this) {
      shaderProgram.updatePreviewTexture();

      if (eglCore.getGlVersion() >= 3) {
        draw();

        //swap main buff
        windowSurface.makeCurrent();
        swapResult = windowSurface.swapBuffers();
      } else //gl v2
      {
        draw();

        windowSurface.makeCurrent();
        swapResult = windowSurface.swapBuffers();
      }

      if (!swapResult) {
        // This can happen if the Activity stops without waiting for us to halt.
        Log.e(TAG, "swapBuffers failed, killing renderer thread");
        shutdown();
      }
    }
  }

  public void draw() {
    shaderProgram.run();
  }

  public void setViewport(int viewportWidth, int viewportHeight) {
    shaderProgram.setViewport(viewportWidth,viewportHeight);
  }

  public RenderHandler getRenderHandler() {
    return handler;
  }

  public void setOnRendererReadyListener(OnRendererReadyListener listener) {
    this.onRendererReadyListener = listener;

  }

  public static class RenderHandler extends Handler {
    private static final String TAG = RenderHandler.class.getSimpleName();

    private static final int MSG_SHUTDOWN = 0;

    private WeakReference<SpiltVideoRenderer> mWeakRenderer;

    public RenderHandler(SpiltVideoRenderer rt) {
      mWeakRenderer = new WeakReference<>(rt);
    }

    public void sendShutdown() {
      sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
    }

    @Override
    public void handleMessage(Message msg) {
      SpiltVideoRenderer renderer = mWeakRenderer.get();
      if (renderer == null) {
        Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
        return;
      }

      int what = msg.what;
      switch (what) {
        case MSG_SHUTDOWN:
          renderer.shutdown();
          break;
        default:
          throw new RuntimeException("unknown message " + what);
      }
    }
  }

  public interface OnRendererReadyListener {
    void onRendererReady();

    void onRendererFinished();
  }
}
