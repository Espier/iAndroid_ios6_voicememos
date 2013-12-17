/*
 * Copyright (C) 2011 The Android Open Source Project Copyright (C) 2013
 * robin.pei(webfanren@gmail.com)
 * 
 * The code is developed under sponsor from Beijing FMSoft Tech. Co. Ltd(http://www.fmsoft.cn)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.espier.voicememos6.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class Recorder implements OnCompletionListener, OnErrorListener {
  static final String SAMPLE_PREFIX = "recording";
  static final String SAMPLE_SUFFIX = ".amr";
  static final String SAMPLE_PATH_KEY = "sample_path";
  static final String SAMPLE_LENGTH_KEY = "sample_length";

  public static final int IDLE_STATE = 0;
  public static final int RECORDING_STATE = 1;
  public static final int PLAYING_STATE = 2;
  public static final int RECORDER_PAUSE_STATE = 3;
  public static final int PLAYER_PAUSE_STATE = 4;


  int mState = IDLE_STATE;

  public static final int NO_ERROR = 0;
  public static final int SDCARD_ACCESS_ERROR = 1;
  public static final int INTERNAL_ERROR = 2;
  public static final int IN_CALL_RECORD_ERROR = 3;

  private List<File> mTempMemoFiles = new ArrayList<File>();

  public interface OnStateChangedListener {
    public void onStateChanged(int state);

    public void onError(int error);
  }

  OnStateChangedListener mOnStateChangedListener = null;

  long mSampleStart = 0; // time at which latest record or play operation
  // started
  int mSampleLength = 0; // length of current sample
  File mSampleFile = null;

  MediaRecorder mRecorder = null;
  MediaPlayer mPlayer = null;

  public Recorder() {}



  public int getMaxAmplitude() {
    if (mState != RECORDING_STATE) return 0;
    return mRecorder.getMaxAmplitude();
  }


  public void setOnStateChangedListener(OnStateChangedListener listener) {
    mOnStateChangedListener = listener;
  }

  public int getState() {
    return mState;
  }

  public int progress() {
    if (mState == RECORDING_STATE)
      return (int) (mSampleLength + (System.currentTimeMillis() - mSampleStart) / 1000);
    return 0;
  }

  public int sampleLength() {
    return mSampleLength;
  }

  public File sampleFile() {
    return mSampleFile;
  }

  /**
   * Resets the recorder state. If a sample was recorded, the file is deleted.
   */
  public void delete() {
    stopRecording();

    if (mSampleFile != null) mSampleFile.delete();

    mSampleFile = null;
    mSampleLength = 0;

    signalStateChanged(IDLE_STATE);
  }

  /**
   * Resets the recorder state. If a sample was recorded, the file is left on disk and will be
   * reused for a new recording.
   */
  public void clear() {
    // stop();

    mSampleLength = 0;

    // signalStateChanged(IDLE_STATE);
  }

  public void startRecording(Context context) {
    File sampleDir = Environment.getExternalStorageDirectory();
    File tempFile = null;
    try {
      tempFile = File.createTempFile(SAMPLE_PREFIX, SAMPLE_SUFFIX, sampleDir);
    } catch (IOException e) {
      setError(SDCARD_ACCESS_ERROR);
      return;
    }

    mTempMemoFiles.add(tempFile);
    mRecorder = new MediaRecorder();
    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    mRecorder.setOutputFile(tempFile.getAbsolutePath());

    // Handle IOException
    try {
      mRecorder.prepare();
    } catch (IOException exception) {
      setError(INTERNAL_ERROR);
      mRecorder.reset();
      mRecorder.release();
      mRecorder = null;
      return;
    }
    // Handle RuntimeException if the recording couldn't start
    try {
      mRecorder.start();
    } catch (RuntimeException exception) {
      AudioManager audioMngr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      boolean isInCall = ((audioMngr.getMode() == AudioManager.MODE_IN_CALL));
      if (isInCall) {
        setError(IN_CALL_RECORD_ERROR);
      } else {
        setError(INTERNAL_ERROR);
      }
      mRecorder.reset();
      mRecorder.release();
      mRecorder = null;
      return;
    }
    mSampleStart = System.currentTimeMillis();
    setState(RECORDING_STATE);
  }

  public static File createTempFile() {
    File sampleDir = Environment.getExternalStorageDirectory();
    File tempFile = null;
    try {
      tempFile = File.createTempFile(SAMPLE_PREFIX, SAMPLE_SUFFIX, sampleDir);
    } catch (IOException e) {
      // setError(SDCARD_ACCESS_ERROR);
      return null;
    }

    return tempFile;

  }

  public void stopRecording() {
    if (mRecorder != null) {
      try {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
      } catch (RuntimeException ex) {
        mRecorder = null;
      }

    }

    mergeFiles(mTempMemoFiles);
    mTempMemoFiles.clear();

    if (mState == RECORDING_STATE) {
      mSampleLength += (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
    }

    setState(IDLE_STATE);
  }

  public void pauseRecording() {
    if (mRecorder == null) return;
    mRecorder.stop();
    mRecorder.release();
    mRecorder = null;
    mSampleLength += (int) ((System.currentTimeMillis() - mSampleStart) / 1000);

    setState(RECORDER_PAUSE_STATE);

  }

  public MediaPlayer createMediaPlayer(String path) {

    mPlayer = new MediaPlayer();
    try {
      mPlayer.setDataSource(path);
      mPlayer.setOnCompletionListener(this);
      mPlayer.setOnErrorListener(this);
      mPlayer.prepare();

    } catch (IllegalArgumentException e) {
      setError(INTERNAL_ERROR);
      mPlayer = null;
      return null;
    } catch (IOException e) {
      setError(SDCARD_ACCESS_ERROR);
      mPlayer = null;
      return null;
    }

    return mPlayer;
  }

  public void startPlayback() {
    mPlayer.start();

    mSampleStart = System.currentTimeMillis();
    setState(PLAYING_STATE);
  }

  public void seekTo(int msec) {
    if (mPlayer != null) {
      mPlayer.seekTo(msec);
    }
  }

  public void pausePlayback() {
    if (mPlayer == null) // we were not in playback
      return;
    mPlayer.pause();

    setState(PLAYER_PAUSE_STATE);
  }

  public void stopPlayback() {
    if (mPlayer == null) // we were not in playback
      return;

    mPlayer.stop();
    mPlayer.release();
    mPlayer = null;

    setState(IDLE_STATE);
  }



  public boolean onError(MediaPlayer mp, int what, int extra) {
    stopPlayback();
    setError(SDCARD_ACCESS_ERROR);
    return true;
  }

  public void onCompletion(MediaPlayer mp) {
    stopPlayback();
  }

  private void setState(int state) {
    if (state == mState) return;

    mState = state;
    signalStateChanged(mState);
  }

  private void signalStateChanged(int state) {
    if (mOnStateChangedListener != null) mOnStateChangedListener.onStateChanged(state);
  }

  private void setError(int error) {
    if (mOnStateChangedListener != null) mOnStateChangedListener.onError(error);
  }

  private void mergeFiles(List<File> files) {
    if (files.size() == 1) {
      mSampleFile = files.get(0);
      return;
    }

    File sampleDir = Environment.getExternalStorageDirectory();
    File tempFile = null;
    try {
      tempFile = File.createTempFile(SAMPLE_PREFIX, SAMPLE_SUFFIX, sampleDir);
    } catch (IOException e) {
      setError(SDCARD_ACCESS_ERROR);
      return;
    }
    FileOutputStream fileOutputStream = null;

    try {
      fileOutputStream = new FileOutputStream(tempFile);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (int index = 0; index < files.size(); index++) {
      File file = files.get(index);
      try {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] myByte = new byte[fileInputStream.available()];
        int length = myByte.length;

        if (index == 0) {
          while (fileInputStream.read(myByte) != -1) {
            fileOutputStream.write(myByte);
          }
        }

        else {
          while (fileInputStream.read(myByte) != -1) {
            fileOutputStream.write(myByte, 6, length - 6);
          }
        }

        fileOutputStream.flush();
        fileInputStream.close();

        file.delete();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      fileOutputStream.flush();
      fileOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    mSampleFile = tempFile;
  }
}
