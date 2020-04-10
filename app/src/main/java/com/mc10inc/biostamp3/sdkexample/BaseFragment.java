package com.mc10inc.biostamp3.sdkexample;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

public class BaseFragment extends Fragment {
    protected ExampleViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(Objects.requireNonNull(getActivity())).get(ExampleViewModel.class);
    }

    protected void errorPopup(String message) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .create()
                .show();
    }
}
