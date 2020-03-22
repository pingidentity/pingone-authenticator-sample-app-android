package com.pingidentity.authenticatorsampleapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.models.User;

import org.jose4j.lang.StringUtil;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UsersAdapter extends ArrayAdapter<User> {

    private Context context;
    private AdapterSaveCallback callback;

    private String lastAddedUserId;
    /*
     * lock is used to avoid simultaneous editing of more than one row
     */
    private final ReentrantLock lock = new ReentrantLock();

    public UsersAdapter(Context context, ArrayList<User> users, AdapterSaveCallback callback){
        super(context, R.layout.row_user, users);
        this.context = context;
        this.callback = callback;
    }

    public interface AdapterSaveCallback {
        void onSave(User user);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final User user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        final ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_user, parent, false);
            viewHolder.rowLayout = convertView.findViewById(R.id.row_layout);
            viewHolder.editableLayout = convertView.findViewById(R.id.editable_row_layout);
            viewHolder.given = convertView.findViewById(R.id.given);
            viewHolder.givenEditable = convertView.findViewById(R.id.given_editable);
            viewHolder.family = convertView.findViewById(R.id.family);
            viewHolder.imageButtonEdit = convertView.findViewById(R.id.button_edit);
            viewHolder.imageButtonClear = convertView.findViewById(R.id.button_clear_editable);
            viewHolder.imageButtonSave = convertView.findViewById(R.id.button_save_editable);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        if(user!=null && user.getUsername()!=null && user.getUsername().getGiven()!=null && !user.getUsername().getGiven().isEmpty()) {
            viewHolder.given.setText(user.getUsername().getGiven());
            //simplest method to open edit mode with cursor position at the end
            viewHolder.givenEditable.setText(null);
            viewHolder.givenEditable.append(user.getUsername().getGiven());
            viewHolder.family.setText("");
        } else {
            viewHolder.given.setText(context.getResources().getString(R.string.username_placeholder));
            viewHolder.family.setText(position+1>=10?String.format("%s", position+1):String.format("0%s", position+1));
        }
        if(user!=null && user.getId().equals(lastAddedUserId)){
            showEditLayoutForRow(viewHolder);
        }

        viewHolder.imageButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditLayoutForRow(viewHolder);
            }
        });

        viewHolder.imageButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideEditLayoutForRow(user, viewHolder);
            }
        });

        viewHolder.imageButtonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.givenEditable.setText("");
            }
        });
        // Return the completed view to render on screen
        return convertView;
    }

    private void showEditLayoutForRow(ViewHolder viewHolder){
        /*
         * Acquires the lock only if it is not held by another thread at the time of invocation.
         */
        if (lock.getHoldCount()==0 && lock.tryLock()) {
            viewHolder.rowLayout.setVisibility(View.GONE);
            viewHolder.editableLayout.setVisibility(View.VISIBLE);
            /*
             * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns
             * false), or if it can't be focused due to other conditions (not focusable in touch mode
             * ({@link #isFocusableInTouchMode}) while the device is in touch mode, not visible, not
             * enabled, or has no size).
             */
            viewHolder.givenEditable.requestFocus(View.FOCUS_FORWARD);
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(viewHolder.givenEditable, InputMethodManager.SHOW_FORCED);
            }
        }
    }

    private void hideEditLayoutForRow(@Nullable User user, ViewHolder viewHolder){
        lock.unlock();
        if (user!=null && user.getUsername()!=null) {
            if (viewHolder.givenEditable.getText().length()==0){
                //user saves an empty string
                viewHolder.givenEditable.setText(user.getNickname());
            }
            user.getUsername().setGiven(viewHolder.givenEditable.getText().toString());
            callback.onSave(user);
        }
        lastAddedUserId = null;
        notifyDataSetInvalidated();
        viewHolder.givenEditable.clearFocus();
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm!=null) {
            imm.hideSoftInputFromWindow(viewHolder.givenEditable.getWindowToken(), 0);
        }
        viewHolder.editableLayout.setVisibility(View.GONE);
        viewHolder.rowLayout.setVisibility(View.VISIBLE);
    }

    private static class ViewHolder {
        LinearLayout editableLayout;
        LinearLayout rowLayout;
        TextView given;

        EditText givenEditable;
        TextView family;
        ImageButton imageButtonEdit;
        ImageButton imageButtonSave;
        ImageButton imageButtonClear;
    }

    public void notifyDataSetChanged(String userId) {
        this.lastAddedUserId = userId;
    }
}

