package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.Image;
import android.opengl.EGLExt;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.phrase.Phrase;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.pkgsoftware.babybuddywidgets.databinding.AboutFragmentBinding;

public class AboutFragment extends BaseFragment {
    public static class IconData {
        public String[] icons;
        public String title;
        public String link;

        public IconData(String icons, String title, String link) {
            this.link = link;
            this.title = title;
            this.icons = icons.split(",");
        }
    }

    private class ClickableLinkSpan extends ClickableSpan {
        private String url;

        public ClickableLinkSpan(String url) {
            this.url = url;
        }

        @Override
        public void onClick(@NonNull View view) {
            showUrlInBrowser(url);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setUnderlineText(true);
            ds.setColor(Color.BLUE);
        }
    }

    private AboutFragmentBinding binding = null;

    private static final Pattern HREF_DETECTOR_PATTERN = Pattern.compile("<a .*href=[\"']([^\"']+)[\"'][^>]*>([^<]*)</a>");

    private void filterLinksFromTextFields(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);
            if (!(v instanceof TextView)) {
                continue;
            }

            TextView tv = (TextView) v;

            String orgText = tv.getText().toString();
            Matcher matcher = HREF_DETECTOR_PATTERN.matcher(orgText);

            SpannableStringBuilder builder = null;
            int prevMatchEnd = 0;
            while (matcher.find()) {
                if (builder == null) {
                    builder = new SpannableStringBuilder();
                }

                builder.append(orgText.substring(prevMatchEnd, matcher.start()));

                String linkUrl = matcher.group(1);
                String linkText = matcher.group(2);
                int startSpanIndex = builder.length();
                builder.append(linkText);
                builder.setSpan(
                    new ClickableLinkSpan(linkUrl),
                    startSpanIndex,
                    builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                prevMatchEnd = matcher.end();
            }

            if (builder != null) {
                builder.append(orgText.substring(prevMatchEnd));

                tv.setMovementMethod(LinkMovementMethod.getInstance());
                tv.setText(builder);
            }
        }
    }

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        binding = AboutFragmentBinding.inflate(inflater);

        final boolean isNightmode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        String[] aboutIconLists = getResources().getStringArray(R.array.autostring_about_icon_iconlists);
        String[] aboutIconTitles = getResources().getStringArray(R.array.autostring_about_icon_titles);
        String[] aboutIconLinks = getResources().getStringArray(R.array.autostring_about_icon_links);

        final int minLength = Math.min(
            aboutIconLists.length, Math.min(aboutIconTitles.length, aboutIconLinks.length)
        );

        ArrayList<IconData> iconDataList = new ArrayList<>(minLength);
        for (int i = 0; i < minLength; i++) {
            iconDataList.add(new IconData(
                aboutIconLists[i],
                aboutIconTitles[i],
                aboutIconLinks[i]
            ));
        }

        final ColorStateList linkColorList = ContextCompat.getColorStateList(
            getContext(), android.R.color.holo_blue_dark
        );

        for (final IconData iconData : iconDataList) {
            LinearLayout group = new LinearLayout(getContext());
            group.setPadding(0, dpToPx(8), 0, dpToPx(8));
            group.setOrientation(LinearLayout.VERTICAL);
            group.setGravity(Gravity.START);

            LinearLayout iconsList = new LinearLayout(getContext());
            group.addView(iconsList);

            int color = android.R.color.secondary_text_light;
            if (isNightmode) {
                color = android.R.color.secondary_text_dark;
            }
            ColorStateList imageColorList = ContextCompat.getColorStateList(
                getContext(),
                color
            );

            for (String icon : iconData.icons) {
                ImageView iView = new ImageView(getContext());
                int id = getResources().getIdentifier(
                    icon, "drawable", getActivity().getPackageName()
                );

                Drawable d;
                try {
                    d = ContextCompat.getDrawable(getActivity(), id);
                } catch (Resources.NotFoundException e) {
                    continue;
                }
                iView.setImageDrawable(d);

                ImageViewCompat.setImageTintMode(iView, PorterDuff.Mode.SRC_IN);
                iView.setImageTintList(imageColorList);

                iView.setMinimumWidth(dpToPx(48));
                iView.setMinimumHeight(dpToPx(48));
                iconsList.addView(iView);
            }

            TextView tv = new TextView(getContext());
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            TextViewCompat.setTextAppearance(tv, R.style.TextAppearance_AppCompat_Body1);
            String rawText = Phrase.from(
                "Images based on or derived from works from:\n{title}"
            ).put(
                "title", iconData.title
            ).format().toString();
            SpannableString spanText = new SpannableString(rawText + "\n" + "Open creator's website");
            spanText.setSpan(
                new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        showUrlInBrowser(iconData.link);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(true);
                        ds.setColor(linkColorList.getDefaultColor());
                    }
                },
                rawText.length() + 1,
                spanText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            tv.setText(spanText);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            group.addView(tv);

            binding.media.addView(group);
        }

        filterLinksFromTextFields(binding.root);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        getMainActivity().setTitle(getResources().getString(R.string.about_page_title));
        getMainActivity().enableBackNavigationButton(true);
    }
}