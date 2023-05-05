package eu.pkgsoftware.babybuddywidgets

import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.databinding.AboutFragmentBinding
import eu.pkgsoftware.babybuddywidgets.databinding.AboutLibraryEntryBinding
import eu.pkgsoftware.babybuddywidgets.utils.ClickHandler
import eu.pkgsoftware.babybuddywidgets.utils.SpannableUtils
import eu.pkgsoftware.babybuddywidgets.widgets.FoldingText

class IconData(icons: String, var title: String, var link: String) {
    var icons: Array<String>

    init {
        this.icons = icons.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }
}


data class LibraryData(
    val title: String,
    val url: String,
    val shortText: String,
    val longText: String
) {
}

class AboutFragment : BaseFragment() {
    private var binding: AboutFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AboutFragmentBinding.inflate(inflater)

        val isNightmode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        addLibraryEntries(isNightmode)
        addIconEntries(isNightmode)

        SpannableUtils.filterLinksFromTextFields(binding!!.root, object : ClickHandler {
            override fun linkClicked(url: String) {
                showUrlInBrowser(url)
            }
        })
        return binding!!.getRoot()
    }

    private fun getString(r: String): String? {
        val _id = resources.getIdentifier(r, "string", requireActivity().packageName)
        if (_id == 0) {
            return null
        }
        return resources.getString(_id)
    }

    private fun addLibraryEntries(isNightmode: Boolean) {
        val licenseEntries = mutableListOf<LibraryData>()

        var i = 0
        while (true) {
            val title = getString("lib_license_${i}_title")
            val url = getString("lib_license_${i}_source")
            val shortText = getString("lib_license_${i}_copyright")
            val longText = getString("lib_license_${i}_full_text")
            if (arrayOf(title, url, shortText, longText).any { it == null }) {
                break;
            }
            licenseEntries.add(LibraryData(title!!, url!!, shortText!!, longText!!))
            i++
        }

        for (licenseEntry in licenseEntries) {
            val entryBinding = AboutLibraryEntryBinding.inflate(layoutInflater)
            entryBinding.caption.setText(licenseEntry.title)
            entryBinding.url.setText(
                Phrase.from("Source: {url}").put("url", licenseEntry.url).format().toString()
            )
            val folding =
                FoldingText(requireActivity(), licenseEntry.shortText, licenseEntry.longText)
            entryBinding.root.addView(folding.view)
            binding!!.libraries.addView(entryBinding.root)
        }
    }

    private fun addIconEntries(isNightmode: Boolean) {
        val aboutIconLists = resources.getStringArray(R.array.autostring_about_icon_iconlists)
        val aboutIconTitles = resources.getStringArray(R.array.autostring_about_icon_titles)
        val aboutIconLinks = resources.getStringArray(R.array.autostring_about_icon_links)
        val minLength = Math.min(
            aboutIconLists.size, Math.min(aboutIconTitles.size, aboutIconLinks.size)
        )
        val iconDataList = ArrayList<IconData>(minLength)
        for (i in 0 until minLength) {
            iconDataList.add(
                IconData(
                    aboutIconLists[i],
                    aboutIconTitles[i],
                    aboutIconLinks[i]
                )
            )
        }
        for (iconData in iconDataList) {
            createIconEntry(isNightmode, iconData)
        }
    }

    private fun createIconEntry(
        isNightmode: Boolean,
        iconData: IconData
    ) {
        val context = requireContext()
        val linkColorList = ContextCompat.getColorStateList(
            context, android.R.color.holo_blue_dark
        )
        val group = LinearLayout(context)
        group.setPadding(0, dpToPx(8f), 0, dpToPx(8f))
        group.orientation = LinearLayout.VERTICAL
        group.gravity = Gravity.START
        val iconsList = LinearLayout(context)
        group.addView(iconsList)
        var color = android.R.color.secondary_text_light
        if (isNightmode) {
            color = android.R.color.secondary_text_dark
        }
        val imageColorList = ContextCompat.getColorStateList(
            context,
            color
        )
        for (icon in iconData.icons) {
            val iView = ImageView(context)
            val id = resources.getIdentifier(
                icon, "drawable", activity!!.packageName
            )
            var d: Drawable?
            d = try {
                ContextCompat.getDrawable(activity!!, id)
            } catch (e: NotFoundException) {
                continue
            }
            iView.setImageDrawable(d)
            ImageViewCompat.setImageTintMode(iView, PorterDuff.Mode.SRC_IN)
            iView.imageTintList = imageColorList
            iView.minimumWidth = dpToPx(48f)
            iView.minimumHeight = dpToPx(48f)
            iconsList.addView(iView)
        }
        val tv = TextView(context)
        tv.movementMethod = LinkMovementMethod.getInstance()
        TextViewCompat.setTextAppearance(tv, R.style.TextAppearance_AppCompat_Body1)
        val rawText = Phrase.from(
            "Images based on or derived from works from:\n{title}"
        ).put(
            "title", iconData.title
        ).format().toString()
        val spanText = SpannableString("$rawText\nOpen creator's website")
        spanText.setSpan(
            object : ClickableSpan() {
                override fun onClick(view: View) {
                    showUrlInBrowser(iconData.link)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = true
                    ds.color = linkColorList!!.defaultColor
                }
            },
            rawText.length + 1,
            spanText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tv.text = spanText
        tv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        group.addView(tv)
        binding!!.media.addView(group)
    }

    override fun onResume() {
        super.onResume()
        mainActivity.setTitle(resources.getString(R.string.about_page_title))
        mainActivity.enableBackNavigationButton(true)
    }
}