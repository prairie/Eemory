package com.prairie.eemory.util;

import java.util.Locale;

public class LocaleUtil {

    public static boolean isChina() {
        return Locale.getDefault() == Locale.CHINA;
    }

}
