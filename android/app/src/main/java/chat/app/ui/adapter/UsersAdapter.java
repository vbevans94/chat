package chat.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.InjectView;
import chat.app.ui.tools.BaseHolder;
import thrift.entity.User;

public class UsersAdapter extends ArrayAdapter<User> {

    public UsersAdapter(Context context, List<User> dialogs) {
        super(context, android.R.layout.simple_list_item_1, dialogs);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = View.inflate(getContext(), android.R.layout.simple_list_item_1, null);
            holder = new ViewHolder(view);
        }
        User user = getItem(position);
        holder.textView1.setText(user.getUsername());

        return view;
    }

    static class ViewHolder extends BaseHolder {

        @InjectView(android.R.id.text1)
        TextView textView1;

        ViewHolder(View view) {
            super(view);
        }
    }
}
