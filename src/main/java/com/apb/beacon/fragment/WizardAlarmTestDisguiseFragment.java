package com.apb.beacon.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.apb.beacon.common.AppConstants;
import com.apb.beacon.common.ApplicationSettings;
import com.apb.beacon.R;
import com.apb.beacon.WizardActivity;
import com.apb.beacon.data.PBDatabase;
import com.apb.beacon.model.Page;
import com.apb.beacon.trigger.MultiClickEvent;

/**
 * Created by aoe on 1/16/14.
 */
public class WizardAlarmTestDisguiseFragment extends Fragment {

    private static final String PAGE_ID = "page_id";
    private Activity activity;

    private int[] buttonIds = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight,
            R.id.nine, R.id.zero, R.id.equals_sign, R.id.plus, R.id.minus, R.id.multiply, R.id.divide, R.id.decimal_point, R.id.char_c};

    private Handler inactiveHandler = new Handler();
    private Handler failHandler = new Handler();

    private int lastClickId = -1;

    Page currentPage;

    public static WizardAlarmTestDisguiseFragment newInstance(String pageId) {
        WizardAlarmTestDisguiseFragment f = new WizardAlarmTestDisguiseFragment();
        Bundle args = new Bundle();
        args.putString(PAGE_ID, pageId);
        f.setArguments(args);
        return (f);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calculator_layout, container, false);
        registerButtonEvents(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity != null) {
            String pageId = getArguments().getString(PAGE_ID);
            String selectedLang = ApplicationSettings.getSelectedLanguage(activity);

            PBDatabase dbInstance = new PBDatabase(activity);
            dbInstance.open();
            currentPage = dbInstance.retrievePage(pageId, selectedLang);
            dbInstance.close();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(">>>>>", "onPause WizardAlarmTestDisguiseFragment");

        inactiveHandler.removeCallbacks(runnableInteractive);
        failHandler.removeCallbacks(runnableFailed);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(">>>>>", "onResume WizardAlarmTestDisguiseFragment");

        inactiveHandler.postDelayed(runnableInteractive, Integer.parseInt(currentPage.getTimers().getInactive()) * 1000);
        failHandler.postDelayed(runnableFailed, Integer.parseInt(currentPage.getTimers().getFail()) * 1000);
    }


    private void registerButtonEvents(View view) {
        for (int buttonId : buttonIds) {
            Button button = (Button) view.findViewById(buttonId);
            button.setOnClickListener(clickListener);
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();

            inactiveHandler.removeCallbacks(runnableInteractive);
            inactiveHandler.postDelayed(runnableInteractive, Integer.parseInt(currentPage.getTimers().getInactive()) * 1000);

            MultiClickEvent multiClickEvent = (MultiClickEvent) view.getTag();
            if (multiClickEvent == null) {
                multiClickEvent = resetEvent(view);
            }

            if (id != lastClickId) multiClickEvent.reset();
            lastClickId = id;
            multiClickEvent.registerClick(System.currentTimeMillis());
            if (multiClickEvent.isActivated()) {
                Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(AppConstants.HAPTIC_FEEDBACK_DURATION);
                resetEvent(view);

                inactiveHandler.removeCallbacks(runnableInteractive);
                failHandler.removeCallbacks(runnableFailed);

                String pageId = currentPage.getSuccessId();

                Intent i = new Intent(activity, WizardActivity.class);
                i.putExtra("page_id", pageId);
                activity.startActivity(i);
                activity.finish();
            }
        }
    };

    private MultiClickEvent resetEvent(View view) {
        MultiClickEvent multiClickEvent = new MultiClickEvent();
        view.setTag(multiClickEvent);
        return multiClickEvent;
    }


    private Runnable runnableInteractive = new Runnable() {
        public void run() {

            failHandler.removeCallbacks(runnableFailed);

            String pageId = currentPage.getFailedId();

            Intent i = new Intent(activity, WizardActivity.class);
            i.putExtra("page_id", pageId);
            activity.startActivity(i);
            activity.finish();
        }
    };


    private Runnable runnableFailed = new Runnable() {
        public void run() {

            inactiveHandler.removeCallbacks(runnableInteractive);

            String pageId = currentPage.getFailedId();

            Intent i = new Intent(activity, WizardActivity.class);
            i.putExtra("page_id", pageId);
            activity.startActivity(i);
            activity.finish();
        }
    };
}
