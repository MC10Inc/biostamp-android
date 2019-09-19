package com.mc10inc.biostamp3.sdkexample;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class DownloadFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.listButton) void listButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getRecordingList((error, result) -> {
            if (error == null) {
                Timber.i(result.toString());
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
            if (error != null) {
                Timber.e(error);
            }
        });
    }
}
