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
package org.espier.voicememos6.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.espier.voicememos6.R;
import org.espier.voicememos6.model.VoiceMemo;
import org.espier.voicememos6.util.MemosUtils;
import org.espier.voicememos6.util.Recorder;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MemoList extends BaseUi
    implements
      OnItemClickListener,
      Recorder.OnStateChangedListener,
      OnClickListener {


  private static final int REFRESH = 1;
  private static final int DEL_REQUEST = 2;

  private ListView mVoiceMemoListView;
  private Recorder mRecorder;
  private MediaPlayer mCurrentMediaPlayer;
  private SeekBar mProgress;
  private int mCurrentDuration;
  private TextView mCurrentTime;
  private TextView mCurrentRemain;

  private int mCurrentPosition = -1;
  private String mCurrentPath = null;
  private int mCurrentMemoId = -1;
  private boolean isCurrentPosition = false;

  private VoiceMemoListAdapter mVoiceMemoListAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);

    setContentView(R.layout.memos_list);

    mProgress = (SeekBar) findViewById(android.R.id.progress);
    mCurrentTime = (TextView) findViewById(R.id.current_positon);
    mCurrentRemain = (TextView) findViewById(R.id.current_remain);
    mVoiceMemoListView = (ListView) findViewById(R.id.memo_list);

    mRecorder = new Recorder();
    if (mProgress instanceof SeekBar) {
      SeekBar seeker = (SeekBar) mProgress;
      seeker.setOnSeekBarChangeListener(mSeekListener);
    }
    mProgress.setMax(1000);

    Cursor cs = managedQuery(VoiceMemo.Memos.CONTENT_URI, null, null, null, null);
    mVoiceMemoListAdapter =
        new VoiceMemoListAdapter(MemoList.this, R.layout.memos_item, cs, new String[] {},
            new int[] {});
    mVoiceMemoListView.setAdapter(mVoiceMemoListAdapter);
    mVoiceMemoListView.setOnItemClickListener(this);

    findViewById(R.id.memo_share).setOnClickListener(this);
    findViewById(R.id.memo_del).setOnClickListener(this);
    findViewById(R.id.memo_list_finish).setOnClickListener(this);

    updateOperatorBtn(false);

  }

  private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    public void onStartTrackingTouch(SeekBar bar) {}

    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
      if (!fromuser) return;
      int pos = mCurrentDuration * progress / 1000;
      mRecorder.seekTo(pos);
    }

    public void onStopTrackingTouch(SeekBar bar) {}
  };

  @Override
  protected void onStop() {
    super.onStop();

    if (mRecorder.getState() == Recorder.PLAYING_STATE
        || mRecorder.getState() == Recorder.PLAYER_PAUSE_STATE) {
      mRecorder.stopPlayback();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    mCurrentPosition = position;
    mVoiceMemoListAdapter.notifyDataSetChanged();

    mCurrentPath = (String) view.findViewById(R.id.memos_item_path).getTag();
    mCurrentMemoId = (Integer) view.findViewById(R.id.memos_item__id).getTag();
    mCurrentDuration = (Integer) view.findViewById(R.id.memos_item_duration).getTag();

    updateOperatorBtn(true);
    resetPlayer();

    int state = mRecorder.getState();
    if (state == Recorder.PLAYING_STATE) {
      mRecorder.stopPlayback();
    }

  }

  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case REFRESH:
          long next = refreshNow();
          queueNextRefresh(next);
          break;
      }
    }
  };

  private void queueNextRefresh(long delay) {
    if (mRecorder.getState() == Recorder.PLAYING_STATE) {
      Message msg = mHandler.obtainMessage(REFRESH);
      mHandler.removeMessages(REFRESH);
      mHandler.sendMessageDelayed(msg, delay);
    }
  }

  private long refreshNow() {
    if (mCurrentMediaPlayer == null || mRecorder.getState() != Recorder.PLAYING_STATE) {

      return 500;
    }

    // try {
    long pos = mCurrentMediaPlayer.getCurrentPosition();
    if ((pos >= 0) && (mCurrentDuration > 0)) {
      mCurrentTime.setText(MemosUtils.makeTimeString(this, pos / 1000));
      mCurrentRemain.setText("-"
          + MemosUtils.makeTimeString(this, ((mCurrentDuration - pos) / 1000)));
      int progress = (int) (1000 * pos / mCurrentDuration);
      mProgress.setProgress(progress);

    } else {
      mCurrentTime.setText("0:00");
      mProgress.setProgress(1000);
    }
    // calculate the number of milliseconds until the next full second,
    // so
    // the counter can be updated at just the right time
    long remaining = 1000 - (pos % 1000);

    // approximate how often we would need to refresh the slider to
    // move it smoothly
    int width = mProgress.getWidth();
    if (width == 0) width = 320;
    long smoothrefreshtime = mCurrentDuration / width;

    if (smoothrefreshtime > remaining) return remaining;
    if (smoothrefreshtime < 20) return 20;
    return smoothrefreshtime;
    // return 500;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.memo_share:
        MemosUtils.shareMemo(this, mCurrentPath);
        break;
      case R.id.memo_del:
        if (mRecorder.getState() != Recorder.IDLE_STATE) {
          mRecorder.stopPlayback();
        }
        Intent delIntent = new Intent(this, MemoDelete.class);
        startActivityForResult(delIntent, DEL_REQUEST);
        break;
      case R.id.memo_list_finish:
        finish();
      default:
        break;
    }

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO Auto-generated method stub
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == DEL_REQUEST) {
        deleteMemo(mCurrentMemoId);
        mVoiceMemoListAdapter.notifyDataSetChanged();
        updateOperatorBtn(false);
        mCurrentDuration = 0;
        resetPlayer();
      }
    }
  }

  private void deleteMemo(int memoId) {
    Uri memoUri = ContentUris.withAppendedId(VoiceMemo.Memos.CONTENT_URI, memoId);
    getContentResolver().delete(memoUri, null, null);
    File file = new File(mCurrentPath);
    if (file.exists()) {
      file.delete();
    }
    mCurrentPosition = -1;
  }

  class VoiceMemoListAdapter extends SimpleCursorAdapter {

    private Context mContext;
    private int mMemoIdx;
    private int mPathIdx;
    private int mLabelIdx;
    private int mLabelTypeIdx;
    private int mDurationIdx;
    private int mCreateDateIdx;
    private int mCurrentBgColor;

    public VoiceMemoListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
      super(context, layout, c, from, to);
      mContext = context;
      mCurrentBgColor = Color.WHITE;
      setupColumnIndices(c);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      Log.d("memo", "getView, mCurrentPosition:" + mCurrentPosition);
      if (mCurrentPosition == position) {
        isCurrentPosition = true;
        mCurrentBgColor = getResources().getColor(R.color.bar_blue);
      } else {
        isCurrentPosition = false;
        mCurrentBgColor = Color.WHITE;
      }
      return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      View v = super.newView(context, cursor, parent);

      ViewHolder vh = new ViewHolder();
      vh.playControl = (ImageView) v.findViewById(R.id.memos_item_play);
      vh.tag = (TextView) v.findViewById(R.id.memos_item_title);
      vh.createDate = (TextView) v.findViewById(R.id.memos_item_create_date);
      vh.detailControl = (ImageView) v.findViewById(R.id.memos_item_detail);
      vh.duration = (TextView) v.findViewById(R.id.memos_item_duration);
      vh.id = (TextView) v.findViewById(R.id.memos_item__id);
      vh.path = (TextView) v.findViewById(R.id.memos_item_path);

      v.setTag(vh);

      return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

      final ViewHolder vh = (ViewHolder) view.getTag();
      vh.tag.setText(cursor.getString(mLabelIdx));

      int secs = cursor.getInt(mDurationIdx);
      if (secs == 0) {
        vh.duration.setText("");
      } else {
        vh.duration.setText(MemosUtils.makeTimeString(context, secs / 1000));
        vh.duration.setTag(secs);
      }

      Long date = cursor.getLong(mCreateDateIdx);
      String dateFormat = getString(R.string.date_time_format);
      int labelType = cursor.getInt(mLabelTypeIdx);
      if (labelType == MemoMain.LABEL_TYPE_NONE) {
        dateFormat = getString(R.string.date_format);
      }
      SimpleDateFormat format = new SimpleDateFormat(dateFormat);
      Date d = new Date(date);
      String dd = format.format(d);
      vh.createDate.setText(dd);

      final String path = cursor.getString(mPathIdx);
      final Integer id = cursor.getInt(mMemoIdx);
      vh.path.setTag(path);
      vh.id.setTag(id);
     

      File file = new File(path);
      if (!file.exists()) {
        vh.detailControl.setEnabled(false);
        vh.playControl.setVisibility(View.INVISIBLE);
        if (isCurrentPosition) {
          view.setBackgroundColor(mCurrentBgColor);
          vh.tag.setTextColor(Color.WHITE);
          vh.createDate.setTextColor(Color.WHITE);
          vh.duration.setTextColor(Color.WHITE);
        }else{
          view.setBackgroundColor(Color.LTGRAY);
          vh.tag.setTextColor(Color.BLACK);
          vh.createDate.setTextColor(Color.GRAY);
          vh.duration.setTextColor(Color.BLUE);
        }
      } else {
        if (isCurrentPosition) {
          vh.playControl.setVisibility(View.VISIBLE);
          vh.tag.setTextColor(Color.WHITE);
          vh.createDate.setTextColor(Color.WHITE);
          vh.duration.setTextColor(Color.WHITE);
        } else {
          vh.playControl.setVisibility(View.INVISIBLE);
          vh.tag.setTextColor(Color.BLACK);
          vh.createDate.setTextColor(Color.GRAY);
          vh.duration.setTextColor(Color.BLUE);
          vh.playControl.setImageResource(R.drawable.list_play);
        }
        
        view.setBackgroundColor(mCurrentBgColor);
        vh.detailControl.setEnabled(true);
        vh.detailControl.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View arg0) {
            Intent intent = new Intent(mContext, MemoDetail.class);
            intent.putExtra(MemoDetail.MEMO_ID, id);
            mContext.startActivity(intent);
          }
        });

        vh.playControl.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View arg0) {
            int state = mRecorder.getState();
            if (state == Recorder.IDLE_STATE) {
              mCurrentMediaPlayer = mRecorder.createMediaPlayer(path);
              mRecorder.startPlayback();
              vh.playControl.setImageResource(R.drawable.list_pause);
            } else if (state == Recorder.PLAYER_PAUSE_STATE) {
              mRecorder.startPlayback();
              vh.playControl.setImageResource(R.drawable.list_pause);
            } else if (state == Recorder.PLAYING_STATE) {
              mRecorder.pausePlayback();
              vh.playControl.setImageResource(R.drawable.list_play);
            }

            mCurrentMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

              @Override
              public void onCompletion(MediaPlayer mp) {
                vh.playControl.setImageResource(R.drawable.list_play);
                mRecorder.stopPlayback();
              }
            });

            long next = refreshNow();
            queueNextRefresh(next);

          }
        });
      }
    }

    @Override
    public void changeCursor(Cursor cursor) {

    }

    class ViewHolder {
      ImageView playControl;
      TextView tag;
      TextView createDate;
      ImageView detailControl;
      TextView duration;
      TextView path;
      TextView id;
    }

    private void setupColumnIndices(Cursor cursor) {
      if (cursor != null) {
        mLabelIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.LABEL);
        mLabelTypeIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.LABEL_TYPE);
        mDurationIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.DURATION);
        mCreateDateIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.CREATE_DATE);
        mMemoIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos._ID);
        mPathIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.DATA);
      }
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


  }

  private void updateOperatorBtn(boolean isEnable) {
    findViewById(R.id.memo_share).setEnabled(isEnable);
    findViewById(R.id.memo_share).setClickable(isEnable);
    findViewById(R.id.memo_del).setEnabled(isEnable);
    findViewById(R.id.memo_del).setClickable(isEnable);
    mProgress.setEnabled(isEnable);
  }

  private void resetPlayer() {
    mCurrentTime.setText("0:00");
    String remain = "-" + MemosUtils.makeTimeString(this, mCurrentDuration / 1000);
    mCurrentRemain.setText(remain);
    mProgress.setProgress(0);
  }

}
