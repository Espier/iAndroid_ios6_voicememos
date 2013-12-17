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

import org.espier.voicememos6.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MemoDelete extends BaseUi implements OnClickListener {

  public static final String MEMO_PATH = "memo_path";
  public static final String MEMO_ID = "memo_id";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.memos_delete);

    findViewById(R.id.memo_del_ok).setOnClickListener(this);
    findViewById(R.id.memo_del_cancel).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.memo_del_ok:
        setResult(Activity.RESULT_OK);
        finish();
        break;
      case R.id.memo_del_cancel:
        setResult(Activity.RESULT_CANCELED);
        finish();
        break;

      default:
        break;
    }

  }

}
