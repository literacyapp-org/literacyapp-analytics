package org.literacyapp.analytics;

import android.Manifest;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.literacyapp.analytics.service.ScreenshotJobService;
import org.literacyapp.analytics.util.RootHelper;

public class MainActivity extends Activity {

    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private Button mMainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mMainButton = (Button) findViewById(R.id.main_button);
    }

    @Override
    protected void onStart() {
        Log.i(getClass().getName(), "onStart");
        super.onStart();

        // Ask for permissions
        int permissionCheckWriteExternalStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        mMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(getClass().getName(), "mMainButton onClick");

                // Request root access
                boolean isRootPermissionGranted = RootHelper.runAsRoot(new String[] {
                        "echo \"Do I have root?\" >/system/sd/temporary.txt\n",
                        "exit\n"
                });
                Log.i(getClass().getName(), "isRootPermissionGranted: " + isRootPermissionGranted);
                if (!isRootPermissionGranted) {
                    Toast.makeText(getApplicationContext(), "Root permission was not granted. Please see log for details.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Root permission was granted. Starting background job...", Toast.LENGTH_LONG).show();

                    // Initiate background job for recording screenshots
                    ComponentName componentName = new ComponentName(getApplicationContext(), ScreenshotJobService.class);
                    JobInfo.Builder builder = new JobInfo.Builder(0, componentName);
                    builder.setPeriodic(5 * 60 * 1000); // Every 5 minutes
                    JobInfo screenshotJobInfo = builder.build();

                    JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.schedule(screenshotJobInfo);
                }

                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted

                // Restart application
                Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                // Permission denied

                // Close application
                finish();
            }
        }
    }
}