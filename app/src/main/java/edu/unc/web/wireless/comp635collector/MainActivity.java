package edu.unc.web.wireless.comp635collector;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        Button readButton = (Button) findViewById(R.id.readButton);
        saveButton.setOnClickListener(saveButtonHandler);
        readButton.setOnClickListener(readButtonHandler);
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
            EditText idText = (EditText) findViewById(R.id.noteEditText);

            DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID, idText.getText().toString());

            long newRowId;
            newRowId = db.insert(
                    DataCollectorContract.DataEntry.TABLE_NAME,
                    "null",
                    values);

            TelephonyManager telephonyManager = (TelephonyManager)getSystemService(getApplicationContext().TELEPHONY_SERVICE);
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

            for (int i = 0; i < cellInfoList.size(); i++) {
                CellInfo ci = cellInfoList.get(i);

                if (ci instanceof CellInfoCdma) {
                    CellInfoCdma cdmaCellInfo = (CellInfoCdma)ci;
                    CellSignalStrengthCdma cdmaSignalStrength = cdmaCellInfo.getCellSignalStrength();
                    System.out.println("RYAN CDMA: " + Integer.toString(cdmaSignalStrength.getDbm()));
                }
                else if (ci instanceof CellInfoLte) {
                    CellInfoLte lteCellInfo = (CellInfoLte)ci;
                    CellSignalStrengthLte lteSignalStrength = lteCellInfo.getCellSignalStrength();
                    System.out.println("RYAN LTE: " + Integer.toString(lteSignalStrength.getDbm()));
                }
                else{
                    System.out.println("RYAN: bad CellInfo!!!!");
                }
            }
        }
    };

    View.OnClickListener readButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            DataCollectorDbHelper dbHelper = new DataCollectorDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String[] projection = {
                    DataCollectorContract.DataEntry._ID,
                    DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID
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
                Toast.makeText(getApplicationContext(), c.getString(c.getColumnIndexOrThrow(DataCollectorContract.DataEntry.COLUMN_NAME_ENTRY_ID)), Toast.LENGTH_LONG).show();
            }
        }
    };
}
