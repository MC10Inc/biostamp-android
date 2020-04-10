package com.mc10inc.biostamp3.sdkexample;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdkexample.databinding.FragmentDownloadBinding;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class DownloadFragment extends BaseFragment {
    private FragmentDownloadBinding binding;
    private RecordingAdapter recordingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDownloadBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.recordingList.setLayoutManager(new LinearLayoutManager(getContext()));
        recordingAdapter = new RecordingAdapter();
        binding.recordingList.setAdapter(recordingAdapter);
        binding.listButton.setOnClickListener(this::listButton);
        binding.clearAllButton.setOnClickListener(this::clearAllButton);
        binding.clearOldestButton.setOnClickListener(this::clearOldestButton);
        binding.downloadButton.setOnClickListener(this::downloadButton);
        binding.cancelButton.setOnClickListener(this::cancelButton);

        viewModel.getRecordingList().observe(getViewLifecycleOwner(), recordings -> {
            updateRecordingList(recordings);
        });

        viewModel.getDownloadInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            if (inProgress) {
                binding.downloadProgressBar.setVisibility(View.VISIBLE);
                binding.cancelButton.setVisibility(View.VISIBLE);
            } else {
                binding.downloadProgressBar.setVisibility(View.INVISIBLE);
                binding.cancelButton.setVisibility(View.INVISIBLE);
            }
        });

        viewModel.getDownloadProgress().observe(getViewLifecycleOwner(), progress ->
                binding.downloadProgressBar.setProgress(
                        (int)(binding.downloadProgressBar.getMax() * progress)));

        return view;
    }

    private void listButton(View v) {
        listRecordings();
    }

    private void listRecordings() {
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

    private void clearAllButton(View v) {
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

    private void clearOldestButton(View v) {
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
                listRecordings();
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

    private void downloadButton(View v) {
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

    private void cancelButton(View v) {
        BioStamp sensor = viewModel.getSensor();
        if (sensor == null) {
            return;
        }
        sensor.cancelTask();
    }
}
