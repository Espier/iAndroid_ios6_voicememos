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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.espier.voicememos.R;
import org.espier.voicememos.model.VoiceMemo;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class LabelSelect extends BaseUi implements OnItemClickListener {

  private static final int CUSTOM_LABEL_INDEX = 7;
  private static final int CUSTOM_LABEL_INTENT = 1;
  public static final String LABEL_TYPE = "label_type";
  public static final String LABEL_NAME = "label_name";
  public static final String LABEL_NEW = "label_new";

  private int mCurrentLabelType = 0;
  private String[] mLabelList;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.label_select);

    ListView labelListView = (ListView) findViewById(R.id.label_list);
    LabeldAdapter labelAdapter = new LabeldAdapter(this);
    labelListView.setAdapter(labelAdapter);
    labelListView.setOnItemClickListener(this);

    mCurrentLabelType = getIntent().getIntExtra(LABEL_TYPE, 0);

    findViewById(R.id.label_select_back).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        finish();
      }
    });

  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (position != CUSTOM_LABEL_INDEX) {
      String labelName = mLabelList[position];
      int labelType = position;
      // if select no label, set create time as the memo label
      if (labelType == MemoMain.LABEL_TYPE_NONE) {
        long createDate = getIntent().getLongExtra(MemoDetail.CREATE_DATE, -1);
        if (createDate != -1) {
          String dateFormat = getString(R.string.time_format);
          SimpleDateFormat format = new SimpleDateFormat(dateFormat);
          Date d = new Date(createDate);
          labelName = format.format(d);
        }
      }
      updateVoiceMemo(labelType, labelName);
      Intent data = new Intent();
      data.putExtra(LABEL_NEW, labelName);
      data.putExtra(LABEL_TYPE, labelType);
      setResult(Activity.RESULT_OK, data);
      finish();
    } else {
      Intent customIntent = new Intent(LabelSelect.this, LabelCustom.class);
      if (mCurrentLabelType == CUSTOM_LABEL_INDEX) {
        String labelName = getIntent().getStringExtra(LABEL_NAME);
        customIntent.putExtra(LABEL_NAME, labelName);
      }
      startActivityForResult(customIntent, CUSTOM_LABEL_INTENT);

    }

  }

  private void updateVoiceMemo(int labelType, String labelName) {
    ContentValues cv = new ContentValues();

    cv.put(VoiceMemo.Memos.LABEL_TYPE, labelType);
    cv.put(VoiceMemo.Memos.LABEL, labelName);

    int memoId = getIntent().getIntExtra(MemoDetail.MEMO_ID, -1);
    if (memoId != -1) {
      Uri memoUri = ContentUris.withAppendedId(VoiceMemo.Memos.CONTENT_URI, memoId);
      getContentResolver().update(memoUri, cv, null, null);
    }

  }

  private class LabeldAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    public LabeldAdapter(Context context) {
      mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      mLabelList = getResources().getStringArray(R.array.labels);

    }

    /**
     * Since the data comes from an array, just returning the index is sufficent to get at the data.
     * If we were using a more complex data structure, we would return whatever object represents
     * one row in the list.
     * 
     * @see android.widget.ListAdapter#getItem(int)
     */
    public Object getItem(int position) {
      return position;
    }

    /**
     * Use the array index as a unique id.
     * 
     * @see android.widget.ListAdapter#getItemId(int)
     */
    public long getItemId(int position) {
      return position;
    }

    /**
     * Make a view to hold each row.
     * 
     * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {

      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.label_item, null);
        TextView tv = (TextView) convertView.findViewById(R.id.label_name);
        tv.setText(mLabelList[position]);
        ImageView iv = (ImageView) convertView.findViewById(R.id.label_check);

        if (position == CUSTOM_LABEL_INDEX) {
          iv.setImageResource(R.drawable.arrow_left);
          iv.setVisibility(View.VISIBLE);
        } else if (mCurrentLabelType == position) {
          iv.setVisibility(View.VISIBLE);
        }
      }

      return convertView;
    }

    @Override
    public int getCount() {
      // TODO Auto-generated method stub
      return mLabelList.length;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO Auto-generated method stub
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == CUSTOM_LABEL_INTENT) {
        Intent newLabel = new Intent();
        String labelName = data.getStringExtra(LabelCustom.CUSTOM_LABEL_NAME);
        newLabel.putExtra(LABEL_NEW, labelName);
        newLabel.putExtra(LABEL_TYPE, CUSTOM_LABEL_INDEX);
        updateVoiceMemo(CUSTOM_LABEL_INDEX, labelName);
        setResult(Activity.RESULT_OK, newLabel);
        finish();
      }
    }
  }

}
