package com.xiaopo.flying.videosplit;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;

import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, SpiltVideoRenderer.OnRendererReadyListener {

  private String videoPath;
  private TextureView textureView;
  private SurfaceTexture surfaceTexture;
  SpiltVideoRenderer renderer;
  private TextureView textureView2;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    videoPath = "/storage/emulated/0/Pictures/ACamera/Test.mp4";

    Toast.makeText(MainActivity.this, videoPath, Toast.LENGTH_SHORT).show();

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

  }

  @PermissionNo(300)
  private void getPermissionNo(List<String> deniedPermissions) {

  }

  @Override
  protected void onResume() {
    super.onResume();
    if (surfaceTexture == null) {
      textureView.setSurfaceTextureListener(this);
    }

//    if (surfaceTexture2 == null){
//      textureView2.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//          final CameraRenderer renderer = new CameraRenderer(PlayerActivity.this, surface, width, height);
//          renderer.setViewport(width, height);
//          renderer.setOnRendererReadyListener(new CameraRenderer.OnRendererReadyListener() {
//            @Override
//            public void onRendererReady() {
//              VideoPlayer videoPlayer = new VideoPlayer(new Surface(renderer.getPreviewTexture()),
//                  "/storage/emulated/0/Pictures/ACamera/Test2.mp4");
//              videoPlayer.play();
//            }
//
//            @Override
//            public void onRendererFinished() {
//
//            }
//          });
//          renderer.start();
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//          return false;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//        }
//      });
//    }
  }

  private void play() {
//    VideoPlayer videoPlayer = new VideoPlayer(new Surface(renderer.getPreviewTexture()), videoPath);
//    VideoPlayer videoPlayer2 = new VideoPlayer(new Surface(renderer.getPreviewTexture2()),
//        "/storage/emulated/0/Pictures/ACamera/Test2.mp4");
//    videoPlayer2.play();
//    videoPlayer.play();
    renderer.play();
  }

  private void initView() {
    textureView = findViewById(R.id.texture_view);
    textureView2 = findViewById(R.id.texture_view2);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    this.surfaceTexture = surface;
    SplitShaderProgram shaderProgram = new SplitShaderProgram();
    shaderProgram.addPiece(videoPath);
    shaderProgram.addPiece("/storage/emulated/0/Pictures/ACamera/Test2.mp4");
    renderer = new SpiltVideoRenderer(this, surfaceTexture, width, height, shaderProgram);
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
