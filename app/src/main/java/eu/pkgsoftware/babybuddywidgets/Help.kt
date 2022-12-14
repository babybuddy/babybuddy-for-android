package eu.pkgsoftware.babybuddywidgets

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import eu.pkgsoftware.babybuddywidgets.databinding.HelpFragmentBinding
import eu.pkgsoftware.babybuddywidgets.databinding.HelpPageBinding

class HelpViewHolder(val imageView: HelpPageBinding) : ViewHolder(imageView.root) {
    init {
        imageView.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    fun setText(s: String) {
        imageView.helpText.setText(s)
    }

    fun setImage(d: Drawable) {
        imageView.helpImage.setImageDrawable(d)
    }
}

class Help : BaseFragment() {
    inner class HelpAdapter : Adapter<HelpViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
            val helpPage = HelpPageBinding.inflate(this@Help.mainActivity.layoutInflater)
            return HelpViewHolder(helpPage)
        }

        private fun getString(name: String): String {
            val id = resources.getIdentifier(
                name, "string", mainActivity.packageName
            )
            return resources.getString(id);
        }

        private fun getDrawable(name: String): Drawable? {
            val drawableName = getString(name);
            val drawableId = resources.getIdentifier(
                drawableName, "drawable", mainActivity.packageName
            )
            try {
                return ResourcesCompat.getDrawable(resources, drawableId, mainActivity.theme)
            } catch (e: Resources.NotFoundException) {
                return null
            }
        }

        override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
            holder.setText(getString("help_item_1_text"))
            getDrawable("help_item_1_image")?.let {
                holder.setImage(it)
            }
        }

        override fun getItemCount(): Int {
            return 1
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = HelpFragmentBinding.inflate(inflater)
        binding.helpPager.adapter = HelpAdapter()
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle("Help");
        mainActivity.enableBackNavigationButton(true)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Help.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            Help().apply {
                arguments = Bundle().apply {
                }
            }
    }
}