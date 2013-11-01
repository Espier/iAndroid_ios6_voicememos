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
package org.espier.voicememos.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.espier.voicememos.R;
import org.espier.voicememos.model.VoiceMemo;
import org.espier.voicememos.util.AMRFileUtils;
import org.espier.voicememos.util.Recorder;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MemoEdit extends BaseUi implements OnClickListener, Recorder.OnStateChangedListener {
  private static final String TAG = "MemoDetail";

  private ImageView mStartView;
  private ImageView mEndView;
  private ImageView mStrenchView;
  private ImageView mPlayView;
  private TextView mCutTextView;

  /**
   * origin positon for the start view
   */
  private int mMarginLeft;
  /**
   * origin positon for the end view
   */
  private int mMarginRight;
  /**
   * current positon for the start view
   */
  private int mCurrentLeft;
  /**
   * current positon for the end view
   */
  private int mCurrentRight;
  private int mTotalLenght;
  private int mStartOffset;
  private int mEndOffset;
  private int mStartPostion;
  private int mEndPostion;

  private int mMemoId;
  private int mTrimDuration;
  private String mMemoFilePath;

  private MediaPlayer mCurrentMediaPlayer;
  private Recorder mRecorder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.memos_edit);

    mStartView = (ImageView) findViewById(R.id.startmarker);
    mEndView = (ImageView) findViewById(R.id.endmarker);
    mStrenchView = (ImageView) findViewById(R.id.strenchmarker);
    mPlayView = (ImageView) findViewById(R.id.memos_detail_play);
    mCutTextView = (TextView) findViewById(R.id.memos_detail_cut);

    mStartView.setOnTouchListener(movingEventListener);
    mEndView.setOnTouchListener(movingEventListener);
    mPlayView.setOnClickListener(this);
    mCutTextView.setOnClickListener(this);
    findViewById(R.id.memos_detail_cancel).setOnClickListener(this);

    mMemoFilePath = getIntent().getStringExtra(MemoDetail.MEMO_PATH);
    mMemoId = getIntent().getIntExtra(MemoDetail.MEMO_ID, -1);

    mRecorder = new Recorder();
    mRecorder.setOnStateChangedListener(this);

  }

  private OnTouchListener movingEventListener = new OnTouchListener() {
    int lastX = -1;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          if (lastX == -1) {
            mMarginLeft = mStartView.getLeft();
            mMarginRight = mEndView.getRight();
            mTotalLenght = mEndView.getLeft() - mStartView.getRight();
          }

          mCurrentLeft = mStartView.getLeft();
          mCurrentRight = mEndView.getRight();

          lastX = (int) event.getRawX();
          break;
        case MotionEvent.ACTION_MOVE:
          int dx = (int) event.getRawX() - lastX;
          int left = v.getLeft() + dx;
          int right = v.getRight() + dx;

          // the start view can not move to left of its origin position
          // it also should not move to right of the end view
          if (v == mStartView) {
            if (left < mMarginLeft) {
              Log.d("prb1", "left:" + left + "mMarginLeft:" + mMarginLeft);
              dx = dx + mMarginLeft - left;
              left = mMarginLeft;
              right = left + mStartView.getWidth();
            }

            if (right > mCurrentRight - mEndView.getWidth()) {
              dx = dx + mCurrentRight - mEndView.getWidth() - right;
              right = mCurrentRight - mEndView.getWidth();
              left = right - mStartView.getWidth();
            }
          }

          // the end view can not move to right of its origin position
          // it also should not move to left of the start view
          if (v == mEndView) {
            if (left < mCurrentLeft + mStartView.getWidth()) {
              dx = dx + mCurrentLeft + mStartView.getWidth() - left;
              left = mCurrentLeft + mStartView.getWidth();
              right = left + mEndView.getWidth();
            }

            if (right > mMarginRight) {
              dx = dx + mMarginRight - right;
              right = mMarginRight;
              left = right - mEndView.getWidth();
            }
          }

          v.layout(left, v.getTop(), right, v.getBottom());

          int left1 = mStrenchView.getLeft();
          if (v == mStartView) {
            left1 = mStrenchView.getLeft() + dx;
          }

          int top1 = mStrenchView.getTop();
          int right1 = mStrenchView.getRight();
          if (v == mEndView) {
            right1 = mStrenchView.getRight() + dx;
          }

          int bottom1 = mStrenchView.getBottom();

          mStrenchView.layout(left1, top1, right1, bottom1);

          lastX = (int) event.getRawX();

          break;
        case MotionEvent.ACTION_UP:
          mStartOffset = mStrenchView.getLeft() - mMarginLeft - mStartView.getWidth();
          mEndOffset = mStrenchView.getRight() - mMarginLeft - mStartView.getWidth();

          break;
      }
      return true;
    }
  };

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.memos_detail_play:

        if (mRecorder.getState() == Recorder.IDLE_STATE) {
          initTrimValue();
          mPlayView.setImageResource(R.drawable.trim_pause_selector);
          mCurrentMediaPlayer.seekTo(mStartPostion);
          play();
        } else if (mRecorder.getState() == Recorder.PLAYER_PAUSE_STATE) {
          mPlayView.setImageResource(R.drawable.trim_pause_selector);
          play();
        } else if (mRecorder.getState() == Recorder.PLAYING_STATE) {
          mPlayView.setImageResource(R.drawable.trim_play_selector);
          mRecorder.pausePlayback();
        }

        break;
      case R.id.memos_detail_cut:
        if (trimMemo()) {
          Intent result = new Intent();
          result.putExtra(MemoDetail.MEMO_PATH, mMemoFilePath);
          result.putExtra(MemoDetail.MEMO_DURATION, mTrimDuration);
          setResult(Activity.RESULT_OK, result);
        }

        finish();
      case R.id.memos_detail_cancel:
        finish();
      default:
        break;
    }

  }

  @Override
  protected void onStop() {
    super.onStop();

    if (mRecorder.getState() == Recorder.PLAYING_STATE
        || mRecorder.getState() == Recorder.PLAYER_PAUSE_STATE) {
      mRecorder.stopPlayback();
    }
  }

  private void play() {

    mRecorder.startPlayback();

    if (mTotalLenght != 0) {
      stopPlayBeforeEnd();
    }

  }

  private void initTrimValue() {
    // if(mRecorder == null){
    mCurrentMediaPlayer = mRecorder.createMediaPlayer(mMemoFilePath);
    int duration = mCurrentMediaPlayer.getDuration();
    Log.d(TAG, "duration:" + duration);
    if (mTotalLenght != 0) {
      mStartPostion = duration * mStartOffset / mTotalLenght;
      mEndPostion = duration * mEndOffset / mTotalLenght;
      mTrimDuration = mEndPostion - mStartPostion;
    }
    // }
  }

  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      stopPlayBeforeEnd();
    }
  };

  private void stopPlayBeforeEnd() {
    if (mRecorder.getState() == Recorder.PLAYING_STATE
        && mCurrentMediaPlayer.getCurrentPosition() > mEndPostion) {
      mRecorder.stopPlayback();
    } else {
      mHandler.sendEmptyMessageDelayed(1, 500);
    }
  }

  private boolean trimMemo() {
    initTrimValue();

    AMRFileUtils fileUtils = new AMRFileUtils();
    int startFrame = fileUtils.secondsToFrames(mStartPostion * 0.001);
    int endFrame = fileUtils.secondsToFrames(mEndPostion * 0.001);

    // memo has not been trimed or memo duration is less than 1s
    if (startFrame == 0 && endFrame == 0) {
      return false;
    } else if (mEndPostion - mStartPostion < 1000) {
      showToast(R.string.memo_short);
      return false;
    }

    File inputFile = new File(mMemoFilePath);
    File outputFile = Recorder.createTempFile();

    try {
      fileUtils.ReadFile(inputFile);
      fileUtils.WriteFile(outputFile, startFrame, endFrame - startFrame);

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }
    inputFile.delete();
    updateVoiceMemo(outputFile, mEndPostion - mStartPostion);
    mMemoFilePath = outputFile.getAbsolutePath();

    return true;

  }

  private void updateVoiceMemo(File outputFile, int duration) {
    if (duration < 1000) {
      return;
    }

    ContentValues cv = new ContentValues();
    // File file = mRecorder.sampleFile();
    long modDate = outputFile.lastModified();

    cv.put(VoiceMemo.Memos.DATA, outputFile.getAbsolutePath());
    cv.put(VoiceMemo.Memos.MODIFICATION_DATE, (int) (modDate / 1000));
    cv.put(VoiceMemo.Memos.DURATION, duration);

    if (mMemoId != -1) {
      Uri memoUri = ContentUris.withAppendedId(VoiceMemo.Memos.CONTENT_URI, mMemoId);
      getContentResolver().update(memoUri, cv, null, null);
    }

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
    if (state == Recorder.IDLE_STATE) {
      mPlayView.setImageResource(R.drawable.trim_play_selector);
    }

  }

}
