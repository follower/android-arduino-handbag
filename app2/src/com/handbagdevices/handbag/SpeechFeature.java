package com.handbagdevices.handbag;

import android.util.Log;
import android.content.Context;
import android.speech.tts.*;

// TODO: Handle situation where the TTS support isn't fully installed.

public class SpeechFeature extends FeatureConfig implements TextToSpeech.OnInitListener {

    final static int SPEECH_ARRAY_OFFSET_ACTION = 2;
    final static int SPEECH_ARRAY_OFFSET_TEXT = 3;
    final static int SPEECH_ARRAY_OFFSET_PITCH = 4;
    final static int SPEECH_ARRAY_OFFSET_RATE = 5;

    private String action;
    private String text;

    private float pitch;
    private float speechRate;

    private TextToSpeech tts;

    private boolean initOk = false;


    // TODO: Do something when the application is destroyed to shutdown TTS engine...

    public SpeechFeature(Context context, String action, String text, float pitch, float speechRate) {
        this.action = action;
        this.text = text;

        this.pitch = pitch;
        this.speechRate = speechRate;

        tts = new TextToSpeech(context, this);
    }


    @Override
    void doAction() {
        // TODO: Check that the TTS system is ready (via the status given to `onInit()`).
        if (!initOk) {
            return; // Note: This relies on `onInit()` calling `doAction()` once it's ready.
        }

        Log.d(this.getClass().getSimpleName(), "Speech action: " + action + " Text: " + text);

        if (action.equals("speak")) {
            // TODO: Have a different "use default" value?
            if (pitch != 1.0) {
                tts.setPitch(pitch);
            }
            if (speechRate != 1.0) {
                tts.setSpeechRate(speechRate);
            }

            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        } else {
            Log.d(this.getClass().getSimpleName(), "Unknown speech action: " + action);
        }

    }


    public static FeatureConfig fromArray(Context context, String[] theArray) {
        return new SpeechFeature(context, theArray[SPEECH_ARRAY_OFFSET_ACTION], theArray[SPEECH_ARRAY_OFFSET_TEXT],
                Float.valueOf(theArray[SPEECH_ARRAY_OFFSET_PITCH]), Float.valueOf(theArray[SPEECH_ARRAY_OFFSET_RATE]));
    }


    public void onInit(int status) {
        Log.d(this.getClass().getSimpleName(), "Speech init status: " + status);

        initOk = (status == TextToSpeech.SUCCESS);

        if (initOk) {
            doAction();
        }
    }
}
