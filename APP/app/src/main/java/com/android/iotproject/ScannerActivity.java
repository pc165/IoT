/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.iotproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.iotproject.adapter.DevicesAdapter;
import com.android.iotproject.adapter.DiscoveredBluetoothDevice;
import com.android.iotproject.databinding.ActivityScannerBinding;
import com.android.iotproject.utils.Utils;
import com.android.iotproject.viewmodels.ScannerStateLiveData;
import com.android.iotproject.viewmodels.ScannerViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {
    private ScannerViewModel scannerViewModel;
    private ActivityScannerBinding binding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        // Set the proper theme for the Activity. This could have been set in "v23/styles..xml"
        // as "postSplashScreenTheme", but as this app works on pre-API-23 devices, it needs to be
        // set for them as well, and that code would not apply in suce case.
        // As "postSplashScreenTheme" is optional, and setting the theme can be done using
        // setTheme, this is preferred in our case, as this also work for older platforms.
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final MaterialToolbar toolbar = binding.toolbar;
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::startScan);

        // Configure the recycler view
        final RecyclerView recyclerView = binding.recyclerViewBleDevices;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        final RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        final DevicesAdapter adapter = new DevicesAdapter(this, scannerViewModel.getDevices());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        // Set up permission request launcher
        final ActivityResultLauncher<String> requestPermission =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                        result -> scannerViewModel.refresh()
                );
        final ActivityResultLauncher<String[]> requestPermissions =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> scannerViewModel.refresh()
                );

        // Configure views
        binding.noDevices.actionEnableLocation.setOnClickListener(v -> openLocationSettings());
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> requestBluetoothEnabled());
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))
                Utils.markLocationPermissionRequested(this);
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> {
            Utils.clearLocationPermissionRequested(this);
            openPermissionSettings();
        });

        if (Utils.isSorAbove()) {
            binding.noBluetoothPermission.actionGrantBluetoothPermission.setOnClickListener(v -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.BLUETOOTH_SCAN)) {
                    Utils.markBluetoothScanPermissionRequested(this);
                }
                requestPermissions.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                });
            });
            binding.noBluetoothPermission.actionPermissionSettings.setOnClickListener(v -> {
                Utils.clearBluetoothPermissionRequested(this);
                openPermissionSettings();
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
        menu.findItem(R.id.filter_uuid).setChecked(scannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(scannerViewModel.isNearbyFilterEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.filter_uuid) {
            item.setChecked(!item.isChecked());
            scannerViewModel.filterByUuid(item.isChecked());
            return true;
        } else if (itemId == R.id.filter_nearby) {
            item.setChecked(!item.isChecked());
            scannerViewModel.filterByDistance(item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull final DiscoveredBluetoothDevice device) {
        final Intent controlBlinkIntent = new Intent(this, FragmentActivity.class);
        controlBlinkIntent.putExtra(FragmentActivity.EXTRA_DEVICE, device);
        startActivity(controlBlinkIntent);
    }

    /**
     * Starts scanning for Bluetooth LE devices or displays a message based on the scanner state.
     */
    private void startScan(@NonNull final ScannerStateLiveData state) {
        // First, check the Location permission.
        // This is required since Marshmallow up until Android 11 in order to scan for Bluetooth LE
        // devices.
        if (!Utils.isLocationPermissionRequired() ||
                Utils.isLocationPermissionGranted(this)) {
            binding.noLocationPermission.getRoot().setVisibility(View.GONE);

            // On Android 12+ a new BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions need to be
            // requested.
            //
            // Note: This has to be done before asking user to enable Bluetooth, as
            //       sending BluetoothAdapter.ACTION_REQUEST_ENABLE intent requires
            //       BLUETOOTH_CONNECT permission.
            if (!Utils.isSorAbove() || Utils.isBluetoothScanPermissionGranted(this)) {
                binding.noBluetoothPermission.getRoot().setVisibility(View.GONE);

                // Bluetooth must be enabled
                if (state.isBluetoothEnabled()) {
                    binding.bluetoothOff.getRoot().setVisibility(View.GONE);

                    // We are now OK to start scanning
                    scannerViewModel.startScan();
                    binding.stateScanning.setVisibility(View.VISIBLE);

                    if (!state.hasRecords()) {
                        binding.noDevices.getRoot().setVisibility(View.VISIBLE);

                        if (!Utils.isLocationRequired(this) ||
                                Utils.isLocationEnabled(this)) {
                            binding.noDevices.noLocation.setVisibility(View.INVISIBLE);
                        } else {
                            binding.noDevices.noLocation.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.noDevices.getRoot().setVisibility(View.GONE);
                    }
                } else {
                    binding.bluetoothOff.getRoot().setVisibility(View.VISIBLE);
                    binding.stateScanning.setVisibility(View.INVISIBLE);
                    binding.noDevices.getRoot().setVisibility(View.GONE);
                    binding.noBluetoothPermission.getRoot().setVisibility(View.GONE);
                    clear();
                }
            } else {
                binding.noBluetoothPermission.getRoot().setVisibility(View.VISIBLE);
                binding.bluetoothOff.getRoot().setVisibility(View.GONE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.getRoot().setVisibility(View.GONE);

                final boolean deniedForever = Utils.isBluetoothScanPermissionDeniedForever(this);
                binding.noBluetoothPermission.actionGrantBluetoothPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
                binding.noBluetoothPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
            }
        } else {
            binding.noLocationPermission.getRoot().setVisibility(View.VISIBLE);
            binding.noBluetoothPermission.getRoot().setVisibility(View.GONE);
            binding.bluetoothOff.getRoot().setVisibility(View.GONE);
            binding.stateScanning.setVisibility(View.INVISIBLE);
            binding.noDevices.getRoot().setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            binding.noLocationPermission.actionGrantLocationPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            binding.noLocationPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Stops scanning for Bluetooth LE devices.
     */
    private void stopScan() {
        scannerViewModel.stopScan();
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        scannerViewModel.getDevices().clear();
        scannerViewModel.getScannerState().clearRecords();
    }

    /**
     * Opens application settings in Android Settings app.
     */
    private void openPermissionSettings() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Opens Location settings.
     */
    private void openLocationSettings() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Shows a prompt to the user to enable Bluetooth on the device.
     *
     * @implSpec On Android 12+ BLUETOOTH_CONNECT permission needs to be granted before calling
     * this method. Otherwise, the app would crash with {@link SecurityException}.
     * @see BluetoothAdapter#ACTION_REQUEST_ENABLE
     */
    private void requestBluetoothEnabled() {
        if (Utils.isBluetoothConnectPermissionGranted(this)) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }
}
