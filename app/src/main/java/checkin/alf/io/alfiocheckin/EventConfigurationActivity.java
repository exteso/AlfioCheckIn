package checkin.alf.io.alfiocheckin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ButtonRectangle;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Arrays;
import java.util.HashMap;

import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;

public class EventConfigurationActivity extends ActionBarActivity {

    private ActionBar actionBar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        AlfioConfiguration alfioConfToUpdate = (AlfioConfiguration) getIntent().getSerializableExtra("alfioConf");

        if (alfioConfToUpdate != null) {
            getMenuInflater().inflate(R.menu.edit_alfio_configuration, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        AlfioConfiguration alfioConfToUpdate = (AlfioConfiguration) getIntent().getSerializableExtra("alfioConf");

        int id = item.getItemId();
        if (id == R.id.edit_alfio_configuration_set_as_active) {

            SharedPreferences pref = getPref();
            AppConfiguration conf = getConf();
            conf.selectedInstance = alfioConfToUpdate.name;
            pref.edit().putString("alfio", Common.GSON.toJson(conf)).commit();

        } else if (id == R.id.edit_alfio_configuration_delete) {
            SharedPreferences pref = getPref();
            AppConfiguration conf = getConf();

            conf.configurations.remove(alfioConfToUpdate.name);
            if (conf.selectedInstance.equals(alfioConfToUpdate.name)) {
                conf.selectedInstance = null;
            }
            pref.edit().putString("alfio", Common.GSON.toJson(conf)).commit();
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }


    private SharedPreferences getPref() {
        return getSharedPreferences("alfio", Context.MODE_PRIVATE);
    }

    private AppConfiguration getConf() {
        return Common.GSON.fromJson(getPref().getString("alfio", null), AppConfiguration.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.actionBar = getSupportActionBar();


        setContentView(R.layout.event_configuration);


        final AlfioConfiguration alfioConfToUpdate = (AlfioConfiguration) getIntent().getSerializableExtra("alfioConf");

        final EditText name = (EditText) findViewById(R.id.event_configuration_name);
        final EditText eventName = (EditText) findViewById(R.id.event_configuration_event_name);
        final EditText baseUrl = (EditText) findViewById(R.id.event_configuration_baseurl);
        final EditText username = (EditText) findViewById(R.id.event_configuration_username);
        final EditText pwd = (EditText) findViewById(R.id.event_configuration_pwd);

        ButtonFlat createOrUpdate = (ButtonFlat) findViewById(R.id.event_configuration_create_or_update);

        actionBar.setTitle("Add new instance");

        if (alfioConfToUpdate != null) {
            name.setText(alfioConfToUpdate.name);
            eventName.setText(alfioConfToUpdate.eventName);
            baseUrl.setText(alfioConfToUpdate.url);
            username.setText(alfioConfToUpdate.username);
            pwd.setText(alfioConfToUpdate.password);

            createOrUpdate.setText("Update");
            actionBar.setTitle("Update " + alfioConfToUpdate.name);
        }


        ButtonRectangle scan = (ButtonRectangle) findViewById(R.id.event_configuration_scan_username_pwd);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(EventConfigurationActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setResultDisplayDuration(0);
                integrator.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                integrator.setPrompt(null);
                integrator.initiateScan();
            }
        });

        ButtonFlat cancel = (ButtonFlat) findViewById(R.id.event_configuration_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventConfigurationActivity.this.finish();
            }
        });


        createOrUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean hasError = false;

                AppConfiguration conf = getConf();
                SharedPreferences pref = getPref();

                for (EditText txt : Arrays.asList(name, eventName, baseUrl, username, pwd)) {
                    if (txt.getText() == null || "".equals(txt.getText().toString())) {
                        txt.setError("Cannot be empty");
                        hasError = true;
                    }
                }

                if (baseUrl.getError() == null && !(baseUrl.getText().toString().startsWith("http://") || baseUrl.getText().toString().startsWith("https://"))) {
                    baseUrl.setError("Must be an url");
                    hasError = true;
                }

                if (alfioConfToUpdate == null && name.getError() != null && conf != null && conf.configurations.containsKey(name.getText().toString())) {
                    name.setError("name is already present!");
                    hasError = true;
                }

                if (!hasError) {

                    String oldName = alfioConfToUpdate != null ? alfioConfToUpdate.name : null;

                    AlfioConfiguration alfConf = alfioConfToUpdate == null ? new AlfioConfiguration() : alfioConfToUpdate;

                    alfConf.name = name.getText().toString();
                    alfConf.eventName = eventName.getText().toString();
                    alfConf.url = baseUrl.getText().toString();
                    alfConf.username = username.getText().toString();
                    alfConf.password = pwd.getText().toString();


                    if (conf == null) {
                        conf = new AppConfiguration();
                        conf.selectedInstance = name.getText().toString();
                        conf.configurations = new HashMap<>();
                    }

                    conf.configurations.put(name.getText().toString(), alfConf);

                    if (oldName != null && oldName.equals(conf.selectedInstance)) {
                        conf.selectedInstance = alfConf.name;
                    }

                    pref.edit().putString("alfio", Common.GSON.toJson(conf)).commit();
                    EventConfigurationActivity.this.finish();
                }

            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null && scanResult.getContents() != null) {
            ((EditText) findViewById(R.id.event_configuration_pwd)).setText(scanResult.getContents());
        }
    }
}
