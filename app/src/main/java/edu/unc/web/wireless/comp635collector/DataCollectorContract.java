package edu.unc.web.wireless.comp635collector;
import android.provider.BaseColumns;

/**
 * Created by rpdoy on 9/26/2015.
 */
public class DataCollectorContract {

    public DataCollectorContract() {}

    public static abstract class DataEntry implements BaseColumns {
        public static final String TABLE_NAME = "data";
        public static final String COLUMN_NAME_ENTRY_ID = "entryId";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_SIGNAL_STRENGTH = "signalStrength";
        public static final String COLUMN_NAME_CONNECTION_TYPE = "connectionType";
        public static final String COLUMN_NAME_DOWNLOAD_SPEED = "downloadSpeed";
        public static final String COLUMN_NAME_UPLOAD_SPEED = "uploadSpeed";
    }
}
