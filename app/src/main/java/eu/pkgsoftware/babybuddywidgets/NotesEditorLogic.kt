package eu.pkgsoftware.babybuddywidgets

import android.animation.LayoutTransition
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import eu.pkgsoftware.babybuddywidgets.CredStore.Notes
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding

interface NotesControl {
    fun getNotes(): Notes
    fun setNotes(notes: Notes)
    fun persistChanges()
}

class NoNotesControl : NotesControl {
    override fun getNotes(): Notes {
        return CredStore.EMPTY_NOTES
    }

    override fun setNotes(notes: Notes) {
    }

    override fun persistChanges() {
    }
}

class CredStoreNotes(val id: String, val credStore: CredStore) : NotesControl {
    override fun getNotes(): Notes {
        return credStore.getObjectNotes(id)
    }

    override fun setNotes(notes: Notes) {
        credStore.setObjectNotes(id, notes.visible, notes.note)
    }

    override fun persistChanges() {
        credStore.storePrefs()
    }
}

class NotesEditorLogic(
    private val binding: NotesEditorBinding,
    private var visible: Boolean
) {
    private var notesControl: NotesControl = NoNotesControl()

    private fun updateVisibility() {
        val params = binding.root.layoutParams
        params.height = if (visible) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.root.layoutParams = params
    }

    init {
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
                notesControl.setNotes(Notes(binding.noteEditor.text.toString(), true))
            }
        })
        binding.noteEditor.onFocusChangeListener =
            OnFocusChangeListener { v: View?, hasFocus: Boolean -> notesControl.persistChanges() }
    }

    fun isVisible(): Boolean {
        return visible
    }

    fun setVisible(b: Boolean) {
        visible = b
        updateTextFromControl()
        updateVisibility()
        notesControl.setNotes(Notes(binding.noteEditor.text.toString(), visible))
        notesControl.persistChanges()
    }

    private fun updateTextFromControl() {
        val notes = notesControl.getNotes()
        binding.noteEditor.setText(notes.note)
    }

    fun setNotes(notesControl: NotesControl) {
        this.notesControl.persistChanges()
        this.notesControl = notesControl
        setVisible(notesControl.getNotes().visible)
        updateTextFromControl()
    }

    val text: String
        get() = if (visible) {
            "" + binding.noteEditor.text
        } else {
            ""
        }

    fun clearText() {
        notesControl.setNotes(CredStore.EMPTY_NOTES)
        notesControl.persistChanges()
        binding.noteEditor.setText("")
    }
}