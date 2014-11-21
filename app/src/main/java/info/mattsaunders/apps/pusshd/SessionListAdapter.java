package info.mattsaunders.apps.pusshd;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Customized baseadapter for displaying SSH/SFTP session
 */
public class SessionListAdapter extends BaseAdapter {
    private Context mContext;
    private List<Object> mObjectList;
    private LayoutInflater mInflater;
    public SessionListAdapter (Context context, List<Object> objectList)
    {
        super();
        mInflater = LayoutInflater.from(context);
        mObjectList = objectList;
        mContext = context;
    }

    public int getCount()
    {
        return mObjectList.size();
    }
    public Object getItem(int position) {
        return mObjectList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position,  View convertView, ViewGroup parent)
    {
        View view;
        ViewHolder holder;

        if(convertView == null) {
            //Get the ID for R.layout.mRowLayout
            int resID = mContext.getResources().getIdentifier("user_session_row", "layout", mContext.getPackageName());
            view = mInflater.inflate(resID, parent, false);

            //view = mInflater.inflate(R.layout.row_layout, parent, false);
            holder = new ViewHolder();

            holder.user = (TextView)view.findViewById(R.id.user);
            holder.ip = (TextView) view.findViewById(R.id.ip);
            holder.port = (TextView) view.findViewById(R.id.port);
            holder.username = (TextView) view.findViewById(R.id.username);
            holder.iplabel = (TextView) view.findViewById(R.id.iplabel);
            holder.portlabel = (TextView) view.findViewById(R.id.portlabel);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder)view.getTag();
        }

        String sessionString = mObjectList.get(position).toString().substring(13);
        String[] splitString = sessionString.split("@|:");

        //Text styling and setup
        holder.username.setText("User:");
        holder.iplabel.setText("IP:");
        holder.portlabel.setText("Port:");
        try {
            holder.user.setText(splitString[0].substring(1));
            holder.ip.setText(splitString[1].substring(1));
            holder.port.setText(splitString[2].substring(0, splitString[2].length() - 1));
        } catch (ArrayIndexOutOfBoundsException ex) {
            Log.e("Failed setting label for active session: split string index out of bounds error", ex.toString());
        }
        holder.username.setTextColor(Color.parseColor("#000000"));
        holder.iplabel.setTextColor(Color.parseColor("#000000"));
        holder.portlabel.setTextColor(Color.parseColor("#000000"));
        holder.ip.setTextColor(Color.parseColor("#000000"));
        holder.port.setTextColor(Color.parseColor("#000000"));
        holder.user.setTextColor(Color.parseColor("#000000"));
        holder.username.setTextSize(15);
        holder.iplabel.setTextSize(15);
        holder.portlabel.setTextSize(15);
        holder.ip.setTextSize(15);
        holder.port.setTextSize(15);
        holder.user.setTextSize(15);

        return view;
    }
    private class ViewHolder {
        public TextView user, ip, port, username, iplabel, portlabel;
    }
}

