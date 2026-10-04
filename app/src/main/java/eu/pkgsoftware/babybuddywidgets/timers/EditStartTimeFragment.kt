package eu.pkgsoftware.babybuddywidgets.timers

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.databinding.FragmentEditStartTimeDialogBinding
import java.util.Calendar
import java.util.Date

const val ARG_SELECTED_TIME = "selected_time"
const val RESULT_KEY = "result_time"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    EditStartTimeFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class EditStartTimeFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditStartTimeDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var selectedTimeArg: Date?
        get() {
            val time = arguments?.getLong(ARG_SELECTED_TIME)
            if (time != null) {
                return Date(time)
            }
            return null
        }
        set(value) {
            arguments?.putLong(ARG_SELECTED_TIME, value?.time ?: 0)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditStartTimeDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedTimeArg?.let {
            val c = Calendar.getInstance()
            c.time = it
            binding.timePicker.hour = c.get(Calendar.HOUR_OF_DAY)
            binding.timePicker.minute = c.get(Calendar.MINUTE)
        }
        binding.okButton.setOnClickListener {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
            c.set(Calendar.MINUTE, binding.timePicker.minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val resultTime = c.time
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply {
                putLong(RESULT_KEY, resultTime.time)
            })
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {

        fun newInstance(dateTime: Date): EditStartTimeFragment =
            EditStartTimeFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SELECTED_TIME, dateTime.time)
                }
            }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}