package checkin.alf.io.alfiocheckin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ButtonRectangle;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.otto.Subscribe;

import checkin.alf.io.alfiocheckin.event.CheckInFailure;
import checkin.alf.io.alfiocheckin.event.CheckInSuccess;
import checkin.alf.io.alfiocheckin.event.FetchCSRFTokenFailure;
import checkin.alf.io.alfiocheckin.event.FetchCSRFTokenSuccess;
import checkin.alf.io.alfiocheckin.event.FetchTicketFailure;
import checkin.alf.io.alfiocheckin.event.FetchTicketSuccess;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;
import checkin.alf.io.alfiocheckin.model.CsrfAndEventId;
import checkin.alf.io.alfiocheckin.model.TicketContainer;
import checkin.alf.io.alfiocheckin.service.CheckInService;


public class MainActivity extends ActionBarActivity {


    private CheckInService checkInService;
    private ActionBar actionBar;

    //TODO: GRUUUUUIK, cleanup, find a better way :D
    private volatile CsrfAndEventId csrfAndEventId;
    //

    private AppConfiguration getConf() {
        SharedPreferences pref = getSharedPreferences("alfio", Context.MODE_PRIVATE);
        return Common.GSON.fromJson(pref.getString("alfio", null), AppConfiguration.class);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Common.BUS.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Common.BUS.unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.actionBar = getSupportActionBar();
        this.checkInService = new CheckInService(Common.BUS);

        AppConfiguration conf = updateActionBarTitle();

        final View load = findViewById(R.id.login_load);
        final ButtonRectangle scan = (ButtonRectangle) findViewById(R.id.barcode_scanner);

        scan.setVisibility(View.INVISIBLE);
        load.setVisibility(View.INVISIBLE);
        findViewById(R.id.cardTicketDetail).setVisibility(View.INVISIBLE);

        scan.setOnClickListener(new TriggerScanListener());

        if (conf != null && conf.getCurrentConfiguration() != null) {
            connect(conf);
        }

        ButtonFlat cancel = (ButtonFlat) findViewById(R.id.cardCancelButton);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.cardTicketDetail).setVisibility(View.INVISIBLE);
                findViewById(R.id.login_load).setVisibility(View.INVISIBLE);
            }
        });

        ButtonFlat checkIn = (ButtonFlat) findViewById(R.id.cardCheckIn);

    }

    private void connect(AppConfiguration conf) {
        if (conf == null || conf.getCurrentConfiguration() == null) {
            return;
        }

        final View load = findViewById(R.id.login_load);
        final View scan = findViewById(R.id.barcode_scanner);
        load.setVisibility(View.VISIBLE);
        checkInService.fetchCSRFTokenAndEventId(conf.getCurrentConfiguration());
    }

    @Subscribe
    public void onFetchCSRFTokenSuccess(final FetchCSRFTokenSuccess success) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.csrfAndEventId = success.csrfAndEventId;
                findViewById(R.id.barcode_scanner).setVisibility(View.VISIBLE);
                findViewById(R.id.login_load).setVisibility(View.INVISIBLE);

                Context context = getApplicationContext();
                CharSequence text = "Connected with success";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    @Subscribe
    public void onFetchCSRFTokenFailure(FetchCSRFTokenFailure failure) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.login_load).setVisibility(View.INVISIBLE);
                Context context = getApplicationContext();
                CharSequence text = "Error while doing login.";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    private AppConfiguration updateActionBarTitle() {
        AppConfiguration conf = getConf();
        String title = "Select a event to check in!";
        if (conf != null && conf.getCurrentConfiguration() != null) {
            title = "Check in for " + conf.selectedInstance;
        }
        actionBar.setTitle(title);
        return conf;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == 1) {
            AppConfiguration conf = updateActionBarTitle();

            if (conf != null && conf.getCurrentConfiguration() != null) {
                connect(conf);
            }

        } else {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null && scanResult.getContents() != null) {

                AppConfiguration conf = getConf();
                final View load = findViewById(R.id.login_load);
                final Runnable showError = getShowErrorToast();
                load.setVisibility(View.VISIBLE);
                String parsedCode = scanResult.getContents();
                Log.i("parsed code is ", parsedCode);
                checkInService.getTicket(conf.getCurrentConfiguration(), this.csrfAndEventId, parsedCode);
            }
        }
    }

    @Subscribe
    public void onFetchTicketSuccess(FetchTicketSuccess fetchTicketSuccess) {
        Runnable showError = getShowErrorToast();
        try {
            TicketContainer tc = fetchTicketSuccess.tc;
            if (tc == null) {
                MainActivity.this.runOnUiThread(showError);
            } else {
                MainActivity.this.runOnUiThread(new DisplayTicket(tc, fetchTicketSuccess.code));
            }
        } catch (Throwable t) {
            MainActivity.this.runOnUiThread(showError);
        }
    }

    @Subscribe
    public void onFetchTicketFailure(FetchTicketFailure fetchTicketFailure) {
        Runnable showError = getShowErrorToast();
        MainActivity.this.runOnUiThread(showError);
    }

    private Runnable getShowErrorToast() {
        return new Runnable() {
            @Override
            public void run() {
                View load = findViewById(R.id.login_load);
                Context context = getApplicationContext();
                CharSequence text = "Error while fetching ticket info. Maybe you should do a reconnect";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                load.setVisibility(View.INVISIBLE);
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //
        int id = item.getItemId();
        //
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else if (id == R.id.action_reconnect) {
            connect(getConf());
        }

        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onCheckInSuccess(CheckInSuccess checkInSuccess) {
        Runnable showError = getShowErrorToast();
        if (checkInSuccess.tc == null) {
            MainActivity.this.runOnUiThread(showError);
        } else {
            MainActivity.this.runOnUiThread(new DisplayTicket(checkInSuccess.tc, checkInSuccess.code));
        }
    }

    @Subscribe
    public void onCheckInFailure(CheckInFailure checkInFailure) {
        Runnable showError = getShowErrorToast();
        MainActivity.this.runOnUiThread(showError);
    }

    public class DisplayTicket implements Runnable {

        private final TicketContainer tc;
        private final String parsedCode;

        DisplayTicket(TicketContainer tc, String parsedCode) {
            this.tc = tc;
            this.parsedCode = parsedCode;
        }

        @Override
        public void run() {
            final View load = findViewById(R.id.login_load);
            load.setVisibility(View.INVISIBLE);
            View ticketCard = findViewById(R.id.cardTicketDetail);
            ticketCard.setVisibility(View.VISIBLE);
            final Runnable showError = getShowErrorToast();

            ButtonFlat checkIn = (ButtonFlat) findViewById(R.id.cardCheckIn);
            checkIn.setText("Check In");

            if ("OK_READY_TO_BE_CHECKED_IN".equals(tc.result.status)) {
                checkIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load.setVisibility(View.VISIBLE);
                        AppConfiguration conf = getConf();
                        checkInService.checkIn(conf.getCurrentConfiguration(), MainActivity.this.csrfAndEventId, parsedCode);
                    }
                });

                checkIn.setVisibility(View.VISIBLE);
            } else if ("SUCCESS".equals(tc.result.status)) {
                checkIn.setText("Next");
                checkIn.setOnClickListener(new TriggerScanListener());
            } else {
                findViewById(R.id.cardCheckIn).setVisibility(View.INVISIBLE);
            }

            setText(R.id.ticketStatus, tc.result.status);
            setText(R.id.ticketMessage, tc.result.message);
            setText(R.id.ticketFullName, tc.ticket.fullName);
            setText(R.id.ticketEmail, tc.ticket.email);
            setText(R.id.ticketCompany, tc.ticket.company);
            setText(R.id.ticketNotes, tc.ticket.notes);
        }
    }

    private void setText(int id, String value) {
        TextView txt = (TextView) findViewById(id);
        txt.setText(value);
    }

    private class TriggerScanListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            findViewById(R.id.cardTicketDetail).setVisibility(View.INVISIBLE);
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setResultDisplayDuration(0);
            integrator.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            integrator.setPrompt(null);
            integrator.initiateScan();
        }
    }
}
