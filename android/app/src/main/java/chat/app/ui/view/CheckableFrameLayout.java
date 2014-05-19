package chat.app.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckableFrameLayout extends FrameLayout implements Checkable {

    private boolean mChecked = false;

    int[] CHECKED_STATE = new int[]{
            android.R.attr.state_checked
    };

    public CheckableFrameLayout(Context context) {
        super(context, null);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    @Override
    public void setChecked(boolean b) {
        if (mChecked != b) {
            mChecked = b;
            refreshDrawableState();
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] baseState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            return mergeDrawableStates(baseState, CHECKED_STATE);
        }
        return baseState;
    }
}