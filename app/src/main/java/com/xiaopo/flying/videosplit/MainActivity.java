package com.xiaopo.flying.videosplit;

import android.Manifest;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.layout.ThreeLayout;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, SpiltVideoRenderer.OnRendererReadyListener {
  private static final String TAG = "MainActivity";
  private TextureView textureView;
  private SurfaceTexture surfaceTexture;
  private SpiltVideoRenderer renderer;
  private Button btnRecord;
  private Button btnStop;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initView();

    AndPermission.with(this)
        .permission(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .requestCode(300)
        .callback(this)
        .start();
  }

  @PermissionYes(300)
  private void getPermissionYes(List<String> grantedPermissions) {
    if (surfaceTexture == null) {
      textureView.setSurfaceTextureListener(this);
    }
  }

  @PermissionNo(300)
  private void getPermissionNo(List<String> deniedPermissions) {

  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onStop() {
    super.onStop();
    renderer.getRenderHandler().sendShutdown();
  }

  private void play() {
    renderer.play();
  }

  private void initView() {
    textureView = findViewById(R.id.texture_view);
    btnRecord = findViewById(R.id.btn_record);
    btnStop = findViewById(R.id.btn_stop);

    btnRecord.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      renderer.startRecording();
      }
    });

    btnStop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        renderer.stopRecording();
      }
    });
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    this.surfaceTexture = surface;
    SplitShaderProgram shaderProgram = new SplitShaderProgram();
    shaderProgram.addPiece("/storage/emulated/0/QuickSaveVideo/taeyeon_ss4.mp4");
    shaderProgram.addPiece("/storage/emulated/0/QuickSaveVideo/taeyeon_ss2.mp4");
    shaderProgram.addPiece("/storage/emulated/0/QuickSaveVideo/taeyeon_ss3.mp4");

    PuzzleLayout threeLayout = new ThreeLayout();
    threeLayout.setOuterBounds(new RectF(0, 0, width, height));
    threeLayout.layout();
    shaderProgram.setPuzzleLayout(threeLayout);

    File file = FileUtil.getNewFile(MainActivity.this, "VideoSplit", "test.mp4");
    Log.d(TAG, "onSurfaceTextureAvailable: file is " + file.getAbsolutePath());
    renderer = new SpiltVideoRenderer(file, surfaceTexture, width, height, shaderProgram);
    renderer.setViewport(width, height);
    renderer.setOnRendererReadyListener(this);
    renderer.start();
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {

  }

  @Override
  public void onRendererReady() {
    play();
  }

  @Override
  public void onRendererFinished() {

  }
}
