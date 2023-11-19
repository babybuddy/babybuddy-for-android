package eu.pkgsoftware.babybuddywidgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import eu.pkgsoftware.babybuddywidgets.networking.CoordinatedDisconnectDialog;
import eu.pkgsoftware.babybuddywidgets.tutorial.Trackable;
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialEntry;
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialManagement;

public abstract class BaseFragment extends Fragment {
    public interface DialogCallback {
        void call(boolean b);
    }

    private AlertDialog dialog = null;
    public CoordinatedDisconnectDialog disconnectDialog;

    protected ProgressDialog progressDialog;

    public AlertDialog showError(boolean override, String title, String errorMessage) {
        return showError(override, title, errorMessage, aBoolean -> {
        });
    }

    public AlertDialog showError(boolean override, int title, String errorMessage) {
        return showError(override, getString(title), errorMessage, aBoolean -> {
        });
    }

    public AlertDialog showError(boolean override, String title, int errorMessage) {
        return showError(override, title, getString(errorMessage), aBoolean -> {
        });
    }

    public AlertDialog showError(boolean override, int title, int errorMessage) {
        return showError(override, getString(title), getString(errorMessage), aBoolean -> {
        });
    }

    public AlertDialog showError(boolean override, String title, String errorMessage, DialogCallback callback) {
        if (override) {
            hideError();
        } else {
            if ((dialog != null) && (dialog.isShowing())) {
                return dialog;
            }
        }
        if (dialog != null) {
            dialog.dismiss();
        }

        dialog = new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setMessage(errorMessage)
            .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                hideError();
                callback.call(true);
            })
            .show();
        return dialog;
    }

    public AlertDialog showQuestion(boolean override, String title, String question, String positiveMessage, String negativeMessage, DialogCallback callback) {
        if (override) {
            hideError();
        } else {
            if ((dialog != null) && (dialog.isShowing())) {
                return dialog;
            }
        }
        if (dialog != null) {
            dialog.dismiss();
        }

        dialog = new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setMessage(question)
            .setCancelable(false)
            .setPositiveButton(positiveMessage, (dialogInterface, i) -> {
                hideError();
                callback.call(true);
            })
            .setNegativeButton(negativeMessage, (dialogInterface, i) -> {
                hideError();
                callback.call(false);
            })
            .show();
        return dialog;
    }

    public void hideError() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    public void runAfterDialog(Runnable r) {
        if (dialog != null) {
            dialog.setOnDismissListener(dialog -> r.run());
        } else {
            r.run();
        }
    }

    public void hideKeyboard() {
        // Modified from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-programmatically
        Activity activity = getActivity();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        TutorialManagement tMan = getMainActivity().getTutorialManagement();
        setupTutorialMessages(tMan);
        tMan.selectActiveFragment(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getMainActivity().getTutorialManagement().deselectActiveFragment(this);
        hideError();
    }

    protected void setupTutorialMessages(@NotNull TutorialManagement m) {
    }

    protected TutorialEntry makeTutorialEntry(@NotNull String id, @NotNull String text, @NotNull Trackable trackable) {
        return new TutorialEntry(
            id,
            getClass(),
            text,
            trackable
        );
    }

    protected TutorialEntry makeTutorialEntry(@NotNull String text, @NotNull Trackable trackable) {
        return new TutorialEntry(
            text.toLowerCase().replaceAll(" ", "_").substring(0, 12) + "_"+ text.hashCode(),
            getClass(),
            text,
            trackable
        );
    }

    protected TutorialEntry makeTutorialEntry(@StringRes int textRes, @NotNull Trackable trackable) {
        String resName = getResources().getResourceEntryName(textRes);
        String text = getString(textRes);
        return new TutorialEntry(
            resName,
            getClass(),
            text,
            trackable
        );
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.hide();

        disconnectDialog = new CoordinatedDisconnectDialog(
            this, getMainActivity().getCredStore()
        );
    }

    public void showProgress(String message) {
        progressDialog.setCancelable(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    public void showProgress(String message, String cancelButtonText, DialogCallback cancelButton) {
        progressDialog.setMessage(message);
        progressDialog.setCancelable(true);
        progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, cancelButtonText, (dialogInterface, i) -> {
            cancelButton.call(false);
        });
        progressDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    public void showUrlInBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public int dpToPx(float dp) {
        return Tools.dpToPx(getContext(), dp);
    }
}
