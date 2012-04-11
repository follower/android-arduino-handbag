package com.handbagdevices.handbag;

import android.view.ViewGroup;
import android.widget.ProgressBar;


public class ProgressBarWidget extends WidgetConfig {

    final static int PROGRESS_ARRAY_OFFSET_ID = 2;
    final static int PROGRESS_ARRAY_OFFSET_VALUE = 3;

    // TODO: Support setting the min/max etc?

    private int barValue = 0;


    public ProgressBarWidget(int widgetId, int barValue) {
        remoteWidgetId = widgetId;
        this.barValue = barValue;
    }


    @Override
    void displaySelf(ViewGroup parent) {

        // TODO: Ensure the widget with the matching id is actually of the correct type?
        ProgressBar bar = (ProgressBar) parent.findViewById(WIDGET_ID_OFFSET + remoteWidgetId);

        if (bar == null) {
            bar = new ProgressBar(parent.getContext(), null, android.R.attr.progressBarStyleHorizontal);
            bar.setId(WIDGET_ID_OFFSET + remoteWidgetId);

            parent.addView(bar);
        }

        bar.setProgress(this.barValue);
    }


    // @Hides
    public static WidgetConfig fromArray(String[] theArray) {
        return new ProgressBarWidget(Integer.valueOf(theArray[PROGRESS_ARRAY_OFFSET_ID]),
                Integer.valueOf(theArray[PROGRESS_ARRAY_OFFSET_VALUE]));
    }

}
