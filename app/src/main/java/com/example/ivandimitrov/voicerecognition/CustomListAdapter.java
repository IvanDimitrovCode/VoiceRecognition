package com.example.ivandimitrov.voicerecognition;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Ivan Dimitrov on 2/21/2017.
 */

public class CustomListAdapter extends ArrayAdapter<File> {

    private final Activity context;
    private ArrayList<File> mCommentsList = new ArrayList<>();

    public CustomListAdapter(Activity context, ArrayList<File> commentsList) {
        super(context, 0, commentsList);
        this.context = context;
        this.mCommentsList = commentsList;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final int itemIndex = position;
        ViewHolder viewHolder = null;
        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.list_item, null, true);
            viewHolder.fileName = (TextView) view.findViewById(R.id.file_name);
            viewHolder.fileName.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        ClipData.Item item = new ClipData.Item(mCommentsList.get(itemIndex).getAbsolutePath());
                        String[] clipDescription = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                        ClipData dragData = new ClipData(mCommentsList.get(itemIndex).getAbsolutePath(), clipDescription, item);
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        view.startDrag(dragData, shadowBuilder, view, 0);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.fileName.setText(mCommentsList.get(position).getName());
        return view;
    }

    static class ViewHolder {
        TextView fileName;
    }
}