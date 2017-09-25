package io.github.lionisaqt.pickupboo;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * Created by Ryan on 9/19/2017.
 */

// An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits
public class ActionEditText extends AppCompatEditText {
    public ActionEditText(Context context) {
        super(context);
    }

    public ActionEditText (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActionEditText (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection (EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return conn;
    }
}
