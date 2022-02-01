package eu.pkgsoftware.babybuddywidgets;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import eu.pkgsoftware.babybuddywidgets.databinding.AboutFragmentBinding;

public class AboutFragment extends Fragment {
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

        for (IconData iconData : iconDataList) {
            TextView tv = new TextView(getContext());
            tv.setText(iconData.title);
        }

        return binding.getRoot();
    }
}