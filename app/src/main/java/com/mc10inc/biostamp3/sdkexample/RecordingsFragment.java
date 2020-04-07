package com.mc10inc.biostamp3.sdkexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.BioStampDb;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class RecordingsFragment extends BaseFragment {
    private static final int REQUEST_CODE_SELECT_EXPORT_FILE = 0;
    private static final String SELECTED_RECORDING_KEY = "SELECTED_RECORDING";

    @BindView(R.id.recordingList)
    RecyclerView recordingList;

    private RecordingAdapter recordingAdapter;
    private RecordingInfo selectedRecordingToDecode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (savedInstanceState != null) {
            selectedRecordingToDecode = savedInstanceState.getParcelable(SELECTED_RECORDING_KEY);
        }

        recordingList.setLayoutManager(new LinearLayoutManager(getContext()));
        recordingAdapter = new RecordingAdapter();
        recordingList.setAdapter(recordingAdapter);

        BioStampManager.getInstance().getDb().getRecordingsLiveData().observe(getViewLifecycleOwner(), recordingInfos -> {
            List<RecordingAdapter.RecordingItem> items = recordingInfos.stream()
                    .map(RecordingAdapter.RecordingItem::new)
                    .collect(Collectors.toList());
            recordingAdapter.setRecordings(items);
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SELECTED_RECORDING_KEY, selectedRecordingToDecode);
    }

    @OnClick(R.id.decodeButton) void decodeButton() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        RecordingAdapter.RecordingItem recordingItem = recordingAdapter.getSelectedItem();
        if (recordingItem == null) {
            errorPopup("Please select a recording to decode");
            return;
        }
        selectedRecordingToDecode = recordingItem.getRecordingInfo();

        String defaultFileName = String.format("%s_%s.zip",
                selectedRecordingToDecode.getSerial(),
                selectedRecordingToDecode.getStartTimestampString()
                        .replace(" ", "_")
                        .replace(":", ""));

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
        startActivityForResult(intent, REQUEST_CODE_SELECT_EXPORT_FILE);
    }

    @OnClick(R.id.deleteButton) void deleteButton() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        RecordingAdapter.RecordingItem recordingItem = recordingAdapter.getSelectedItem();
        if (recordingItem == null) {
            errorPopup("Please select a recording to delete");
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to delete the selected recording?")
                .setPositiveButton("Yes", (dialogInterface, i) ->
                        BioStampManager.getInstance().getDb().deleteRecording(
                                recordingItem.getRecordingInfo()))
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @OnClick(R.id.deleteAllButton) void deleteAllButton() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to delete all recordings?")
                .setPositiveButton("Yes", (dialogInterface, i) ->
                        BioStampManager.getInstance().getDb().deleteAllRecordings())
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getActivity() == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_SELECT_EXPORT_FILE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Timber.e("No data from select export file result");
                return;
            }
            Uri uri = data.getData();
            if (uri == null) {
                Timber.e("URI is null");
                return;
            }
            if (selectedRecordingToDecode == null) {
                Timber.e("No selected recording");
                return;
            }

            OutputStream os;
            try {
                os = getActivity().getContentResolver().openOutputStream(uri);
            } catch (IOException e) {
                Timber.e(e);
                return;
            }
            Timber.i("Exporting zip to %s", uri);
            new DecodeRecordingTask(selectedRecordingToDecode, os).execute();
        }
    }
}
