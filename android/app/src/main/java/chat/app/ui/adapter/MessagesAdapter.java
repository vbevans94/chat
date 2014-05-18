package chat.app.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.InjectView;
import chat.app.R;
import chat.app.ui.tools.BaseHolder;
import thrift.entity.Message;
import thrift.entity.User;

public class MessagesAdapter extends BaseAdapter {

    private static final int MY_MESSAGE = 0;
    private static final int HIS_MESSAGE = 1;

    private final List<Message> mMessages;
    private final Context mContext;
    private final User mMe;

    public MessagesAdapter(Context context, List<Message> messages, User me) {
        mContext = context;
        mMessages = messages;
        mMe = me;
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Message getItem(int position) {
        return mMessages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        boolean my = mMessages.get(position).getAuthor().equals(mMe);
        return my ? MY_MESSAGE : HIS_MESSAGE;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int type = getItemViewType(position);
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            int resId = 0;
            switch (type) {
                case MY_MESSAGE:
                    resId = R.layout.item_message_my;
                    break;

                case HIS_MESSAGE:
                    resId = R.layout.item_message_his;
                    break;
            }

            view = View.inflate(mContext, resId, null);
            holder = new ViewHolder(view);
        }
        Message message = getItem(position);
        if (type == HIS_MESSAGE) {
            holder.textAuthor.setText(message.getAuthor().getUsername());
        }
        holder.textData.setText(message.getData());

        return view;
    }

    static class ViewHolder extends BaseHolder {

        @InjectView(R.id.text_author)
        TextView textAuthor;

        @InjectView(R.id.text_data)
        TextView textData;

        ViewHolder(View view) {
            super(view);
        }
    }
}
