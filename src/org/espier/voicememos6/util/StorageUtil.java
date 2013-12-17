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
package org.espier.voicememos6.util;

import android.os.Environment;
import android.os.StatFs;

public class StorageUtil {

  public static boolean hasDiskSpace() {
    StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
    return fs.getAvailableBlocks() > 1;
  }

  public static boolean isStorageMounted() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

  }
}
