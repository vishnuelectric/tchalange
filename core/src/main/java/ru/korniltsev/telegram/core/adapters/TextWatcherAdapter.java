package ru.korniltsev.telegram.core.adapters;

import android.text.Editable;
import android.text.TextWatcher;

public class TextWatcherAdapter implements TextWatcher {
//    protected boolean handleTextChange = true;
//    protected void changeText(Runnable r) {
//        handleTextChange = false;
//        r.run();
//        handleTextChange = true;
//    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
