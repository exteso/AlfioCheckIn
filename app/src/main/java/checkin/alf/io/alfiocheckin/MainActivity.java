package checkin.alf.io.alfiocheckin;

import android.content.Context;
import android.content.Intent;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
import checkin.alf.io.alfiocheckin.event.CheckInFailure;
import checkin.alf.io.alfiocheckin.event.CheckInSuccess;
import checkin.alf.io.alfiocheckin.event.FetchEventIdFailure;
import checkin.alf.io.alfiocheckin.event.FetchEventIdSuccess;
import checkin.alf.io.alfiocheckin.event.FetchTicketFailure;
import checkin.alf.io.alfiocheckin.event.FetchTicketSuccess;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;
import checkin.alf.io.alfiocheckin.model.TicketContainer;
import checkin.alf.io.alfiocheckin.service.CheckInService;
import checkin.alf.io.alfiocheckin.service.DataService;


public class MainActivity extends ActionBarActivity {

    //
    @InjectView(R.id.login_load)
    View load;
    @InjectView(R.id.barcode_scanner)
    ButtonRectangle scan;
    @InjectView(R.id.cardTicketDetail)
    View cardTicketDetail;
    @InjectView(R.id.cardCancelButton)
    ButtonFlat cancel;
    @InjectView(R.id.cardCheckIn)
    ButtonFlat checkIn;
    @InjectView(R.id.ticketStatus)
    TextView ticketStatus;
    @InjectView(R.id.ticketMessage)
    TextView ticketMessage;
    @InjectView(R.id.ticketFullName)
    TextView ticketFullName;
    @InjectView(R.id.ticketEmail)
    TextView ticketEmail;
    @InjectView(R.id.ticketCompany)
    TextView ticketCompany;
    @InjectView(R.id.ticketNotes)
    TextView ticketNotes;
    private DataService dataService;
    private CheckInService checkInService;
    private ActionBar actionBar;
    //

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

        ButterKnife.inject(this);

        this.actionBar = getSupportActionBar();
        this.checkInService = new CheckInService(Common.BUS);
        this.dataService = new DataService(this);

        AppConfiguration conf = updateActionBarTitle();


        scan.setVisibility(View.INVISIBLE);
        load.setVisibility(View.INVISIBLE);
        cardTicketDetail.setVisibility(View.INVISIBLE);

        scan.setOnClickListener(new TriggerScanListener());

        if (conf != null && conf.getCurrentConfiguration() != null) {
            connect(conf);
        }

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.cardTicketDetail).setVisibility(View.INVISIBLE);
                findViewById(R.id.login_load).setVisibility(View.INVISIBLE);
            }
        });


    }

    private void connect(AppConfiguration conf) {
        if (conf == null || conf.getCurrentConfiguration() == null) {
            return;
        }
        load.setVisibility(View.VISIBLE);
        checkInService.fetchEventId(conf.getCurrentConfiguration());
    }

    @Subscribe
    public void onFetchEventIdSuccess(final FetchEventIdSuccess success) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataService.saveCsrfAndEventId(success.eventId);
                scan.setVisibility(View.VISIBLE);
                load.setVisibility(View.INVISIBLE);

                Context context = getApplicationContext();
                CharSequence text = "Connected with success";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    @Subscribe
    public void onFetchEventIdFailure(FetchEventIdFailure failure) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                load.setVisibility(View.INVISIBLE);
                Context context = getApplicationContext();
                CharSequence text = "Error while doing login.";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    private AppConfiguration updateActionBarTitle() {
        AppConfiguration conf = dataService.getAppConfiguration();
        String title = "Select an event to check in!";
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

                AppConfiguration conf = dataService.getAppConfiguration();
                load.setVisibility(View.VISIBLE);
                String parsedCode = scanResult.getContents();
                Log.i("parsed code is ", parsedCode);
                checkInService.getTicket(conf.getCurrentConfiguration(), dataService.getEventId(), parsedCode);
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
            connect(dataService.getAppConfiguration());
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

            load.setVisibility(View.INVISIBLE);
            cardTicketDetail.setVisibility(View.VISIBLE);

            checkIn.setText("Check In");

            if ("OK_READY_TO_BE_CHECKED_IN".equals(tc.result.status)) {
                checkIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load.setVisibility(View.VISIBLE);
                        AppConfiguration conf = dataService.getAppConfiguration();
                        checkInService.checkIn(conf.getCurrentConfiguration(), dataService.getEventId(), parsedCode);
                    }
                });

                checkIn.setVisibility(View.VISIBLE);
            } else if ("SUCCESS".equals(tc.result.status)) {
                checkIn.setText("Next");
                checkIn.setOnClickListener(new TriggerScanListener());
            } else {
                checkIn.setVisibility(View.INVISIBLE);
            }

            ticketStatus.setText(tc.result.status);
            ticketMessage.setText(tc.result.message);
            ticketFullName.setText(tc.ticket.fullName);
            ticketEmail.setText(tc.ticket.email);
            ticketCompany.setText(tc.ticket.company);
            ticketNotes.setText(tc.ticket.notes);
        }
    }

    private class TriggerScanListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            cardTicketDetail.setVisibility(View.INVISIBLE);
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setResultDisplayDuration(0);
            integrator.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            integrator.setPrompt(null);
            integrator.initiateScan();
        }
    }
}
