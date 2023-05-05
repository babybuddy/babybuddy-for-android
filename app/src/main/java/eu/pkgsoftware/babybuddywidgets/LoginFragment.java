package eu.pkgsoftware.babybuddywidgets;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.login.QRCode;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.login.GrabAppToken;
import eu.pkgsoftware.babybuddywidgets.utils.RunOnceAfterLayoutUpdate;

public class LoginFragment extends BaseFragment {
    private LoginFragmentBinding binding;

    private Button loginButton;
    private EditText addressEdit, loginNameEdit, loginPasswordEdit;

    private void updateLoginButton() {
        loginButton.setEnabled(
            (addressEdit.getText().length() > 0) &&
                (loginNameEdit.getText().length() > 0) &&
                (loginPasswordEdit.getText().length() > 0));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.loggedout_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.aboutPageMenuItem) {
            Navigation.findNavController(getView()).navigate(R.id.global_aboutFragment);
        }
        if (item.getItemId() == R.id.showHelpMenuButton) {
            getMainActivity().getCredStore().setTutorialParameter("help_hint", 10);
            Navigation.findNavController(getView()).navigate(R.id.global_showHelp);
        }
        return false;
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

        String cleanedAddress = ("" + addressEdit.getText()).trim();
        if (cleanedAddress.toLowerCase().startsWith("http:")) {
            new AlertDialog.Builder(getContext())
                .setTitle("Insecure connection")
                .setMessage(
                    "You have entered a URL  that does not start with 'https'. This means " +
                        "that the password you entered can be intercepted and stolen!")
                .setPositiveButton(
                    "Cancel (advised)",
                    (dialogInterface, i) -> dialogInterface.dismiss()
                )
                .setNegativeButton(
                    "Continue anyway",
                    (dialogInterface, i) -> performLogin()
                ).show();
        } else {
            if (!cleanedAddress.toLowerCase().startsWith("https:")) {
                addressEdit.setText("https://" + addressEdit.getText());
            }
            performLogin();
        }
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

        final QRCode qrCode = new QRCode(this, null, true);
        qrCode.setCameraOnInitialized(() -> {
            binding.qrCode.setEnabled(qrCode.getHasCamera());
            if (qrCode.getHasCamera()) {
                binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text);
            } else {
                binding.qrCodeInfoText.setText(R.string.login_qrcode_info_text_no_camera);
            }
        });

        if (getMainActivity().getCredStore().getAppToken() != null) {
            progressDialog.hide();
            moveToLoggedIn();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void testLogin(Promise<Object, String> promise) {
        CredStore credStore = new CredStore(getContext());
        if (credStore.getAppToken() != null) {
            final MainActivity mainActivity = getMainActivity();
            BabyBuddyClient client = mainActivity.getClient();
            client.listChildren(new BabyBuddyClient.RequestCallback<>() {
                @Override
                public void error(Exception error) {
                    promise.failed(error.getMessage());
                }

                @Override
                public void response(BabyBuddyClient.Child[] response) {
                    mainActivity.children = response;
                    promise.succeeded(new Object());
                }
            });
        } else {
            promise.failed("No app token found.");
        }
    }

    private void performLogin() {
        showProgress();

        CredStore credStore = getMainActivity().getCredStore();
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

        testLogin(new Promise<Object, String>() {
            @Override
            public void succeeded(Object o) {
                progressDialog.hide();
                moveToLoggedIn();
            }

            @Override
            public void failed(String s) {
                progressDialog.hide();
                showError(true, "Login failed", s);
            }
        });
    }

    private void moveToLoggedIn() {
        NavController controller = Navigation.findNavController(getView());
        controller.navigate(R.id.action_LoginFragment_to_loggedInFragment2);
    }
}