package com.xiaopo.flying.videosplit;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author wupanjie
 */
public class AVMixTask implements Runnable {
  private static final String TAG = "AVMixTask";
  private final File output;
  private final String videoPath;
  private final String audioPath;
  private final AVMixListener listener;

  public AVMixTask(File output, String videoPath, String audioPath, AVMixListener listener) {
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
    mixAV(output);
    if (listener != null) {
      listener.onMixEnded();
    }
  }

  private void mixAV(File output) {
    try {
      MediaExtractor videoExtractor = new MediaExtractor();
      videoExtractor.setDataSource(videoPath);
      MediaFormat videoFormat = null;
      int videoTrackIndex = -1;
      int videoTrackCount = videoExtractor.getTrackCount();
      for (int i = 0; i < videoTrackCount; i++) {
        videoFormat = videoExtractor.getTrackFormat(i);
        String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
        if (mimeType.startsWith("video/")) {
          videoTrackIndex = i;
          break;
        }
      }

      MediaExtractor audioExtractor = new MediaExtractor();
      audioExtractor.setDataSource(audioPath);
      MediaFormat audioFormat = null;
      int audioTrackIndex = -1;
      int audioTrackCount = audioExtractor.getTrackCount();
      for (int i = 0; i < audioTrackCount; i++) {
        audioFormat = audioExtractor.getTrackFormat(i);
        String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
        if (mimeType.startsWith("audio/")) {
          audioTrackIndex = i;
          break;
        }
      }

      videoExtractor.selectTrack(videoTrackIndex);
      audioExtractor.selectTrack(audioTrackIndex);

      MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
      MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

      MediaMuxer mediaMuxer = new MediaMuxer(output.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
      int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
      int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
      mediaMuxer.start();

      ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
      long sampleTime = 0;
      {
        videoExtractor.readSampleData(byteBuffer, 0);
        if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
          videoExtractor.advance();
        }
        videoExtractor.readSampleData(byteBuffer, 0);
        long secondTime = videoExtractor.getSampleTime();
        videoExtractor.advance();
        long thirdTime = videoExtractor.getSampleTime();
        sampleTime = Math.abs(thirdTime - secondTime);
      }
      videoExtractor.unselectTrack(videoTrackIndex);
      videoExtractor.selectTrack(videoTrackIndex);

      int videoFrameCount = 0;
      int audioFrameCount = 0;

      while (true) {
        int readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0);
        if (readVideoSampleSize < 0) {
          break;
        }
        videoBufferInfo.size = readVideoSampleSize;
        videoBufferInfo.presentationTimeUs += sampleTime;
        videoBufferInfo.offset = 0;
        videoBufferInfo.flags = videoExtractor.getSampleFlags();
        mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo);
        videoExtractor.advance();
        videoFrameCount++;
      }

      while (true) {
        int readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
        if (readAudioSampleSize < 0) {
          break;
        }

        if (audioFrameCount >= videoFrameCount) {
          break;
        }

        audioBufferInfo.size = readAudioSampleSize;
        audioBufferInfo.presentationTimeUs += sampleTime;
        audioBufferInfo.offset = 0;
        audioBufferInfo.flags = videoExtractor.getSampleFlags();
        mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo);
        audioExtractor.advance();
        audioFrameCount++;
      }

      Log.d(TAG, "combineVideo: sampleTime : " + sampleTime);
      Log.d(TAG, "combineVideo: videoTime : " + videoBufferInfo.presentationTimeUs);
      Log.d(TAG, "combineVideo: audioTime : " + audioBufferInfo.presentationTimeUs);

      mediaMuxer.stop();
      mediaMuxer.release();
      videoExtractor.release();
      audioExtractor.release();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public interface AVMixListener {

    void onMixStarted();

    void onMixEnded();

  }
}
