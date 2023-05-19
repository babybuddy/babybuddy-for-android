package eu.pkgsoftware.babybuddywidgets

import android.animation.LayoutTransition
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding

class NotesEditorLogic(
    private val activity: MainActivity,
    private val binding: NotesEditorBinding,
    private var visible: Boolean
) {
    private var id: String? = null
    private val credStore: CredStore

    private fun updateVisibility() {
        val params = binding.root.layoutParams
        params.height = if (visible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.root.layoutParams = params
    }

    init {
        credStore = activity.credStore
        updateVisibility()

        val lt = LayoutTransition()
        lt.setDuration(200)
        lt.enableTransitionType(LayoutTransition.CHANGING)
        lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
        lt.enableTransitionType(LayoutTransition.DISAPPEARING)
        //binding.getRoot().setLayoutTransition(lt); - Causes the Notes-dialog to NOT appear. Disabled for now.

        binding.noteEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                credStore.setObjectNotes(id, true, binding.noteEditor.text.toString())
            }
        })
        binding.noteEditor.onFocusChangeListener =
            OnFocusChangeListener { v: View?, hasFocus: Boolean -> credStore.storePrefs() }
    }

    fun isVisible(): Boolean {
        return visible
    }

    fun setVisible(b: Boolean) {
        visible = b
        updateTextFromCredStore()
        updateVisibility()
        if (id != null) {
            credStore.setObjectNotes(id, visible, binding.noteEditor.text.toString())
            credStore.storePrefs()
        }
    }

    private fun updateTextFromCredStore() {
        if (id != null) {
            val notes = activity.credStore.getObjectNotes(id)
            binding.noteEditor.setText(notes.note)
        }
    }

    fun setIdentifier(id: String?) {
        this.id = id
        setVisible(credStore.getObjectNotes(id).visible)
        updateTextFromCredStore()
    }

    val text: String
        get() = if (visible) {
            "" + binding.noteEditor.text
        } else {
            ""
        }

    fun clearText() {
        if (id != null) {
            credStore.setObjectNotes(id, visible, null)
            credStore.storePrefs()
        }
        binding.noteEditor.setText("")
    }
}