package com.mc10inc.biostamp3.sdkexample;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class DownloadFragment extends BaseFragment {
    @BindView(R.id.recordingList)
    RecyclerView recordingList;

    @BindView(R.id.downloadProgressBar)
    ProgressBar progressBar;

    @BindView(R.id.cancelButton)
    Button cancelButton;

    private RecordingAdapter recordingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        unbinder = ButterKnife.bind(this, view);

        recordingList.setLayoutManager(new LinearLayoutManager(getContext()));
        recordingAdapter = new RecordingAdapter();
        recordingList.setAdapter(recordingAdapter);

        viewModel.getRecordingList().observe(getViewLifecycleOwner(), recordings -> {
            updateRecordingList(recordings);
        });

        viewModel.getDownloadInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            if (inProgress) {
                progressBar.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
            }
        });

        viewModel.getDownloadProgress().observe(getViewLifecycleOwner(), progress ->
                progressBar.setProgress((int)(progressBar.getMax() * progress)));

        return view;
    }

    @OnClick(R.id.listButton) void listButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getRecordingList((error, result) -> {
            if (error == null) {
                viewModel.setRecordingList(result);
            } else {
                Timber.e(error);
            }
        });
    }

    @OnClick(R.id.clearAllButton) void clearAllButton() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to clear all recordings?")
                .setPositiveButton("Yes", (dialogInterface, i) -> clearAllRecordings())
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @OnClick(R.id.clearOldestButton) void clearOldestButton() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to clear the oldest recording?")
                .setPositiveButton("Yes", (dialogInterface, i) -> clearOldestRecording())
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    private void clearAllRecordings() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.clearAllRecordings((error, result) -> {
            if (error == null) {
                viewModel.setRecordingList(Collections.emptyList());
            } else {
                Timber.e(error);
            }
        });
    }

    private void clearOldestRecording() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.clearOldestRecording((error, result) -> {
            if (error == null) {
                // Update the list now
                viewModel.setRecordingList(Collections.emptyList());
                listButton();
            } else {
                Timber.e(error);
            }
        });
    }

    private void updateRecordingList(List<RecordingInfo> recordings) {
        if (recordings == null) {
            recordingAdapter.setRecordings(Collections.emptyList());
        } else {
            List<RecordingAdapter.RecordingItem> items = recordings
                    .stream()
                    .map(r -> new RecordingAdapter.RecordingItem(r))
                    .collect(Collectors.toList());
            recordingAdapter.setRecordings(items);
        }
    }

    @OnClick(R.id.downloadButton) void downloadButton() {
        BioStamp sensor = viewModel.getSensor();
        if (sensor == null) {
            return;
        }
        RecordingAdapter.RecordingItem item = recordingAdapter.getSelectedItem();
        if (item == null) {
            errorPopup("Please select a recording to download");
            return;
        }
        if (item.getRecordingInfo().isInProgress()) {
            errorPopup("The selected recording is in progress; stop it before downloading.");
            return;
        }
        sensor.downloadRecording(item.getRecordingInfo(), (error, result) -> {
            viewModel.setDownloadInProgress(false);
            if (error != null) {
                Timber.e(error);
            }
        }, progress -> viewModel.setDownloadProgress(progress));
        viewModel.setDownloadInProgress(true);
    }

    @OnClick(R.id.cancelButton) void cancelButton() {
        BioStamp sensor = viewModel.getSensor();
        if (sensor == null) {
            return;
        }
        sensor.cancelTask();
    }
}
