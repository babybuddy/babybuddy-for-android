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

import java.text.DecimalFormat;
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
import eu.pkgsoftware.babybuddywidgets.widgets.HorizontalNumberPicker;

public class FeedingFragment extends BaseFragment {
    public interface ButtonListCallback {
        void onSelectionChanged(int i);
    };

    public static class AmountValuesGenerator implements HorizontalNumberPicker.ValueGenerator {
        public static final DecimalFormat FORMAT_VALUE = new DecimalFormat("#.#");

        public long minValue() {
            return -1L;
        }

        public long maxValue() {
            return 1000L;
        }

        public String getValue(long index) {
            if (index < 0) {
                return "None";
            } else {
                return FORMAT_VALUE.format(getRawValue(index, 0f));
            }
        }

        public Double getRawValue(long index, float offset) {
            if (index + offset < -0.001) {
                return null;
            } else {
                return Math.pow(1.2, index + offset);
            }
        }

        public long getValueIndex(Double value) {
            if (value == null) {
                return -1L;
            } else {
                long result = (long) Math.round(Math.log(value) / Math.log(1.2));
                result = Math.max(minValue(), Math.min(maxValue(), result));
                return result;
            }
        }

        public double getValueOffset(Double value) {
            if (value == null) {
                return 0.0d;
            } else {
                double result = getValueIndex(value) - Math.log(value) / Math.log(1.2);
                result = Math.max(-0.5, Math.min(0.5, result));
                return result;
            }
        }
    }

    private FeedingFragmentBinding binding = null;
    private Double amount = 30.0;
    private NotesEditorBinding notesEditor = null;
    private AmountValuesGenerator amountValuesGenerator = new AmountValuesGenerator();

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
                    setupFeedingMethodButtons(Constants.FeedingTypeEnumValues.get((int) l));
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
                                        //liveUpdateAmount(binding.seekBar.getProgress() - binding.seekBar.getMax() / 2);
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

        binding.amountNumberPicker.setValueGenerator(amountValuesGenerator);
        binding.amountNumberPicker.setValueUpdatedListener(
            new HorizontalNumberPicker.ValueUpdatedListener() {
                @Override
                public void valueChangeChanging(long valueIndex, float relativeOffset) {
                    amount = amountValuesGenerator.getRawValue(valueIndex, relativeOffset);
                    updateAmount();
                }

                @Override
                public void valueChangeFinished(long valueIndex, float relativeOffset) {
                    valueChangeChanging(valueIndex, relativeOffset);
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
        binding.amountNumberPicker.setValueIndex(amountValuesGenerator.getValueIndex(30.0));
        binding.amountNumberPicker.setRelativeValueIndexOffset(amountValuesGenerator.getValueOffset(30.0));

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
            i -> setupFeedingMethodButtons(Constants.FeedingTypeEnumValues.get(i))
        );
        binding.feedingMethodSpinner.setVisibility(View.GONE);
        binding.feedingMethodButtons.setVisibility(View.GONE);
        updateAmount();
    }

    private void updateAmount() {
        String text = "(None)";
        if (amount != null) {
            text = AmountValuesGenerator.FORMAT_VALUE.format(amount);
        }
        binding.amountText.setText("Amount: " + text);
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
        if (amount < 0) {
            amount = 0d;
        }
        updateAmount();
    }

    private List<Constants.FeedingMethodEnum> assignedMethodButtons = null;
    private void setupFeedingMethodButtons(Constants.FeedingTypeEnum type) {
        binding.submitButton.setVisibility(View.GONE);
        assignedMethodButtons = new ArrayList<>(10);

        switch (type) {
            case BREAST_MILK:
                assignedMethodButtons.add(Constants.FeedingMethodEnum.LEFT_BREAST);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.RIGHT_BREAST);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTH_BREASTS);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.SELF_FED);
                break;
            default:
                assignedMethodButtons.add(Constants.FeedingMethodEnum.BOTTLE);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.PARENT_FED);
                assignedMethodButtons.add(Constants.FeedingMethodEnum.SELF_FED);
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
        Constants.FeedingTypeEnum feedingType = Constants.FeedingTypeEnumValues.get((int) feedingTypeId);
        long feedingMethodId = binding.feedingMethodSpinner.getSelectedItemId();
        Constants.FeedingMethodEnum feedingMethod = assignedMethodButtons.get((int) feedingMethodId);

        Float dAmount = amount == null ? null : (float) (amount * 1.0d);

        mainActivity().getClient().createFeedingRecordFromTimer(
            mainActivity().selectedTimer,
            feedingType.post_name,
            feedingMethod.post_name,
            dAmount,
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
                    notesEditor.noteEditor.setText("");
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
