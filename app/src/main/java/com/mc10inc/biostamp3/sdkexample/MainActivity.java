package com.mc10inc.biostamp3.sdkexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdkexample.databinding.ActivityMainBinding;
import com.mc10inc.biostamp3.sdkexample.streaming.StreamingFragment;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_FOR_BLE = 1;

    private ExampleViewModel viewModel;

    private ActivityMainBinding binding;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);

        binding.pager.setAdapter(new PagesAdapter(getSupportFragmentManager()));

        binding.selectedSensorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                viewModel.setSelectedSensor((String)binding.selectedSensorSpinner.getItemAtPosition(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                viewModel.setSelectedSensor(null);
            }
        });

        BioStampManager.getInstance().getBioStampsLiveData().observe(this, sensors -> {
            List<String> connectedSensors = sensors.values().stream()
                    .filter(s -> s.getState() == BioStamp.State.CONNECTED)
                    .map(BioStamp::getSerial)
                    .sorted()
                    .collect(Collectors.toList());
            String previousSelection = (String)binding.selectedSensorSpinner.getSelectedItem();
            binding.selectedSensorSpinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, connectedSensors));
            if (previousSelection != null && connectedSensors.contains(previousSelection)) {
                binding.selectedSensorSpinner.setSelection(
                        connectedSensors.indexOf(previousSelection));
            }
            if (connectedSensors.isEmpty()) {
                viewModel.setSelectedSensor(null);
            }
        });

        BioStampManager.getInstance().getThroughput().observe(this, bps -> {
            if (bps == 0) {
                binding.throughputText.setText("");
            } else {
                binding.throughputText.setText(String.format("%dbps", bps));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menu_item:
                showAboutPopup();
                return true;
            case R.id.test_crash_report_menu_item:
                showTestCrashReportPopup();
                return true;
            case R.id.export_database_menu_item:
                exportDatabase();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutPopup() {
        new AlertDialog.Builder(this)
                .setMessage(String.format("Version %s", BuildConfig.GIT_HASH))
                .create()
                .show();
    }

    private void showTestCrashReportPopup() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    throw new RuntimeException("Test Crash");
                })
                .create()
                .show();
    }

    private void exportDatabase() {
        File dbFile = new File(BioStampManager.getInstance().getDbImpl().getDatabasePath());
        Uri uri = FileProvider.getUriForFile(this,
                "com.mc10inc.biostamp3.sdkexample.dbfileprovider", dbFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Select destination for database export"));
    }

    public void requestBlePermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSIONS_REQUEST_FOR_BLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FOR_BLE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("User granted permissions for BLE");
            } else {
                Timber.e("User denied permissions for BLE");
            }
        }
    }

    private static class PagesAdapter extends FragmentPagerAdapter {
        private static class Page {
            private interface FragmentConstructor {
                Fragment construct();
            }

            FragmentConstructor constructor;
            String title;

            Page(FragmentConstructor constructor, String title) {
                this.constructor = constructor;
                this.title = title;
            }
        }

        private final List<Page> pages;

        PagesAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

            pages = new ArrayList<>();
            pages.add(new Page(ScanFragment::new, "Scan"));
            pages.add(new Page(SensorsFragment::new, "Sensors"));
            pages.add(new Page(ControlsFragment::new, "Controls"));
            pages.add(new Page(SensingFragment::new, "Sensing"));
            pages.add(new Page(StreamingFragment::new, "Streaming"));
            pages.add(new Page(DownloadFragment::new, "Download"));
            pages.add(new Page(RecordingsFragment::new, "Recordings"));
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            if (position < pages.size()) {
                return pages.get(position).constructor.construct();
            } else {
                return pages.get(0).constructor.construct();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position < pages.size()) {
                return pages.get(position).title;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return pages.size();
        }
    }
}
