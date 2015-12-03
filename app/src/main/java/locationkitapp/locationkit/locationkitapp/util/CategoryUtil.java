package locationkitapp.locationkit.locationkitapp.util;

import android.util.Log;

import java.util.HashMap;

import locationkitapp.locationkit.locationkitapp.R;

/**
 * Created by johnfontaine on 12/1/15.
 */
public class CategoryUtil {
    private static final String LOG_TAG = "CategoryUtil";
    private static final HashMap<String,Integer> map = new HashMap<>();
    static {
        map.put("automotive", R.drawable.ic_automotive);
        map.put("bar", R.drawable.ic_bar_nightlife);
        map.put("nightlife", R.drawable.ic_bar_nightlife);
        map.put("coffee", R.drawable.ic_coffee_tea);
        map.put("tea", R.drawable.ic_coffee_tea);
        map.put("community", R.drawable.ic_community_public_services);
        map.put("public_services", R.drawable.ic_community_public_services);
        map.put("education", R.drawable.ic_education);
        map.put("entertainment", R.drawable.ic_education);
        map.put("financial", R.drawable.ic_financial);
        map.put("fitness", R.drawable.ic_fitness_sports_recreation);
        map.put("sports", R.drawable.ic_fitness_sports_recreation);
        map.put("recreation", R.drawable.ic_fitness_sports_recreation);
        map.put("grocery", R.drawable.ic_grocery);
        map.put("home", R.drawable.ic_home_garden);
        map.put("garden", R.drawable.ic_home_garden);
        map.put("legal", R.drawable.ic_legal);
        map.put("medical", R.drawable.ic_medical);
        map.put("organizations", R.drawable.ic_organizations_associations);
        map.put("associations", R.drawable.ic_organizations_associations);
        map.put("personal_care_services", R.drawable.ic_personal_care_services);
        map.put("professional", R.drawable.ic_professional);
        map.put("restaurants", R.drawable.ic_restuarants);
        map.put("retail", R.drawable.ic_retail);
        map.put("travel", R.drawable.ic_travel);
        map.put("unknown", R.drawable.ic_unknown);
    }


    public static int getIconForCategorySubcategory(String category, String subcategory) {
        if (category == null) {
                return map.get("unknown");
        }
        if (map.containsKey(category.toLowerCase())) {
            Log.v(LOG_TAG, "category matched exact");
            return map.get(category.toLowerCase());
        }
        if (category.contains(" ")) {
            String[] words = category.toLowerCase().split(" ");
            for (String word : words) {

                if (map.containsKey(word)) {
                    Log.v(LOG_TAG, String.format("category matched word %s", word));
                    return map.get(word);
                }
            }
        }
        Log.v(LOG_TAG, String.format("category is unknown %s subcategory %s", category, subcategory));
        return map.get("unknown");
    }
}
