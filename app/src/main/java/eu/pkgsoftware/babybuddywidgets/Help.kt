package eu.pkgsoftware.babybuddywidgets

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import eu.pkgsoftware.babybuddywidgets.databinding.HelpDepthPagerBinding
import eu.pkgsoftware.babybuddywidgets.databinding.HelpFragmentBinding
import eu.pkgsoftware.babybuddywidgets.databinding.HelpPageBinding

class HelpDepthViewHolder(val imageView: HelpPageBinding) : ViewHolder(imageView.root) {
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

class HelpMainViewHolder(val binding: HelpDepthPagerBinding) : ViewHolder(binding.root) {
    init {
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }
}

class Help : BaseFragment() {
    fun constructName(i: Int, j: Int, type: String): String {
        return "help_item_${i}_${j}_${type}"
    }

    inner class HelpDepthAdapter(val baseIndex: Int) : Adapter<HelpDepthViewHolder>() {
        private var _count = 0

        init {
            while (hasResource(_count + 1)) {
                _count++
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpDepthViewHolder {
            val helpPage = HelpPageBinding.inflate(this@Help.mainActivity.layoutInflater)
            return HelpDepthViewHolder(helpPage)
        }

        private fun hasResource(i: Int): Boolean {
            val id = resources.getIdentifier(
                constructName(baseIndex, i, "text"), "string", mainActivity.packageName
            )
            return id != 0
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
                return ResourcesCompat.getDrawableForDensity(
                    resources, drawableId, DisplayMetrics.DENSITY_XXXHIGH, null
                )
            } catch (e: Resources.NotFoundException) {
                return null
            }
        }

        override fun onBindViewHolder(holder: HelpDepthViewHolder, position: Int) {
            holder.setText(getString(constructName(baseIndex, position + 1, "text")))
            getDrawable(constructName(baseIndex, position + 1, "image"))?.let {
                holder.setImage(it)
            }
        }

        override fun getItemCount(): Int {
            return _count
        }
    }

    inner class HelpMainAdapter() : Adapter<HelpMainViewHolder>() {
        private var _count = 0

        init {
            while (hasResource(_count + 1)) {
                _count++
            }
        }

        private fun hasResource(i: Int): Boolean {
            val id = resources.getIdentifier(
                constructName(i, 1,"text"), "string", mainActivity.packageName
            )
            return id != 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpMainViewHolder {
            return HelpMainViewHolder(HelpDepthPagerBinding.inflate(layoutInflater))
        }

        override fun onBindViewHolder(holder: HelpMainViewHolder, position: Int) {
            holder.binding.helpDepthPager.adapter = HelpDepthAdapter(position + 1)
            holder.binding.helpDepthPager.currentItem = 0
        }

        override fun getItemCount(): Int {
            return _count
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = HelpFragmentBinding.inflate(inflater)
        binding.helpPager.adapter = HelpMainAdapter()
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