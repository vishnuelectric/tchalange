package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.emoji.Emoji;

import javax.inject.Inject;

public class EmojiTextView extends TextView{
    @Inject Emoji emoji;
    public EmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (emoji == null){ //setText may be called before injection
            return;
        }
        super.setText(
                emoji.replaceEmoji(text),
                type);
    }
}
