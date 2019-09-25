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

import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.db.BioStampDb;

import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecordingsFragment extends BaseFragment {
    @BindView(R.id.recordingList)
    RecyclerView recordingList;

    private RecordingAdapter recordingAdapter;

    private final BioStampDb.RecordingUpdateListener updateListener = () -> {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        getActivity().runOnUiThread(this::refresh);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recordings, container, false);
        unbinder = ButterKnife.bind(this, view);

        recordingList.setLayoutManager(new LinearLayoutManager(getContext()));
        recordingAdapter = new RecordingAdapter();
        recordingList.setAdapter(recordingAdapter);

        viewModel.getLocalRecordingList().observe(getViewLifecycleOwner(), recordingInfos -> {
            List<RecordingAdapter.RecordingItem> items = recordingInfos.stream()
                    .map(RecordingAdapter.RecordingItem::new)
                    .collect(Collectors.toList());
            recordingAdapter.setRecordings(items);
        });

        refresh();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        BioStampManager.getInstance().getDb().addRecordingUpdateListener(updateListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        BioStampManager.getInstance().getDb().removeRecordingUpdateListener(updateListener);
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
        new DecodeRecordingTask(recordingItem.getRecordingInfo()).execute();
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

    private void refresh() {
        new GetRecordingsTask(viewModel).execute();
    }
}
