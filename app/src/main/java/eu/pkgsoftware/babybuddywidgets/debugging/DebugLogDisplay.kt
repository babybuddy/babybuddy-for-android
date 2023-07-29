package eu.pkgsoftware.babybuddywidgets.debugging

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.databinding.FragmentDebugLogDisplayBinding

class DebugLogDisplay : BaseFragment() {
    lateinit var fragment: FragmentDebugLogDisplayBinding

    override fun onResume() {
        super.onResume()

        val stringBuilder = StringBuilder(16000)
        for (line in GlobalDebugObject.getLog()) {
            stringBuilder.append(line.trim())
            stringBuilder.append("\n<EOL>\n")
        }
        fragment.debugTextArea.setText(stringBuilder)

        mainActivity.setTitle(getString(R.string.export_debug_logs_title))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragment = FragmentDebugLogDisplayBinding.inflate(inflater)

        fragment.copyToClipboardButton.setOnClickListener {
            val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(
                "BabyBuddy Debug Data", fragment.debugTextArea.text
            ))

            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        return fragment.root
    }
}