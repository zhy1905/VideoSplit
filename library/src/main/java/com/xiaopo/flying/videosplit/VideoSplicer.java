package com.xiaopo.flying.videosplit;

import android.content.Context;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.util.Log;

import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.filter.ShaderFilter;
import com.xiaopo.flying.videosplit.mix.AVMixingTask;

import java.io.File;

/**
 * @author wupanjie
 */
public class VideoSplicer {
  private static final String TAG = "VideoSplicer";

  private Context context;
  private PuzzleLayout puzzleLayout;
  private int padding;
  private int width;
  private int height;
  private SpiltVideoCreator creator;
  private SplitShaderProgram shaderProgram;
  private File outputFile;
  private String audioPath;
  private File tempFile;

  private VideoSplicer(Context context) {
    this.context = context;
    this.shaderProgram = new SplitShaderProgram();
  }

  public static VideoSplicer newInstance(Context context) {
    return new VideoSplicer(context);
  }

  public VideoSplicer puzzleLayout(PuzzleLayout puzzleLayout) {
    this.puzzleLayout = puzzleLayout;
    return this;
  }

  public VideoSplicer width(int width) {
    this.width = width;
    return this;
  }

  public VideoSplicer height(int height) {
    this.height = height;
    return this;
  }

  public VideoSplicer padding(int padding) {
    this.padding = padding;
    return this;
  }

  public VideoSplicer addVideo(String path, Class<? extends ShaderFilter> filter) {
    shaderProgram.addPiece(path, filter);
    return this;
  }

  public VideoSplicer addVideo(String path, ShaderFilter filter) {
    shaderProgram.addPiece(path, filter);
    return this;
  }

  public VideoSplicer backgroundColor(@ColorInt int color) {
    shaderProgram.setBackgroundColor(color);
    return this;
  }

  public VideoSplicer outputFile(File outputFile) {
    this.outputFile = outputFile;
    return this;
  }

  public VideoSplicer audioPath(String audioPath) {
    this.audioPath = audioPath;
    return this;
  }

  public void create() {
    puzzleLayout.setOuterBounds(new RectF(0, 0, width, height));
    puzzleLayout.layout();
    puzzleLayout.setPadding(padding);
    shaderProgram.setPuzzleLayout(puzzleLayout);
    final String tempPath = context.getCacheDir().getPath() + File.separator + "Temp" + System.currentTimeMillis() + ".mp4";
    tempFile = new File(tempPath);
    creator = new SpiltVideoCreator(tempFile, width, height, shaderProgram);
    creator.setViewport(width, height);
    creator.setOnRendererReadyListener(new SpiltVideoCreator.OnRendererListener() {
      @Override
      public void onRendererReady() {
        Log.d(TAG, "onRendererReady: ");
        creator.create();
      }

      @Override
      public void onRendererFinished() {
        Log.d(TAG, "onRendererFinished: ");
      }
    });
    creator.setOnProcessProgressListener(new SpiltVideoCreator.OnProcessProgressListener() {
      @Override
      public void onProcessStarted() {
        Log.d(TAG, "onProcessStarted: ");
      }

      @Override
      public void onProcessProgressChanged(int progress) {
        Log.d(TAG, "onProcessProgressChanged: progress -> " + progress);
      }

      @Override
      public void onProcessEnded() {
        Log.d(TAG, "onProcessEnded: ");
        mixAV();
      }
    });
    creator.start();
  }

  private void mixAV() {
    AsyncTask.SERIAL_EXECUTOR
        .execute(new AVMixingTask(outputFile, tempFile.getAbsolutePath(), audioPath, new AVMixingTask.AVMixListener() {
          @Override
          public void onMixStarted() {
            Log.d(TAG, "onMixStarted: ");
          }

          @Override
          public void onMixEnded() {
            tempFile.delete();
            Log.d(TAG, "onMixEnded: ");
          }
        }));
  }
}
