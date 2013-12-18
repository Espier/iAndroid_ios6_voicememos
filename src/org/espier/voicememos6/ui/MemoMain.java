/*
 * Copyright (C) 2013 robin.pei(webfanren@gmail.com)
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
package org.espier.voicememos6.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.espier.voicememos6.R;
import org.espier.voicememos6.model.VoiceMemo;
import org.espier.voicememos6.util.Recorder;
import org.espier.voicememos6.util.StorageUtil;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MemoMain extends BaseUi implements OnClickListener, Recorder.OnStateChangedListener {

  /**
   * label type that is not defined
   */
  public static final int LABEL_TYPE_NONE = 0;

  private static final int UPDATE_TIMER = 1;

  private VUMeter mVUMeter;
  private Recorder mRecorder;
  private ImageView mStartRecordView;
  private ImageView mListRecordView;
  private LinearLayout mRecordTimeBar;
  private TextView mRecordTimeView;

  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case UPDATE_TIMER:
          int state = mRecorder.getState();
          int duration = 0;
          String timePrefix = getString(R.string.recording);
          if (state == Recorder.RECORDING_STATE) {
            duration = mRecorder.progress();
          } else if (state == Recorder.IDLE_STATE) {
            duration = mRecorder.sampleLength();
            mRecorder.clear();
          } else if (state == Recorder.RECORDER_PAUSE_STATE) {
            duration = mRecorder.sampleLength();
            timePrefix = getString(R.string.record_stop);
          }
          String recordTime = DateUtils.formatElapsedTime(duration);
          mRecordTimeView.setText(timePrefix + " " + recordTime);
          sendEmptyMessageDelayed(UPDATE_TIMER, 1000);
          break;
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.memos_main);

    mRecorder = new Recorder();

    mVUMeter = (VUMeter) findViewById(R.id.uvmeter);
    mStartRecordView = (ImageView) findViewById(R.id.start_record);
    mListRecordView = (ImageView) findViewById(R.id.list_record);
    mRecordTimeBar = (LinearLayout) findViewById(R.id.record_time_bar);
    mRecordTimeView = (TextView) findViewById(R.id.record_time);

    mStartRecordView.setOnClickListener(this);
    mListRecordView.setOnClickListener(this);
    mVUMeter.setRecorder(mRecorder);
    mRecorder.setOnStateChangedListener(this);

  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start_record:
        if (!StorageUtil.hasDiskSpace()) {
          showMessage(R.string.storage_is_full);
          return;
        } else if (!StorageUtil.isStorageMounted()) {
          showMessage(R.string.insert_sd_card);;
          return;
        }

        stopMusic();
        if (mRecorder.getState() == Recorder.RECORDING_STATE) {
          mRecorder.pauseRecording();
          mStartRecordView.setImageResource(R.drawable.main_record_selector);
        } else {
          mRecordTimeBar.setVisibility(View.VISIBLE);
          mRecorder.startRecording(this);
          mListRecordView.setImageResource(R.drawable.main_stop_selector);
          mStartRecordView.setImageResource(R.drawable.main_pause_selector);
        }

        break;

      case R.id.list_record:
        if (mRecorder.getState() == Recorder.IDLE_STATE) {
          Intent intent = new Intent(this, MemoList.class);
          startActivity(intent);
        } else {
          mRecordTimeBar.setVisibility(View.GONE);
          mRecorder.stopRecording();
          // mRecorder.clear();
          insertVoiceMemo();
          ((ImageView) view).setImageResource(R.drawable.main_list_selector);
          mStartRecordView.setImageResource(R.drawable.main_record_selector);
        }
        ;

        break;

      default:
        break;
    }

  }



  @Override
  protected void onStop() {
    super.onStop();

    if (mRecorder.getState() == Recorder.RECORDING_STATE) {
      mRecorder.stopRecording();
    }
  }

  private void insertVoiceMemo() {
    Resources res = getResources();
    ContentValues cv = new ContentValues();
    long current = System.currentTimeMillis();
    File file = mRecorder.sampleFile();
    long modDate = file.lastModified();
    Date date = new Date(current);
    SimpleDateFormat formatter = new SimpleDateFormat(res.getString(R.string.time_format));
    String title = formatter.format(date);
    // long sampleLengthMillis = mRecorder.sampleLength() * 1000L;
    String filepath = file.getAbsolutePath();
    MediaPlayer mediaPlayer = mRecorder.createMediaPlayer(filepath);
    int duration = mediaPlayer.getDuration();
    mRecorder.stopPlayback();
    if(duration < 1000){
      return;
    }

    cv.put(VoiceMemo.Memos.DATA, filepath);
    cv.put(VoiceMemo.Memos.LABEL, title);
    cv.put(VoiceMemo.Memos.LABEL_TYPE, LABEL_TYPE_NONE);
    cv.put(VoiceMemo.Memos.CREATE_DATE, current);
    cv.put(VoiceMemo.Memos.MODIFICATION_DATE, (int) (modDate / 1000));
    cv.put(VoiceMemo.Memos.DURATION, duration);
    getContentResolver().insert(VoiceMemo.Memos.CONTENT_URI, cv);
  }

  @Override
  public void onError(int error) {
    int message = -1;
    switch (error) {
      case Recorder.SDCARD_ACCESS_ERROR:
        message = R.string.error_sdcard_access;
        break;
      case Recorder.INTERNAL_ERROR:
        message = R.string.error_app_internal;
        break;
    }
    if (message != -1) {
      showMessage(message);
    }
  }

  @Override
  public void onStateChanged(int state) {
    mVUMeter.invalidate();
    mHandler.sendEmptyMessage(UPDATE_TIMER);

  }

  private void stopMusic() {
    Intent i = new Intent("com.android.music.musicservicecommand");
    i.putExtra("command", "pause");

    sendBroadcast(i);
  };


}
