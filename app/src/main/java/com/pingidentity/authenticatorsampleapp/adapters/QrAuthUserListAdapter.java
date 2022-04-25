package com.pingidentity.authenticatorsampleapp.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.models.QrAuthUserModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class QrAuthUserListAdapter extends ArrayAdapter<QrAuthUserModel> {

    private final Context context;

    private int checkedPosition = -1;
    private ToggleButton selected = null;
    private UserSelectCallback callback;
    private ArrayList users;

    public QrAuthUserListAdapter(Context context, ArrayList<QrAuthUserModel> users, UserSelectCallback callback){
        super(context, R.layout.row_user_auth_approval, users);
        this.context = context;
        this.users = users;
        this.callback = callback;
    }

    public interface UserSelectCallback {
        void onUserSelected();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final QrAuthUserModel user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        final ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_user_auth_approval, parent, false);
            viewHolder.rowLayout = convertView.findViewById(R.id.layout_row_qr_auth);
            viewHolder.name = convertView.findViewById(R.id.text_view_user_name);
            viewHolder.family = convertView.findViewById(R.id.text_view_user_family);
            viewHolder.imageButtonSelect = convertView.findViewById(R.id.button_select_user);
            //if there is only one user hide selection button
            if (users.size()==1){
                viewHolder.imageButtonSelect.setVisibility(View.GONE);
            }else{
                viewHolder.imageButtonSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selected!=null){
                            selected.setChecked(false);
                        }
                        callback.onUserSelected();
                        viewHolder.imageButtonSelect.setChecked(true);
                        selected = viewHolder.imageButtonSelect;
                        checkedPosition = position;
                    }
                });
            }
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        if(user!=null && user.getFullName().getGiven()!=null) {
            viewHolder.name.setText(user.getFullName().getGiven());
        }else if(user!=null && user.getFullName().getFamily()!=null){
            viewHolder.family.setText(user.getFullName().getFamily());
        }else{
            viewHolder.name.setText(context.getResources().getString(R.string.username_placeholder));
            viewHolder.family.setText(position+1>=10?String.format("%s", position+1):String.format("0%s", position+1));
        }
        viewHolder.rowLayout.setOnClickListener(v -> viewHolder.imageButtonSelect.callOnClick());

        // Return the completed view to render on screen
        return convertView;
    }

    private static class ViewHolder {
        RelativeLayout rowLayout;
        TextView name;
        TextView family;
        TextView nickname;
        ToggleButton imageButtonSelect;
    }

    public int getSelectedPosition() {
        return checkedPosition;
    }
}


