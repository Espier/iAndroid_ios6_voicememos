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

import org.espier.voicememos.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class LabelCustom extends BaseUi implements OnClickListener{
    public static final String CUSTOM_LABEL_NAME = "custom_label_name";
    
    private EditText mLabelText;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.label_custom);
        
        mLabelText = (EditText)findViewById(R.id.label_edit);
        
        String oldName = getIntent().getStringExtra(LabelSelect.LABEL_NAME);
        if(!TextUtils.isEmpty(oldName)){
            mLabelText.setText(oldName);
            mLabelText.setSelection(oldName.length());
        }
        
        findViewById(R.id.clear_label).setOnClickListener(this);
        findViewById(R.id.label_finish).setOnClickListener(this);
        findViewById(R.id.label_custom_back).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.clear_label:
            mLabelText.setText("");
            break;
        case R.id.label_finish:
            String labelName = mLabelText.getText().toString();
            if(!TextUtils.isEmpty(labelName)){
                Intent data = new Intent();
                data.putExtra(CUSTOM_LABEL_NAME, labelName);
                setResult(Activity.RESULT_OK, data);
            }
            finish();
            break;
        case R.id.label_custom_back:
            finish();
            break;
        default:
            break;
        }
        
    }

}
