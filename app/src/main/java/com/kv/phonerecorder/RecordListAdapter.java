package com.kv.phonerecorder;


import static com.kv.phonerecorder.utils.Utilities.getAudioDuration;
import static com.kv.phonerecorder.utils.Utils.showToast;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;



public class RecordListAdapter extends RecyclerView.Adapter<RecordListAdapter.ViewHolder> {

    private static ClickListener clickListener;
    private Activity context;
    private ArrayList<AudioModel> audiofiles;
    private boolean longclicked;
    private LayoutInflater inflater;

    RecordListAdapter(Activity context, ArrayList<AudioModel> audiofiles) {
        this.context = context;
        this.audiofiles = audiofiles;
        this.inflater = LayoutInflater.from(context);
    }

    static void setBinder(ClickListener clickListener1) {
        clickListener = clickListener1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int i) {
        View view = inflater.inflate(R.layout.recyclerview_item, container, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {

        String file_name = audiofiles.get(i).getName();
        final String number = file_name.substring(file_name.indexOf("_", 7) + 1, file_name.lastIndexOf("_"));
        String time = file_name.substring(file_name.lastIndexOf("_") + 1, file_name.lastIndexOf("."));
        final String call_type = file_name.contains("IN") ? "IN" : "OUT";
        String duration = getAudioDuration(context, audiofiles.get(i).getLength(), audiofiles.get(i).getPath());
        final String date_time = audiofiles.get(i).getDate() + " " + time.substring(0, 2) + ":" + time.substring(2, 4);

        viewHolder.tv_number.setText("Unknown call");
        viewHolder.tv_duration.setText(duration);
        viewHolder.tv_date.setText(date_time);

        if (call_type.equals("IN")) {
            viewHolder.img_call_status.setBackground(context.getResources().getDrawable(R.drawable.call_in));
        } else {
            viewHolder.img_call_status.setBackground(context.getResources().getDrawable(R.drawable.calls_out));
        }

        viewHolder.tv_number.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("number", number));
                //showToast(context, "Copied to clipboard");
                return true;
            }
        });

        if (longclicked) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkBox.setChecked(false);
            viewHolder.checkBox.setVisibility(View.GONE);
        }

        viewHolder.viewForeground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String duration = viewHolder.tv_duration.getText().toString();
                if (longclicked) {
                    checkBoxStageChange(viewHolder);
                } else {
                    if (!duration.equals("0 sec")) {
                        int position = viewHolder.getAdapterPosition();
                        clickListener.onClick(position, number, date_time, call_type);
                    } else {
                        showToast(context, "The audio cannot be played");
                    }
                }

            }
        });

        viewHolder.viewForeground.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longclicked) {
                    checkBoxStageChange(viewHolder);
                } else {
                    longclicked = true;
                    checkBoxStageChange(viewHolder);
                    notifyDataSetChanged();
                }
                return true;
            }
        });
    }

    private void checkBoxStageChange(ViewHolder viewHolder) {
        boolean isChecked = !viewHolder.checkBox.isChecked();
        viewHolder.checkBox.setChecked(isChecked);
        clickListener.onSelect(isChecked, viewHolder.getAdapterPosition());
    }

//    @Override
//    public int getItemViewType(int position) {
//        return position;
//    }

    @Override
    public int getItemCount() {
        return audiofiles.size();
    }

    void removeItem(int positon) {
        audiofiles.remove(positon);
        notifyItemRemoved(positon);
    }

    void cancelLongClick() {
        longclicked = false;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_number;
        TextView tv_duration;
        TextView tv_date;
        ImageView img_call_status;
        CardView viewForeground;
        CheckBox checkBox;

        ViewHolder(View view) {
            super(view);

            tv_number = view.findViewById(R.id.tv_number);
            tv_duration = view.findViewById(R.id.tv_duration);
            tv_date = view.findViewById(R.id.tv_date);
            viewForeground = view.findViewById(R.id.view_foreground);
            img_call_status = view.findViewById(R.id.call_status);
            checkBox = view.findViewById(R.id.view_checkbox);
        }
    }
}
