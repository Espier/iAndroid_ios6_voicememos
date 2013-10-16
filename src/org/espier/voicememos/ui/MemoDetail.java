/*
 * Copyright (C) 2013 robin.pei(webfanren@gmail.com)
 * 
 * The code is developed under sponsor from
 * Beijing FMSoft Tech. Co. Ltd(http://www.fmsoft.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.espier.voicememos.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.espier.voicememos.R;
import org.espier.voicememos.model.VoiceMemo;
import org.espier.voicememos.util.MemosUtils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MemoDetail extends BaseUi implements OnClickListener{
    private static final String TAG = "MemoDetail";

    public static final String MEMO_PATH = "memo_path";
    public static final String MEMO_DURATION = "memo_duration";
    public static final String MEMO_ID = "memo_id";
    public static final String CREATE_DATE = "creat_date";
    
    private static final int REQUEST_LABEL_SELECT = 1;
    private static final int REQUEST_EDIT_MEMO = 2;
    
    private int mLabelType = 0;
    private String mLabelName = null;
    private String mMemoFilePath = null;
    private int mMemoId = 0;
    private long mCreateDate;
    
    private TextView mLabelView;
    private TextView mDurationView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memos_detail);
        
        findViewById(R.id.detail_trim_memo).setOnClickListener(this);
        findViewById(R.id.detail_share_memo).setOnClickListener(this);
        findViewById(R.id.detail_label_select).setOnClickListener(this);
        findViewById(R.id.detail_back).setOnClickListener(this);
        
        mLabelView = (TextView)findViewById(R.id.detail_label);
        mDurationView = (TextView)findViewById(R.id.detail_duration);
        TextView createDateView = (TextView)findViewById(R.id.detail_create_date);
        
        mMemoId = getIntent().getIntExtra(MEMO_ID, -1);
        Uri memoUri = ContentUris.withAppendedId(
                VoiceMemo.Memos.CONTENT_URI, mMemoId);
        Cursor cursor = getContentResolver().query(memoUri, null, null, null, null);
        if(cursor != null){
            int labelIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.LABEL);
            int labelTypeIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.LABEL_TYPE);
            int durationIdx = cursor
                    .getColumnIndexOrThrow(VoiceMemo.Memos.DURATION);
            int createDateIdx = cursor
                    .getColumnIndexOrThrow(VoiceMemo.Memos.CREATE_DATE);
            int pathIdx = cursor.getColumnIndexOrThrow(VoiceMemo.Memos.DATA);
            if(cursor.moveToFirst()){
                mLabelName = cursor.getString(labelIdx);
                mLabelType = cursor.getInt(labelTypeIdx);
                mMemoFilePath  = cursor.getString(pathIdx);
                String durarion = MemosUtils.makeTimeString(this, cursor.getInt(durationIdx)/1000);
                mCreateDate = cursor.getLong(createDateIdx);
                String  dateFormat = getString(R.string.detail_date_time_format);
                if(mLabelType == MemoMain.LABEL_TYPE_NONE){
                    dateFormat = getString(R.string.detail_date_format);
                }
                SimpleDateFormat format= new SimpleDateFormat(dateFormat);
                Date d =new Date(mCreateDate);
                String dd =format.format(d);
                
                mLabelView.setText(mLabelName);
                mDurationView.setText(durarion);
                createDateView.setText(dd);
            }
            cursor.close();
        }

        
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.detail_trim_memo:
            Intent trimIntent = new Intent(this, MemoEdit.class);
            trimIntent.putExtra(MEMO_ID, mMemoId);
            trimIntent.putExtra(MEMO_PATH, mMemoFilePath);
            startActivityForResult(trimIntent, REQUEST_EDIT_MEMO);
            break;
            
        case R.id.detail_share_memo:
            MemosUtils.shareMemo(this,mMemoFilePath);
            break;

        case R.id.detail_label_select:
            Intent labelIntent = new Intent(this, LabelSelect.class);
            labelIntent.putExtra(LabelSelect.LABEL_TYPE, mLabelType);
            labelIntent.putExtra(LabelSelect.LABEL_NAME, mLabelName);
            labelIntent.putExtra(CREATE_DATE, mCreateDate);
            labelIntent.putExtra(MEMO_ID, mMemoId);
            startActivityForResult(labelIntent, REQUEST_LABEL_SELECT);
            break;
        case R.id.detail_back:
            finish();
            break;
        default:
            break;
        }
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
            case REQUEST_LABEL_SELECT:
                mLabelView.setText(data.getStringExtra(LabelSelect.LABEL_NEW));
                mLabelType = data.getIntExtra(LabelSelect.LABEL_TYPE, 0);
                break;
            case REQUEST_EDIT_MEMO:
                mMemoFilePath = data.getStringExtra(MEMO_PATH);
                int newDuration = data.getIntExtra(MEMO_DURATION, -1);
                if(newDuration != -1){
                    String durarion = MemosUtils.makeTimeString(this, newDuration/1000);
                    mDurationView.setText(durarion);
                }
               
                break;

            default:
                break;
            }
            
        }
    }

  

}