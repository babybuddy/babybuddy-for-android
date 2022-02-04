package eu.pkgsoftware.babybuddywidgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    public static interface Callback<T> {
        public void call(T t);
    }

    public static interface Promise<S, F> {
        public void succeeded(S s);
        public void failed(F f);
    }

    private AlertDialog dialog = null;
    protected ProgressDialog progressDialog;

    protected AlertDialog showError(boolean override, String title, String errorMessage) {
        return showError(override, title, errorMessage, aBoolean -> {});
    }

    protected AlertDialog showError(boolean override, String title, String errorMessage, Callback<Boolean> callback) {
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
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    hideError();
                    callback.call(true);
                }
            })
            .show();
        return dialog;
    }

    protected AlertDialog showQuestion(boolean override, String title, String question, String positiveMessage, String negativeMessage, Callback<Boolean> callback) {
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

    protected void hideError() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }
    }

    protected void hideKeyboard() {
        // Modified from https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-programmatically
        Activity activity = getActivity();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideError();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.hide();
    }

    public void showProgress(String message) {
        progressDialog.setCancelable(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    public void showProgress(String message, String cancelButtonText, Callback<Object> cancelButton) {
        progressDialog.setMessage(message);
        progressDialog.setCancelable(true);
        progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, cancelButtonText, (dialogInterface, i) -> {
            cancelButton.call(null);
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

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    protected void showUrlInBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
