package com.mc10inc.biostamp3.sdkexample;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
}
