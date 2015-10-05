package edu.unc.web.wireless.comp635collector;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

/**
 * Created by rpdoy on 9/26/2015.
 */
public class DataCollectorDbHelper extends SQLiteOpenHelper{
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DataCollectorContract.DataEntry.TABLE_NAME + " (" +
                    DataCollectorContract.DataEntry._ID + " INTEGER PRIMARY KEY, " +
                    DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID + " TEXT," +
                    DataCollectorContract.DataEntry.COLUMN_NAME_TIME + " TEXT," +
                    DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH + " TEXT," +
                    DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE + " TEXT," +
                    DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED + " TEXT," +
                    DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DataCollectorContract.DataEntry.TABLE_NAME;


    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "DataCollector.db";

    public DataCollectorDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


}
