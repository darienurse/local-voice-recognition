/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {

    private File assetsDir;

    public enum Language{
        ENGLISH, SPANISH
    }
    private static final String KWS_SEARCH = "wakeup";
    private static final String SPAN_NUM_SEARCH = "espanol";
    private static final String ENG_NUM_SEARCH = "english";
    private static final String ENG_POLAR_SEARCH = "polar";
    private static final String SPAN_POLAR_SEARCH = "el polar";
    private static final String LANG_SEARCH = "menu";
    private static final String KEYPHRASE = "cycle count";
    private boolean countQuery = false;
    private Language currentLang = Language.ENGLISH;

    private SpeechRecognizer recognizer;
    private SpeechRecognizer english_recognizer;
    private SpeechRecognizer spanish_recognizer;

    private HashMap<String, Integer> captions;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(LANG_SEARCH, R.string.lang_caption);
        captions.put(ENG_NUM_SEARCH, R.string.eng_num_caption);
        captions.put(SPAN_NUM_SEARCH, R.string.span_num_caption);
        captions.put(ENG_POLAR_SEARCH, R.string.eng_polar_caption);
        captions.put(SPAN_POLAR_SEARCH, R.string.span_polar_caption);
        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    assetsDir = assets.syncAssets();
                    setupRecognizer(assetsDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH,"");
                }
            }
        }.execute();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(LANG_SEARCH,"");
        else if (text.equals(ENG_NUM_SEARCH)) {
            switchSearch(ENG_NUM_SEARCH,"");
        }
        else if (text.equals(SPAN_NUM_SEARCH)) {
            recognizer.stop();
            recognizer = spanish_recognizer;
            switchSearch(SPAN_NUM_SEARCH,"");
        }
        else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        String result = ((TextView) findViewById(R.id.result_text)).getText().toString();
        if(ENG_NUM_SEARCH.equals(recognizer.getSearchName())){
            switchSearch(ENG_POLAR_SEARCH, result);
        }
        else if(SPAN_NUM_SEARCH.equals(recognizer.getSearchName())){
            switchSearch(SPAN_POLAR_SEARCH, result);
        }
        else if (ENG_POLAR_SEARCH.equals(recognizer.getSearchName())
                || SPAN_POLAR_SEARCH.equals(recognizer.getSearchName())){
            recognizer.stop();
            recognizer = english_recognizer;
            switchSearch(KWS_SEARCH,"");
        }
    }

    private void switchSearch(String searchName, String result) {
        recognizer.stop();
        recognizer.startListening(searchName);
        String caption = getResources().getString(captions.get(searchName));
        if(!result.isEmpty()) caption = result + "\n" + caption;
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        english_recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        spanish_recognizer = defaultSetup()
//                .setAcousticModel(new File(modelsDir, "es_MX_broadcast/model_parameters/hub4_spanish_itesm.cd_cont_2500))
//                .setDictionary(new File(modelsDir, "es_MX_broadcast/etc_mx/h4.dict"))
                .setAcousticModel(new File(modelsDir, "voxforge-es-0.1.1/model_parameters/voxforge_es_sphinx.cd_cont_1500"))
                .setDictionary(new File(modelsDir, "voxforge-es-0.1.1/etc/voxforge_es_sphinx.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();

        // Create keyword-activation search.
        english_recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based searches.
        File menuGrammar = new File(modelsDir, "grammar/menu.gram");
        english_recognizer.addGrammarSearch(LANG_SEARCH, menuGrammar);

        File numbersGrammar = new File(modelsDir, "grammar/numbers.gram");
        english_recognizer.addGrammarSearch(ENG_NUM_SEARCH, numbersGrammar);
        File spanNumbersGrammar = new File(modelsDir, "grammar/numeros.gram");
        spanish_recognizer.addGrammarSearch(SPAN_NUM_SEARCH, spanNumbersGrammar);

        File yesNoGrammar = new File(modelsDir, "grammar/polar.gram");
        english_recognizer.addGrammarSearch(ENG_POLAR_SEARCH, yesNoGrammar);
        File siNoGrammar = new File(modelsDir, "grammar/span_polar.gram");
        spanish_recognizer.addGrammarSearch(SPAN_POLAR_SEARCH, siNoGrammar);

        english_recognizer.addListener(this);
        spanish_recognizer.addListener(this);
        recognizer = english_recognizer;
    }
}
