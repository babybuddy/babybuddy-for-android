package eu.pkgsoftware.babybuddywidgets;

import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.Image;
import android.os.Bundle;
import android.text.SpannableString;
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

import java.util.ArrayList;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
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

    private int dpToPx(float dp) {
        return (int) (getContext().getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        AboutFragmentBinding binding = AboutFragmentBinding.inflate(inflater);

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

        for (final IconData iconData : iconDataList) {
            LinearLayout group = new LinearLayout(getContext());
            group.setPadding(0, dpToPx(8), 0, dpToPx(8));
            group.setOrientation(LinearLayout.VERTICAL);
            group.setGravity(Gravity.START);

            LinearLayout iconsList = new LinearLayout(getContext());
            group.addView(iconsList);

            for (String icon : iconData.icons) {
                ImageView iView = new ImageView(getContext());
                int id = getResources().getIdentifier(icon, "drawable", getActivity().getPackageName());
                iView.setImageDrawable(ContextCompat.getDrawable(getActivity(), id));
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
                        ds.setColor(Color.BLUE);
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

            binding.root.addView(group);
        }

        return binding.root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getMainActivity().setTitle(getResources().getString(R.string.about_page_title));
    }
}