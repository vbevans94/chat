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

public class DialogsAdapter extends ArrayAdapter<Dialog> {

    public DialogsAdapter(Context context, List<Dialog> dialogs) {
        super(context, R.layout.item_dialog, dialogs);
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
        holder.textLastMessage.setText(dialog.getLastMessage().getData());

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
