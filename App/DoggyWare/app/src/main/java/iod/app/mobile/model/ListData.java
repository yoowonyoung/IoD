package iod.app.mobile.model;

import java.text.Collator;
import java.util.Comparator;

/**
 * Created by Kim on 2015-11-17.
 */
public class ListData {
    public String mModuleName;
    public String mModuleStatus;

    public static final Comparator<ListData> ALPHA_COMPARATOR = new Comparator<ListData>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ListData mListDate_1, ListData mListDate_2) {
            return sCollator.compare(mListDate_1.mModuleName, mListDate_2.mModuleName);
        }
    };

    public ListData(String moduleName, String moduleStatus){
        this.mModuleName = moduleName;
        this.mModuleStatus = moduleStatus;
    }
}
