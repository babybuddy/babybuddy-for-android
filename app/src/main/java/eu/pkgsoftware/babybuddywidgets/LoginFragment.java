package eu.pkgsoftware.babybuddywidgets;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.GrabAppToken;

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
        });

        binding.passwordEdit.setText("");
        binding.loginNameEdit.setText("");

        binding.loginInfoText.setMovementMethod(LinkMovementMethod.getInstance());

        updateLoginButton();

        return binding.getRoot();
    }

    /**
     * Check if running in a testlab-context
     */
    @SuppressLint("SetTextI18n")
    private boolean isTestlab() {
        boolean testlab = false;

        ContentResolver cr = getMainActivity().getContentResolver();
        if (cr != null) {
            String testLabSetting = Settings.System.getString(cr,"firebase.test.lab");
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
            BabyBuddyClient client = getMainActivity().getClient();
            client.listChildren(new BabyBuddyClient.RequestCallback<BabyBuddyClient.Child[]>() {
                @Override
                public void error(Exception error) {
                    promise.failed(error.getMessage());
                }

                @Override
                public void response(BabyBuddyClient.Child[] response) {
                    getMainActivity().children = response;
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
        String token = GrabAppToken.grabToken(
                addressEdit.getText().toString(),
                loginNameEdit.getText().toString(),
                loginPasswordEdit.getText().toString());
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