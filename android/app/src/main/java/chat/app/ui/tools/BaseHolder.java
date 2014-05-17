package chat.app.ui.tools;

import android.view.View;

import butterknife.ButterKnife;

public class BaseHolder {
    public BaseHolder(View view) {
        ButterKnife.inject(this, view);

        view.setTag(this);
    }
}