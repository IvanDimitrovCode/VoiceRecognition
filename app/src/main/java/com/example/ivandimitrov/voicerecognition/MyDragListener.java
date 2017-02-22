package com.example.ivandimitrov.voicerecognition;

import android.app.Activity;
import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.View;

/**
 * Created by Ivan Dimitrov on 2/21/2017.
 */

class MyDragListener implements View.OnDragListener {
    private Drawable       enterShape;
    private Drawable       normalShape;
    private OnDropListener mListener;

    MyDragListener(Activity activity, OnDropListener listener) {
        enterShape = activity.getResources().getDrawable(R.drawable.shape_droptarget);
        normalShape = activity.getResources().getDrawable(R.drawable.shape);
        mListener = listener;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // do nothing
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                v.setBackgroundDrawable(enterShape);
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                v.setBackgroundDrawable(normalShape);
                break;
            case DragEvent.ACTION_DROP:
                ClipData.Item item = event.getClipData().getItemAt(0);
                String itemLocation = item.getText().toString();
                mListener.onItemDropped(itemLocation);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                v.setBackgroundDrawable(normalShape);
            default:
                break;
        }
        return true;
    }

    interface OnDropListener {
        void onItemDropped(String item);
    }
}
