package eu.pkgsoftware.babybuddywidgets.login;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.CredStore;
import eu.pkgsoftware.babybuddywidgets.MainActivity;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.TutorialAccess;
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.utils.RunOnceAfterLayoutUpdate;

public class LoginFragment extends BaseFragment {
    private LoginFragmentBinding binding;

    private Button loginButton;
    private EditText addressEdit, loginNameEdit, loginPasswordEdit;

    private final LoggedOutMenu menu = new LoggedOutMenu(this);

    private void updateLoginButton() {
        loginButton.setEnabled(
            (addressEdit.getText().length() > 0) &&
                (loginNameEdit.getText().length() > 0) &&
                (loginPasswordEdit.getText().length() > 0));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = LoginFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        loginButton = view.findViewById(R.id.loginButton);
        addressEdit = view.findViewById(R.id.serverAddressEdit);
        loginNameEdit = view.findViewById(R.id.loginNameEdit);
        loginPasswordEdit = view.findViewById(R.id.passwordEdit);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateLoginButton();
            }
        };

        CredStore credStore = getMainActivity().getCredStore();
        String serverUrl = credStore.getServerUrl();
        if (serverUrl == null) {
            serverUrl = "";
            if (isTestlab()) {
                serverUrl = "https://babybuddy-test.pkgsoftware.eu/";
            }
        }
        addressEdit.setText(serverUrl);

        addressEdit.addTextChangedListener(tw);
        loginNameEdit.addTextChangedListener(tw);
        loginPasswordEdit.addTextChangedListener(tw);

        loginButton.setOnClickListener(view1 -> {
            uiStartLogin();
        });

        binding.passwordEdit.setText("");
        binding.loginNameEdit.setText("");
        binding.passwordEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                uiStartLogin();
                return true;
            }
            return false;
        });

        binding.loginInfoText.setMovementMethod(LinkMovementMethod.getInstance());

        binding.qrCode.setOnClickListener(view1 -> {
            NavController controller = Navigation.findNavController(getView());
            controller.navigate(R.id.action_LoginFragment_to_QRCodeLoginFragment);
        });
        binding.qrCode.setEnabled(false);

        updateLoginButton();

        final View mainLayout = getMainActivity().findViewById(R.id.main_layout);
        mainLayout.requestLayout();
        new RunOnceAfterLayoutUpdate(mainLayout, () -> {
            Rect r = new Rect();

            final int hintPresentedCount = credStore.getTutorialParameter("help_hint");
            if (hintPresentedCount >= 2) {
                return;
            }

            MainActivity mainAct = getMainActivity();
            if (mainAct == null) {
                return; // Hotfix -  can happen when the app restarts from resume-state if the fragment deactivates.
            }
            View toolbar = mainAct.findViewById(R.id.app_toolbar);
            toolbar.getGlobalVisibleRect(r);
            TutorialAccess tutorialAccess = getMainActivity().getTutorialAccess();
            tutorialAccess.tutorialMessage(
                r.right - dpToPx(20),
                r.top,
                getString(R.string.tutorial_help_1)
            );
            tutorialAccess.setManuallyDismissedCallback(
                () -> credStore.setTutorialParameter("help_hint", hintPresentedCount + 1)
            );
        });

        return binding.getRoot();
    }

    private void uiStartLogin() {
        hideKeyboard();

        new Utils(getMainActivity()).httpCleaner(
            addressEdit.getText().toString(),
            new Promise<>() {
                @Override
                public void succeeded(String s) {
                    addressEdit.setText(s);
                    performLogin();
                }

                @Override
                public void failed(Object o) {
                }
            }
        );
    }

    /**
     * Check if running in a testlab-context
     */
    @SuppressLint("SetTextI18n")
    private boolean isTestlab() {
        boolean testlab = false;

        ContentResolver cr = getMainActivity().getContentResolver();
        if (cr != null) {
            String testLabSetting = Settings.System.getString(cr, "firebase.test.lab");
            if ("true".equals(testLabSetting)) {
                testlab = true;
            }
        }

        return testlab;
    }

    private void showProgress() {
        showProgress(getString(R.string.logging_in_message));
    }

    @Override
    public void onResume() {
        super.onResume();
        getMainActivity().setTitle("Login to Baby Buddy");

        getMainActivity().addMenuProvider(menu);

        Bundle b = getArguments();
        if (b != null) {
            if (b.getBoolean("noCameraAccess", false)) {
                binding.errorBubble.flashMessage(
                    R.string.login_qrcode_camera_access_was_disabled_message,
                    5000
                );
            }
        }

        if (getMainActivity().getCredStore().getAppToken() != null) {
            progressDialog.hide();
            moveToLoggedIn();
        } else {
            final QRCode qrCode = new QRCode(this, null, true);
            qrCode.setCameraOnInitialized(() -> {
                binding.qrCode.setEnabled(qrCode.getHasCamera());
                if (qrCode.getHasCamera()) {
                    binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text);
                } else {
                    binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text_no_camera);
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getMainActivity().removeMenuProvider(menu);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void performLogin() {
        showProgress();

        final CredStore credStore = getMainActivity().getCredStore();
        credStore.storeServerUrl(addressEdit.getText().toString());
        String token = null;
        try {
            token = GrabAppToken.grabToken(
                addressEdit.getText().toString(),
                loginNameEdit.getText().toString(),
                loginPasswordEdit.getText().toString());
        } catch (IOException e) {
            showError(true, "Login failed", e.getMessage());
            progressDialog.hide();
            return;
        } catch (Exception e) {
            showError(true, "Login failed", "Internal error message: " + e.getMessage());
            progressDialog.hide();
            return;
        }
        credStore.storeAppToken(token);

        new Utils(getMainActivity()).testLoginToken(
            new Promise<>() {
                @Override
                public void succeeded(Object o) {
                    progressDialog.hide();
                    moveToLoggedIn();
                }

                @Override
                public void failed(String s) {
                    progressDialog.hide();
                    credStore.storeAppToken(null);
                    showError(true, "Login failed", s);
                }
            }
        );
    }

    private void moveToLoggedIn() {
        NavController controller = Navigation.findNavController(getView());
        controller.navigate(R.id.action_LoginFragment_to_loggedInFragment2);
    }
}