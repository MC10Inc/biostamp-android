package com.mc10inc.biostamp3.sdkexample;

import androidx.fragment.app.Fragment;

import butterknife.Unbinder;

public class BaseFragment extends Fragment {
    protected Unbinder unbinder;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
