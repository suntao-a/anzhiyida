package com.azyd.face.util;

import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.azyd.face.app.AppContext;

import java.util.Locale;

/**
 * @author suntao
 * @creat-time 2019/1/15 on 15:23
 * $describe$
 */
public class ChineseToSpeech {
    private TextToSpeech textToSpeech;

    public ChineseToSpeech() {
        this.textToSpeech = new TextToSpeech(AppContext.getInstance(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(AppContext.getInstance(),"不支持中文朗读功能",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void speech(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

}
