package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.databinding.LoginFragmentBinding;

public class LoginFragment extends Fragment {
    public static interface Promise<S, F> {
        public void succeeded(S s);
        public void failed(F f);
    }

    private LoginFragmentBinding binding;
    private ProgressDialog progressDialog;

    private Button loginButton;
    private EditText addressEdit, loginNameEdit, loginPasswordEdit;

    private void updateLoginButton() {
        loginButton.setEnabled(
                (addressEdit.getText().length() > 0) &&
                        (loginNameEdit.getText().length() > 0) &&
                        (loginPasswordEdit.getText().length() > 0));
    }

    private MainActivity mainActivity() {
        return (MainActivity) getActivity();
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

        CredStore credStore = new CredStore(getContext());
        String serverUrl = credStore.getServerUrl();
        if (serverUrl == null) {
            serverUrl = "";
        }
        addressEdit.setText(serverUrl);

        addressEdit.addTextChangedListener(tw);
        loginNameEdit.addTextChangedListener(tw);
        loginPasswordEdit.addTextChangedListener(tw);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performLogin();
            }
        });

        updateLoginButton();


        progressDialog = new ProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.LoggingInMessage));

        progressDialog.show();
        testLogin(new Promise<Object, String>() {
            @Override
            public void succeeded(Object o) {
                progressDialog.hide();
                moveToLoggedIn();
            }

            @Override
            public void failed(String s) {
                progressDialog.hide();
            }
        });

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void testLogin(Promise<Object, String> promise) {
        CredStore credStore = new CredStore(getContext());
        if (credStore.getAppToken() != null) {
            BabyBuddyClient client = new BabyBuddyClient(getActivity().getMainLooper(), credStore);
            client.listChildren(new BabyBuddyClient.RequestCallback<BabyBuddyClient.Child[]>() {
                @Override
                public void error(Exception error) {
                    promise.failed(error.getMessage());
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Login failed")
                            .setMessage("[Login failed?]")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                }

                @Override
                public void response(BabyBuddyClient.Child[] response) {
                    promise.succeeded(new Object());
                }
            });
        }
    }

    private void performLogin() {
        progressDialog.show();

        CredStore credStore = new CredStore(getContext());
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
                new AlertDialog.Builder(getContext())
                        .setTitle("Login failed")
                        .setMessage("[Login failed?]")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .show();
            }
        });
    }

    private void moveToLoggedIn() {
        NavController controller = Navigation.findNavController(getView());
        controller.navigate(R.id.action_LoginFragment_to_loggedInFragment2);
    }
}