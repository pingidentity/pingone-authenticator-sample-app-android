package com.pingidentity.authenticatorsampleapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pingidentity.authenticatorsampleapp.R;

import java.util.ArrayList;

public class SideMenuAdapter extends ArrayAdapter<MenuItem> {

    private ArrayList<MenuItem> items;
    public SideMenuAdapter(Context context, ArrayList<MenuItem> menuItems){
        super(context, R.layout.row_side_menu, menuItems);
        this.items = menuItems;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public MenuItem getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MenuItem item = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_side_menu, parent, false);
            viewHolder.menuItemTitle = convertView.findViewById(R.id.text_view_menu);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        if(item!=null && item.getTitle()!=null ) {
            viewHolder.menuItemTitle.setText(item.getTitle());
        }

        // Return the completed view to render on screen
        return convertView;
    }

    private static class ViewHolder {
        TextView menuItemTitle;
    }
}
