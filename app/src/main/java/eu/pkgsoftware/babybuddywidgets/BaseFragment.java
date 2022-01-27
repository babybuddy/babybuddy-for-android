package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    public static interface Promise<S, F> {
        public void succeeded(S s);
        public void failed(F f);
    }

    private AlertDialog dialog = null;
    protected ProgressDialog progressDialog;

    protected AlertDialog showError(boolean override, String title, String errorMessage) {
        if (override) {
            hideError();
        }

        dialog = new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setMessage(errorMessage)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    hideError();
                }
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
        progressDialog.setMessage(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }
}
