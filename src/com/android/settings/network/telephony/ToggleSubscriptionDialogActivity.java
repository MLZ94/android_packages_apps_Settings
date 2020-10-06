/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SwitchToEuiccSubscriptionSidecar;

/** This dialog activity handles both eSIM and pSIM subscriptions enabling and disabling. */
public class ToggleSubscriptionDialogActivity extends SubscriptionActionDialogActivity
        implements SidecarFragment.Listener {

    private static final String TAG = "ToggleSubscriptionDialogActivity";

    private static final String ARG_SUB_ID = "sub_id";
    private static final String ARG_enable = "enable";

    /**
     * Returns an intent of ToggleSubscriptionDialogActivity.
     * @param context The context used to start the ToggleSubscriptionDialogActivity.
     * @param subId The subscription ID of the subscription needs to be toggled.
     * @param enable Whether the activity should enable or disable the subscription.
     */
    public static Intent getIntent(Context context, int subId, boolean enable) {
        Intent intent = new Intent(context, ToggleSubscriptionDialogActivity.class);
        intent.putExtra(ARG_SUB_ID, subId);
        intent.putExtra(ARG_enable, enable);
        return intent;
    }

    private SubscriptionManager mSubscriptionManager;
    private SubscriptionInfo mSubInfo;
    private SwitchToEuiccSubscriptionSidecar mSwitchToEuiccSubscriptionSidecar;
    private AlertDialog mToggleSimConfirmDialog;
    private boolean mEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int subId = intent.getIntExtra(ARG_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mSubscriptionManager = getSystemService(SubscriptionManager.class);

        UserManager userManager = getSystemService(UserManager.class);
        if (!userManager.isAdminUser()) {
            Log.e(TAG, "It is not the admin user. Unable to toggle subscription.");
            finish();
            return;
        }

        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.e(TAG, "The subscription id is not usable.");
            finish();
            return;
        }

        mSubInfo = SubscriptionUtil.getSubById(mSubscriptionManager, subId);
        mSwitchToEuiccSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getFragmentManager());
        mEnable = intent.getBooleanExtra(ARG_enable, true);

        if (mEnable) {
            handleEnablingSubAction();
        } else {
            handleDisablingSubAction();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchToEuiccSubscriptionSidecar.addListener(this);
    }

    @Override
    protected void onPause() {
        mSwitchToEuiccSubscriptionSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchToEuiccSubscriptionSidecar) {
            handleSwitchToEuiccSubscriptionSidecarStateChange();
        }
    }

    private void handleSwitchToEuiccSubscriptionSidecarStateChange() {
        switch (mSwitchToEuiccSubscriptionSidecar.getState()) {
            case SidecarFragment.State.SUCCESS:
                Log.i(
                        TAG,
                        String.format(
                                "Successfully %s the eSIM profile.",
                                mEnable ? "enable" : "disable"));
                mSwitchToEuiccSubscriptionSidecar.reset();
                dismissProgressDialog();
                finish();
                break;
            case SidecarFragment.State.ERROR:
                Log.i(
                        TAG,
                        String.format(
                                "Failed to %s the eSIM profile.", mEnable ? "enable" : "disable"));
                mSwitchToEuiccSubscriptionSidecar.reset();
                dismissProgressDialog();
                showErrorDialog(
                        getString(R.string.privileged_action_disable_fail_title),
                        getString(R.string.privileged_action_disable_fail_text),
                        (dialog, which) -> finish());
                break;
        }
    }

    /* Handles the enabling SIM action. */
    private void handleEnablingSubAction() {
        Log.i(TAG, "handleEnableSub");
        // TODO(b/160819390): Implement enabling eSIM/pSIM profile.
    }

    /* Handles the disabling SIM action. */
    private void handleDisablingSubAction() {
        showToggleSimConfirmDialog(
                (dialog, which) -> {
                    if (mSubInfo.isEmbedded()) {
                        Log.i(TAG, "Disabling the eSIM profile.");
                        showProgressDialog(
                                getString(R.string.privileged_action_disable_sub_dialog_progress));
                        mSwitchToEuiccSubscriptionSidecar.run(
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                        return;
                    }
                    Log.i(TAG, "Disabling the pSIM profile.");
                    // TODO(b/160819390): Implement disabling pSIM profile.
                });
    }

    /* Displays the SIM toggling confirmation dialog. */
    private void showToggleSimConfirmDialog(
            DialogInterface.OnClickListener positiveOnClickListener) {
        if (mToggleSimConfirmDialog == null) {
            mToggleSimConfirmDialog =
                    new AlertDialog.Builder(this)
                            .setTitle(getToggleSimConfirmDialogTitle())
                            .setPositiveButton(
                                    R.string.yes,
                                    (dialog, which) -> {
                                        positiveOnClickListener.onClick(dialog, which);
                                        dismissToggleSimConfirmDialog();
                                    })
                            .setNegativeButton(
                                    R.string.cancel,
                                    (dialog, which) -> {
                                        dismissToggleSimConfirmDialog();
                                        finish();
                                    })
                            .create();
        }
        mToggleSimConfirmDialog.show();
    }

    /* Dismisses the SIM toggling confirmation dialog. */
    private void dismissToggleSimConfirmDialog() {
        if (mToggleSimConfirmDialog != null) {
            mToggleSimConfirmDialog.dismiss();
        }
    }

    /* Returns the title of toggling SIM confirmation dialog. */
    private String getToggleSimConfirmDialogTitle() {
        if (mEnable) {
            // TODO(b/160819390): Handle the case for enabling SIM.
            return null;
        }
        return mSubInfo == null || TextUtils.isEmpty(mSubInfo.getDisplayName())
                ? getString(R.string.privileged_action_disable_sub_dialog_title_without_carrier)
                : getString(
                        R.string.privileged_action_disable_sub_dialog_title,
                        mSubInfo.getDisplayName());
    }
}