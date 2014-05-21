package chat.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.InjectView;
import chat.app.R;
import chat.app.ui.tools.BaseHolder;
import thrift.entity.Dialog;
import thrift.entity.User;

public class DialogsAdapter extends ArrayAdapter<Dialog> {

    private final User mMe;

    public DialogsAdapter(Context context, List<Dialog> dialogs, User me) {
        super(context, R.layout.item_dialog, dialogs);
        mMe = me;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = View.inflate(getContext(), R.layout.item_dialog, null);
            holder = new ViewHolder(view);
        }
        Dialog dialog = getItem(position);
        holder.textPartner.setText(dialog.getPartner().getUsername());
        String username = dialog.getLastMessage().getAuthor().getUsername();
        if (username.equals(mMe.getUsername())) {
            username = getContext().getString(R.string.title_me);
        }
        holder.textLastMessage.setText(getContext().getString(R.string.message_last, username, dialog.getLastMessage().getData()));

        return view;
    }

    static class ViewHolder extends BaseHolder {

        @InjectView(R.id.text_partner)
        TextView textPartner;

        @InjectView(R.id.text_last_message)
        TextView textLastMessage;

        ViewHolder(View view) {
            super(view);
        }
    }
}
