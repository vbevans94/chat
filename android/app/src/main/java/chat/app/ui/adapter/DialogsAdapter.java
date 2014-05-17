package chat.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.InjectView;
import chat.app.ui.tools.BaseHolder;
import thrift.entity.Dialog;

public class DialogsAdapter extends ArrayAdapter<Dialog> {

    public DialogsAdapter(Context context, List<Dialog> dialogs) {
        super(context, android.R.layout.simple_list_item_2, dialogs);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = View.inflate(getContext(), android.R.layout.simple_list_item_2, null);
            holder = new ViewHolder(view);
        }
        Dialog dialog = getItem(position);
        holder.textView1.setText(dialog.getPartner().getUsername());
        holder.textView2.setText(dialog.getLastMessage().getData());

        return view;
    }

    static class ViewHolder extends BaseHolder {

        @InjectView(android.R.id.text1)
        TextView textView1;

        @InjectView(android.R.id.text2)
        TextView textView2;

        ViewHolder(View view) {
            super(view);
        }
    }
}
