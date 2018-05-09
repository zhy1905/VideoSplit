package com.xiaopo.flying.demo;

import android.Manifest;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xiaopo.flying.demo.layout.OneLayout;
import com.xiaopo.flying.demo.layout.ThreeLayout;
import com.xiaopo.flying.demo.layout.TwoLayout;
import com.xiaopo.flying.demo.utils.DipPixelKit;
import com.xiaopo.flying.demo.utils.FileUtil;
import com.xiaopo.flying.puzzlekit.PuzzleLayout;
import com.xiaopo.flying.videosplit.SpiltVideoCreator;
import com.xiaopo.flying.videosplit.SpiltVideoRenderer;
import com.xiaopo.flying.videosplit.SplitShaderProgram;
import com.xiaopo.flying.videosplit.filter.NoFilter;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionNo;
import com.yanzhenjie.permission.PermissionYes;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.drakeet.multitype.MultiTypeAdapter;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "VideoSpilt";

  private RecyclerView videoList;
  private MultiTypeAdapter videoAdapter;
  private Set<Integer> selectedPositions = new LinkedHashSet<>();
  private ArrayList<VideoAttachment> videoAttachments = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initView();

    AndPermission.with(this)
        .permission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .requestCode(300)
        .callback(this)
        .start();
  }

  private void initView() {
    videoList = findViewById(R.id.video_list);
    videoList.setLayoutManager(new GridLayoutManager(this, 4));
    final int space = DipPixelKit.dip2px(MainActivity.this, 2);
    VideoItemDecoration itemDecoration
        = new VideoItemDecoration(4, space, false);
    videoList.addItemDecoration(itemDecoration);

    videoAdapter = new MultiTypeAdapter();
    final int screenWidth = DipPixelKit.getDeviceWidth(this);
    final int availableLength = screenWidth - 3 * DipPixelKit.dip2px(this, 2);
    VideoAttachmentBinder videoAttachmentBinder =
        new VideoAttachmentBinder(selectedPositions, 3, availableLength / 4, availableLength / 4);
    videoAdapter.register(VideoAttachment.class, videoAttachmentBinder);

    videoList.setAdapter(videoAdapter);

    findViewById(R.id.fab_action).setOnClickListener(this::startMix);
  }

  private void startMix(View view) {
    final int width = 1080;
    final int height = 1080;

    ArrayList<PuzzleLayout> puzzleLayouts = new ArrayList<>();
    puzzleLayouts.add(new OneLayout());
    puzzleLayouts.add(new TwoLayout());
    puzzleLayouts.add(new ThreeLayout());

    ArrayList<String> videoPaths = new ArrayList<>();
    for (Integer selectedPosition : selectedPositions) {
      videoPaths.add(videoAttachments.get(selectedPosition).getPath());
    }

    SplitShaderProgram shaderProgram = new SplitShaderProgram();

    for (String path : videoPaths) {
      shaderProgram.addPiece(path, NoFilter.class);
    }

    PuzzleLayout puzzleLayout = puzzleLayouts.get(videoPaths.size() - 1);
    puzzleLayout.setOuterBounds(new RectF(0, 0, width, height));
    puzzleLayout.layout();
    shaderProgram.setPuzzleLayout(puzzleLayout);

    File file = FileUtil.getNewFile(this, "VideoSplit", "creator.mp4");
    final SpiltVideoCreator creator = new SpiltVideoCreator(file, width, height, shaderProgram);
    creator.setViewport(width, height);
    creator.setOnRendererReadyListener(new SpiltVideoCreator.OnRendererReadyListener() {
      @Override
      public void onRendererReady() {
        creator.play();
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
        Log.d(TAG, "onProcessProgressChanged: progress is " + progress);
      }

      @Override
      public void onProcessEnded() {
        Log.d(TAG, "onProcessEnded: ");
      }
    });
    creator.start();

//    view.postDelayed(()->{
//      creator.stopRecording();
//      Toast.makeText(MainActivity.this, "finish", Toast.LENGTH_SHORT).show();
//    },10000);
  }

  private void toSplitVideo(View view) {
    ArrayList<String> videoPaths = new ArrayList<>();
    for (Integer selectedPosition : selectedPositions) {
      videoPaths.add(videoAttachments.get(selectedPosition).getPath());
    }

    Intent intent = new Intent(this, SplitVideoActivity.class);
    intent.putStringArrayListExtra("paths", videoPaths);
    startActivity(intent);
  }

  @PermissionYes(300)
  private void getPermissionYes(List<String> grantedPermissions) {
    loadVideos();
  }

  private void loadVideos() {
    MediaManager mediaManager = new MediaManager(this);
    videoAttachments.clear();
    videoAttachments.addAll(mediaManager.getAllVideos());
    videoAdapter.setItems(videoAttachments);
    videoAdapter.notifyDataSetChanged();
  }

  @PermissionNo(300)
  private void getPermissionNo(List<String> deniedPermissions) {
    Toast.makeText(this, "必须要权限", Toast.LENGTH_SHORT).show();
  }
}
