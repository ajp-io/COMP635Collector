package edu.unc.web.wireless.comp635collector;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {

    final static private String APP_KEY = "qielk8mjvk131u9";
    final static private String APP_SECRET = "d4g8n9hu8rune6d";
    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonHandler);

        Button exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(exportButtonHandler);

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<>(session);
        mDBApi.getSession().startOAuth2Authentication(getApplicationContext());

        Button collectButton = (Button) findViewById(R.id.collectButton);
        collectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                collectData();
            }
        });
    }

    private void collectData() {
        ((TextView) findViewById(R.id.timeTextView)).setText(getTime());
        ((TextView) findViewById(R.id.connectionTypeTextView)).setText(getNetworkClass(getApplicationContext()));
        ((TextView) findViewById(R.id.signalStrengthTextView)).setText(getSignalStrength());
/*
        try {
            getDownloadSpeed();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    */
        try {
            getUploadSpeed();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //getSpeeds();

    }

    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss, z");
        return df.format(Calendar.getInstance().getTime());
    }

    private String getSignalStrength() {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

        for (int i = 0; i < cellInfoList.size(); i++) {
            CellInfo ci = cellInfoList.get(i);

            if (ci instanceof CellInfoCdma) {
                CellInfoCdma cdmaCellInfo = (CellInfoCdma)ci;
                CellSignalStrengthCdma cdmaSignalStrength = cdmaCellInfo.getCellSignalStrength();
                return Integer.toString(cdmaSignalStrength.getDbm()) + "dBm";
            }
            else if (ci instanceof CellInfoLte) {
                CellInfoLte lteCellInfo = (CellInfoLte)ci;
                CellSignalStrengthLte lteSignalStrength = lteCellInfo.getCellSignalStrength();
               return Integer.toString(lteSignalStrength.getDbm()) + "dBm";
            }
            else{
                return "Error: bad cell info";
            }
        }

        return "Error: no cell info";
    }

    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    public static String getNetworkClass(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info==null || !info.isConnected())
            return "-"; //not connected
        if(info.getType() == ConnectivityManager.TYPE_WIFI)
            return "WIFI";
        if(info.getType() == ConnectivityManager.TYPE_MOBILE){
            int networkType = info.getSubtype();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                    return "4G";
                default:
                    return "?";
            }
        }
        return "?";
    }

    public void getSpeeds() {
        File sdCard = Environment.getExternalStorageDirectory();
        File uploadFile = new File(sdCard, "VMware.exe");
        UploadToDropbox upload = new UploadToDropbox(this, mDBApi, "/", uploadFile);
        upload.execute();
    }

    public void getUploadSpeed() throws ExecutionException, InterruptedException {
        File sdCard = Environment.getExternalStorageDirectory();
        File uploadFile = new File(sdCard, "VMware.exe");
        UploadToDropbox upload = new UploadToDropbox(this, mDBApi, "/", uploadFile);
        upload.execute();
    }

    public void getDownloadSpeed() throws ExecutionException, InterruptedException {
        DownloadFromDropbox downloadFile = new DownloadFromDropbox(this, mDBApi, "/VMware.exe", Environment.getExternalStorageDirectory().getPath() + "/VMware.exe");
        downloadFile.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    View.OnClickListener saveButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            TextView idTextView = (TextView) findViewById(R.id.noteEditText);
            TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
            TextView signalStrengthTextView = (TextView) findViewById(R.id.signalStrengthTextView);
            TextView connectionTypeTextView = (TextView) findViewById(R.id.connectionTypeTextView);
            TextView downloadSpeedTextView = (TextView) findViewById(R.id.downloadSpeedTextView);
            TextView uploadSpeedTextView = (TextView) findViewById(R.id.uploadSpeedTextView);

            DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID, idTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_TIME, timeTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH, signalStrengthTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE, connectionTypeTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED, downloadSpeedTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED, uploadSpeedTextView.getText().toString());

            db.insert(
                    DataCollectorContract.DataEntry.TABLE_NAME,
                    "null",
                    values);
        }
    };

    View.OnClickListener exportButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String[] projection = {
                    DataCollectorContract.DataEntry._ID,
                    DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID,
                    DataCollectorContract.DataEntry.COLUMN_NAME_TIME,
                    DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH,
                    DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE,
                    DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED,
                    DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED
            };

            String sortOrder = DataCollectorContract.DataEntry._ID + " ASC";

            Cursor c = db.query(
                    DataCollectorContract.DataEntry.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );

            c.moveToFirst();

            while (c.moveToNext()) {
                // write to csv file here
            }
        }
    };
}
