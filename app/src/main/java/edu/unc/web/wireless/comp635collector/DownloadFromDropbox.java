package edu.unc.web.wireless.comp635collector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
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
    protected ProgressDialog mDialog;

    private String mErrorMsg;

    private double BeforeTime;
    private double TotalRxBeforeTest;

    public DownloadFromDropbox(Context context, DropboxAPI<?> api, String downloadFile, String localFile) {
        mContext = context;
        mActivity = (Activity) mContext;
        mApi = api;
        mFilePath = downloadFile;
        mFile = new File(localFile);

        mDialog = new ProgressDialog(context);
        mDialog.setMax(100);
        mDialog.setMessage("Downloading " + mFile.getName());
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            FileOutputStream outputStream = new FileOutputStream(mFile);
            mApi.getFile(mFilePath, null, outputStream,
                    new ProgressListener() {
                        @Override
                        public long progressInterval() {
                            // Update the progress bar every half-second or so
                            return 500;
                        }

                        @Override
                        public void onProgress(long bytes, long total) {
                            publishProgress(bytes, total);
                        }
                    });
            return true;

        } catch (FileNotFoundException e) {
            mErrorMsg = "The file wasn't found";
        } catch (DropboxException e) {
            mErrorMsg = "Unknown error";
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
        int percent = (int) (100.0 * (double) progress[0] / progress[1] + 0.5);
        mDialog.setProgress(percent);
    }

    @Override
    protected void onPreExecute() {
        BeforeTime = System.currentTimeMillis();
        TotalRxBeforeTest = TrafficStats.getTotalRxBytes();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            long TotalRxAfterTest = TrafficStats.getTotalRxBytes();
            long AfterTime = System.currentTimeMillis();

            double TimeDifference = AfterTime - BeforeTime;
            double rxDiff = TotalRxAfterTest - TotalRxBeforeTest;

            double downloadSpeed = (rxDiff / (TimeDifference/1000) / 1000000 * 8);

            ((TextView) mActivity.findViewById(R.id.downloadSpeedTextView)).setText(String.format("%1$,.2f", downloadSpeed));
            showToast("Successfully downloaded");

            File sdCard = Environment.getExternalStorageDirectory();
            File uploadFile = new File(sdCard, "VMware.exe");
            UploadToDropbox upload = new UploadToDropbox(mContext, mApi, "/", uploadFile);
            upload.execute();
        } else {
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
