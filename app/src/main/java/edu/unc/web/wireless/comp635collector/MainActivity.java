package edu.unc.web.wireless.comp635collector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
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
    private int threeGSignalStrength = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            public void onCallForwardingIndicatorChanged(boolean cfi) {}
            public void onCallStateChanged(int state, String incomingNumber) {}
            public void onCellLocationChanged(CellLocation location) {}
            public void onDataActivity(int direction) {}
            public void onDataConnectionStateChanged(int state) {}
            public void onMessageWaitingIndicatorChanged(boolean mwi) {}
            public void onServiceStateChanged(ServiceState serviceState) {}
            //Deprecated
            //public void onSignalStrengthChanged(int asu)
            public void onSignalStrengthsChanged (SignalStrength signalStrength)
            {
                threeGSignalStrength = signalStrength.getEvdoDbm();
                /*Context context = getApplicationContext();
                CharSequence text = "Signal strength changed: " + signalStrength.getEvdoDbm();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();*/
            }
        };

        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR |
                        PhoneStateListener.LISTEN_CALL_STATE |
                        PhoneStateListener.LISTEN_CELL_LOCATION |
                        PhoneStateListener.LISTEN_DATA_ACTIVITY |
                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                        PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR |
                        PhoneStateListener.LISTEN_SERVICE_STATE |
                        PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

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

    /*  try {
            getDownloadSpeed();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    /*
        try {
            getUploadSpeed();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    */
       getSpeeds();

    }

    private String getTime() {
        SimpleDateFormat df = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss z");
        return df.format(Calendar.getInstance().getTime());
    }

    private String getSignalStrength() {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(getApplicationContext().TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

        if (((TextView) findViewById(R.id.connectionTypeTextView)).getText().toString().equals("3G")) {
            return Integer.toString(threeGSignalStrength);
        }

        for (int i = 0; i < cellInfoList.size(); i++) {
            CellInfo ci = cellInfoList.get(i);

            if (ci instanceof CellInfoCdma) {
                CellInfoCdma cdmaCellInfo = (CellInfoCdma)ci;
                CellSignalStrengthCdma cdmaSignalStrength = cdmaCellInfo.getCellSignalStrength();
                return Integer.toString(cdmaSignalStrength.getDbm());
            }
            else if (ci instanceof CellInfoLte) {
                CellInfoLte lteCellInfo = (CellInfoLte)ci;
                CellSignalStrengthLte lteSignalStrength = lteCellInfo.getCellSignalStrength();
               return Integer.toString(lteSignalStrength.getDbm());
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
        DownloadFromDropbox downloadFile = new DownloadFromDropbox(this, mDBApi, "/test.mp3", Environment.getExternalStorageDirectory().getPath() + "/test.mp3");
        downloadFile.execute();
    }

    public void getUploadSpeed() throws ExecutionException, InterruptedException {
        File sdCard = Environment.getExternalStorageDirectory();
        File uploadFile = new File(sdCard, "test.mp3");
        UploadToDropbox upload = new UploadToDropbox(this, mDBApi, "/", uploadFile);
        upload.execute();
    }

    public void getDownloadSpeed() throws ExecutionException, InterruptedException {
        DownloadFromDropbox downloadFile = new DownloadFromDropbox(this, mDBApi, "/test.mp3", Environment.getExternalStorageDirectory().getPath() + "/test.mp3");
        downloadFile.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    DialogInterface.OnClickListener clearDatabaseDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("delete from " + DataCollectorContract.DataEntry.TABLE_NAME);
                    db.close();

                    Context context = getApplicationContext();
                    CharSequence text = "Database cleared. Save next entry twice.";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.clear_database) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure?").setPositiveButton("Yes", clearDatabaseDialogClickListener)
                    .setNegativeButton("No", clearDatabaseDialogClickListener).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    View.OnClickListener saveButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            TextView idTextView = (EditText) findViewById(R.id.idEditText);
            TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
            TextView signalStrengthTextView = (TextView) findViewById(R.id.signalStrengthTextView);
            TextView connectionTypeTextView = (TextView) findViewById(R.id.connectionTypeTextView);
            TextView downloadSpeedTextView = (TextView) findViewById(R.id.downloadSpeedTextView);
            TextView uploadSpeedTextView = (TextView) findViewById(R.id.uploadSpeedTextView);
            EditText noteEditText = (EditText) findViewById(R.id.noteEditText);

            DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID, idTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_TIME, timeTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH, signalStrengthTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE, connectionTypeTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED, downloadSpeedTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED, uploadSpeedTextView.getText().toString());
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_NOTE, noteEditText.getText().toString());

            db.insert(
                    DataCollectorContract.DataEntry.TABLE_NAME,
                    "null",
                    values);

            db.close();

            Context context = getApplicationContext();
            CharSequence text = "Data Saved Successfully!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
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
                    DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED,
                    DataCollectorContract.DataEntry.COLUMN_NAME_NOTE
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

            SimpleDateFormat df = new SimpleDateFormat("dMMMyyyyHHmmss");
            String date = df.format(Calendar.getInstance().getTime());

            File sdCard = Environment.getExternalStorageDirectory();
            File exportFile = new File(sdCard, "COMP635-Exported-" + date + ".csv");

            try {
                FileWriter fw = new FileWriter(exportFile);
                fw.append("HAI");

                String headerString = DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_TIME + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED + "," +
                                        DataCollectorContract.DataEntry.COLUMN_NAME_NOTE + "\n";

                fw.append(headerString);

                c.moveToFirst();

                while (c.moveToNext()) {
                    String id = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID));
                    String time = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_TIME));
                    String signalStrength = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_SIGNAL_STRENGTH));
                    String connectionType = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_CONNECTION_TYPE));
                    String downloadSpeed = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_DOWNLOAD_SPEED));
                    String uploadSpeed = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_UPLOAD_SPEED));
                    String note = c.getString(c.getColumnIndex(DataCollectorContract.DataEntry.COLUMN_NAME_NOTE));

                    String line = id + "," + time + "," + signalStrength + "," + connectionType + "," + downloadSpeed + "," +
                                    uploadSpeed + "," + note + "\n";

                    fw.append(line);
                }

                fw.flush();
                fw.close();
            } catch (Exception e) {
                db.close();
                Context context = getApplicationContext();
                CharSequence text = "Export Failed :(";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }

            db.close();
            Context context = getApplicationContext();
            CharSequence text = "Data Exported Successfully!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    };
}
