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

import java.util.Formatter;
import java.util.Locale;

import org.espier.voicememos6.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MemosUtils {
  private static StringBuilder sFormatBuilder = new StringBuilder();
  private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
  private static final Object[] sTimeArgs = new Object[5];

  public static String makeTimeString(Context context, long secs) {
    String durationformat =
        context.getString(secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);

    /*
     * Provide multiple arguments so the format can be changed easily by modifying the xml.
     */
    sFormatBuilder.setLength(0);

    final Object[] timeArgs = sTimeArgs;
    timeArgs[0] = secs / 3600;
    timeArgs[1] = secs / 60;
    timeArgs[2] = (secs / 60) % 60;
    timeArgs[3] = secs;
    timeArgs[4] = secs % 60;

    return sFormatter.format(durationformat, timeArgs).toString();
  }

  public static void shareMemo(Context context, String path) {
    Uri uri = Uri.parse("file:///" + path);
    Intent it = new Intent(Intent.ACTION_SEND);
    it.putExtra(Intent.EXTRA_STREAM, uri);
    it.setType("audio/*");
    context.startActivity(Intent.createChooser(it, context.getString(R.string.list_share_title)));
  }


}
