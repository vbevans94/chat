package chat.app.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import chat.app.R;

public class ConfirmFragment extends DialogFragment {

    public static final String TAG_CONFIRM_FRAGMENT = "tag_confirm_fragment";
    public static final String ARG_MESSAGE = "arg_message";
    private OnClickListener mListener;

    public static ConfirmFragment show(String message, FragmentManager fm) {
        ConfirmFragment fragment = new ConfirmFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);

        fragment.show(fm, TAG_CONFIRM_FRAGMENT);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnClickListener) {
            mListener = (OnClickListener) activity;
        } else {
            throw new ClassCastException("Parent activity must implement dialoginterface.onclicklistener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(getArguments().getString(ARG_MESSAGE))
                .setTitle(R.string.title_confirm)
                .setPositiveButton(android.R.string.yes, mListener)
                .setNegativeButton(android.R.string.no, mListener)
                .create();
    }
}
