package com.xiaopo.flying.videosplit.mix;

import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * @author wupanjie
 */
public class AVMixingTask implements Runnable {
  private static final String TAG = "AVMixingTask";
  private final File output;
  private final String videoPath;
  private final String audioPath;
  private final AVMixListener listener;

  public AVMixingTask(File output, String videoPath, String audioPath, AVMixListener listener) {
    this.output = output;
    this.videoPath = videoPath;
    this.audioPath = audioPath;
    this.listener = listener;
  }

  @Override
  public void run() {
    if (listener != null) {
      listener.onMixStarted();
    }
    mixAV();
    if (listener != null) {
      listener.onMixEnded();
    }
  }

  private void mixAV(){
    try {
      MediaMuxer muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
      VideoMixer videoMixer = new VideoMixer(muxer, videoPath);
      AudioMixer audioMixer = new AudioMixer(muxer, audioPath);

      final long mixDuration = Math.min(videoMixer.getDurationUs(), audioMixer.getDurationUs());
      Log.d(TAG, "mixAV: duration is " + mixDuration);
      // if audio need transcode, let transcoder to start muxer
      if (!audioMixer.needTranscode()) {
         muxer.start();
      }
      while (!(videoMixer.isMixEnded() && audioMixer.isMixEnded())){
        boolean mixed = audioMixer.mix(mixDuration) || videoMixer.mix(mixDuration);
      }
      muxer.stop();
      muxer.release();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public interface AVMixListener {

    void onMixStarted();

    void onMixEnded();

  }
}
