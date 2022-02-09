package eu.pkgsoftware.babybuddywidgets;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import eu.pkgsoftware.babybuddywidgets.databinding.FeedingFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class FeedingFragment extends BaseFragment {
    public interface ButtonListCallback {
        void onSelectionChanged(int i);
    };

    private enum FeedingTypeEnum {
        BREAST_MILK(0, "breast milk"),
        FORMULA(1, "formula"),
        FORTIFIED_BREAST_MILK(2, "fortified breast milk"),
        SOLID_FOOD(3, "solid food");

        public int value;
        public String post_name;
        FeedingTypeEnum(int v, String post_name) {
            value = v;
            this.post_name = post_name;
        }
    };
    public static Map<Integer, FeedingTypeEnum> FeedingTypeEnumValues = new HashMap<>();
    static {
        for (FeedingTypeEnum e : FeedingTypeEnum.values()) {
            FeedingTypeEnumValues.put(e.value, e);
        }
    }

    private enum FeedingMethodEnum {
        BOTTLE(0, "bottle"),
        LEFT_BREAST(1, "left breast"),
        RIGHT_BREAST(2, "right breast"),
        BOTH_BREASTS(3, "both breasts"),
        PARENT_FED(4, "parent fed"),
        SELF_FED(5, "self fed");

        public int value;
        public String post_name;
        FeedingMethodEnum(int v, String post_name) {
            value = v;
            this.post_name = post_name;
        }
    };
    public static Map<Integer, FeedingMethodEnum> FeedingMethodEnumValues = new HashMap<>();
    static {
        for (FeedingMethodEnum e : FeedingMethodEnum.values()) {
            FeedingMethodEnumValues.put(e.value, e);
        }
    }

    private FeedingFragmentBinding binding = null;
    private double amount = 30.0;
    private NotesEditorBinding notesEditor = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FeedingFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitFeeding();
            }
        });
        binding.feedingTypeSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    setupFeedingMethodButtons(FeedingTypeEnumValues.get((int) l));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            }
        );

        binding.seekBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                Timer timer = null;
                Handler handler = new Handler(getActivity().getMainLooper());

                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        liveUpdateAmount(binding.seekBar.getProgress() - binding.seekBar.getMax() / 2);
                                    }
                                }
                            );
                        }
                    }, 0, 200);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    timer.cancel();
                    timer.purge();
                    timer = null;
                    binding.seekBar.setProgress(binding.seekBar.getMax() / 2);
                }
            }
        );

        notesEditor = NotesEditorBinding.inflate(mainActivity().getLayoutInflater());
        binding.notes.addView(notesEditor.getRoot());

        resetVisibilityState();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        amount = 30.0;

        resetVisibilityState();
    }

    @Override
    public void onResume() {
        super.onResume();

        CredStore.Notes notes = mainActivity().getCredStore().getObjectNotes(
            "timer_" + mainActivity().selectedTimer.id
        );
        notesEditor.noteEditor.setText(notes.visible ? notes.note : "");
    }

    @Override
    public void onPause() {
        super.onPause();

        CredStore.Notes notes = mainActivity().getCredStore().getObjectNotes(
            "timer_" + mainActivity().selectedTimer.id
        );
        notes.note = notesEditor.noteEditor.getText().toString();
        mainActivity().getCredStore().setObjectNotes(
            "timer_" + mainActivity().selectedTimer.id,
            notes.visible,
            notes.note
        );
        mainActivity().getCredStore().storePrefs();
    }

    private void resetVisibilityState() {
        populateButtonList(
            getResources().getTextArray(R.array.feedingTypes),
            binding.feedingTypeButtons,
            binding.feedingTypeSpinner,
            i -> setupFeedingMethodButtons(FeedingTypeEnumValues.get(i))
        );
        binding.feedingMethodSpinner.setVisibility(View.GONE);
        binding.feedingMethodButtons.setVisibility(View.GONE);
        updateAmount();
    }

    private void updateAmount() {
        binding.amountText.setText("Amount: " + amount);
    }

    private static class ButtonListOnClickListener implements View.OnClickListener {
        private int i;
        private ButtonListCallback cb;

        public ButtonListOnClickListener(ButtonListCallback cb, int i) {
            this.i = i;
            this.cb = cb;
        }

        public void onClick(View view) {
            cb.onSelectionChanged(i);
        }
    }

    private void populateButtonList(CharSequence[] textArray, LinearLayout buttons, Spinner spinner, ButtonListCallback callback) {
        spinner.setVisibility(View.GONE);
        buttons.setVisibility(View.VISIBLE);

        buttons.removeAllViewsInLayout();
        for (int i = 0; i < textArray.length; i++) {
            Button button = new Button(getContext());
            button.setOnClickListener(
                new ButtonListOnClickListener(
                    i0 -> {
                        spinner.setSelection(i0);
                        spinner.setVisibility(View.VISIBLE);
                        buttons.setVisibility(View.GONE);
                        callback.onSelectionChanged(i0);
                    }, i)
            );
            button.setText(textArray[i]);
            button.setLayoutParams(
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1));
            buttons.addView(button);
        }
    }

    private void liveUpdateAmount(int offset) {
        final int[] baseFactors = {1, 2, 5, 10};
        double foffset = 0;
        if (offset == 0) {
        } else {
            double sign = offset / Math.abs(offset);
            offset = Math.abs(offset) - 1;
            foffset = 1;
            while (offset > 0) {
                if (offset >= baseFactors.length) {
                    foffset *= baseFactors[baseFactors.length - 1];
                    offset -= baseFactors.length;
                } else {
                    foffset *= baseFactors[offset];
                    offset = 0;
                }
            }
            amount += foffset * sign;
        }
        if (amount < 1) {
            amount = 1;
        }
        updateAmount();
    }

    private List<FeedingMethodEnum> assignedMethodButtons = null;
    private void setupFeedingMethodButtons(FeedingTypeEnum type) {
        binding.submitButton.setVisibility(View.GONE);
        assignedMethodButtons = new ArrayList<>(10);

        switch (type) {
            case BREAST_MILK:
                assignedMethodButtons.add(FeedingMethodEnum.LEFT_BREAST);
                assignedMethodButtons.add(FeedingMethodEnum.RIGHT_BREAST);
                assignedMethodButtons.add(FeedingMethodEnum.BOTH_BREASTS);
                assignedMethodButtons.add(FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(FeedingMethodEnum.SELF_FED);
                break;
            default:
                assignedMethodButtons.add(FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(FeedingMethodEnum.SELF_FED);
        }

        CharSequence[] orgItems = getResources().getTextArray(R.array.feedingMethods);
        List<CharSequence> textItems = new ArrayList<>(10);
        for (int i = 0; i < assignedMethodButtons.size(); i++) {
            textItems.add(orgItems[assignedMethodButtons.get(i).value]);
        }

        binding.feedingMethodSpinner.setAdapter(
            new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_dropdown_item, textItems)
        );

        populateButtonList(
            textItems.toArray(
                new CharSequence[0]),
                binding.feedingMethodButtons,
                binding.feedingMethodSpinner,
            i -> binding.submitButton.setVisibility(View.VISIBLE)
        );
    }

    private MainActivity mainActivity() {
        return (MainActivity) getActivity();
    }

    private void submitFeeding() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.LoggingInMessage));

        long feedingTypeId = binding.feedingTypeSpinner.getSelectedItemId();
        FeedingTypeEnum feedingType = FeedingTypeEnumValues.get((int) feedingTypeId);
        long feedingMethodId = binding.feedingMethodSpinner.getSelectedItemId();
        FeedingMethodEnum feedingMethod = assignedMethodButtons.get((int) feedingMethodId);

        mainActivity().getClient().createFeedingRecordFromTimer(
            mainActivity().selectedTimer,
            feedingType.post_name,
            feedingMethod.post_name,
            (float) amount,
            notesEditor.noteEditor.getText().toString().trim(),
            new BabyBuddyClient.RequestCallback<Boolean>() {
                @Override
                public void error(Exception error) {
                    progressDialog.cancel();

                    showError(
                        true,
                        "Failed storing feeding",
                        "Error: " + error.getMessage(),
                        b -> navUp()
                    );
                }

                @Override
                public void response(Boolean response) {
                    progressDialog.cancel();
                    mainActivity().getCredStore().setObjectNotes(
                        "timer_" + mainActivity().selectedTimer.id,
                        false,
                        ""
                    );
                    mainActivity().getCredStore().storePrefs();
                    navUp();
                }
            }
        );
    }

    private void navUp() {
        NavController nav = Navigation.findNavController(getView());
        nav.navigateUp();
    }
}
