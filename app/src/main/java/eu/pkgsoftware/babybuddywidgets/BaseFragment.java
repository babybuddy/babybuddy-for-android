package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    private AlertDialog dialog = null;

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
}
