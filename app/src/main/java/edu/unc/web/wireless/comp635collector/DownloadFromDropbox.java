package edu.unc.web.wireless.comp635collector;

import android.app.Activity;
import android.content.Context;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by alexjp on 9/28/15.
 */
public class DownloadFromDropbox extends AsyncTask<Void, Long, Boolean> {

    private DropboxAPI<?> mApi;
    private String mFilePath;
    private File mFile;

    private Context mContext;
    private Activity mActivity;

    private String mErrorMsg;

    private double BeforeTime;
    private double TotalRxBeforeTest;

    public DownloadFromDropbox(Context context, DropboxAPI<?> api, String downloadFile, String localFile) {
        mContext = context;
        mActivity = (Activity) mContext;
        mApi = api;
        mFilePath = downloadFile;
        mFile = new File(localFile);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            FileOutputStream outputStream = new FileOutputStream(mFile);
            mApi.getFile(mFilePath, null, outputStream, null);
            return true;

        } catch (FileNotFoundException e) {
            mErrorMsg = "The file wasn't found";
        } catch (DropboxException e) {
            mErrorMsg = "Unknown error";
        }
        return false;
    }

    @Override
    protected void onPreExecute() {
        BeforeTime = System.currentTimeMillis();
        TotalRxBeforeTest = TrafficStats.getTotalRxBytes();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            long TotalRxAfterTest = TrafficStats.getTotalRxBytes();
            long AfterTime = System.currentTimeMillis();

            double TimeDifference = AfterTime - BeforeTime;
            double rxDiff = TotalRxAfterTest - TotalRxBeforeTest;

            double downloadSpeed = (rxDiff / (TimeDifference/1000) / 1000000 * 8);
            ((TextView) mActivity.findViewById(R.id.downloadSpeedTextView)).setText((Double.toString(downloadSpeed)));
            showToast("Successfully downloaded");
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
