package checkin.alf.io.alfiocheckin;

import android.content.Intent;
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
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;
import checkin.alf.io.alfiocheckin.service.DataService;

public class EventConfigurationActivity extends ActionBarActivity {

    @InjectView(R.id.event_configuration_name)
    EditText name;
    @InjectView(R.id.event_configuration_event_name)
    EditText eventName;
    @InjectView(R.id.event_configuration_baseurl)
    EditText baseUrl;
    @InjectView(R.id.event_configuration_username)
    EditText username;
    @InjectView(R.id.event_configuration_pwd)
    EditText pwd;
    @InjectView(R.id.event_configuration_create_or_update)
    ButtonFlat createOrUpdate;
    @InjectView(R.id.event_configuration_scan_username_pwd)
    ButtonRectangle scan;
    @InjectView(R.id.event_configuration_cancel)
    ButtonFlat cancel;
    private ActionBar actionBar;
    private DataService dataService;


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


            AppConfiguration conf = dataService.getAppConfiguration();
            conf.selectedInstance = alfioConfToUpdate.name;
            dataService.saveAppConfiguration(conf);
        } else if (id == R.id.edit_alfio_configuration_delete) {

            AppConfiguration conf = dataService.getAppConfiguration();
            conf.configurations.remove(alfioConfToUpdate.name);
            if (conf.selectedInstance.equals(alfioConfToUpdate.name)) {
                conf.selectedInstance = null;
            }

            dataService.saveAppConfiguration(conf);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.actionBar = getSupportActionBar();


        setContentView(R.layout.event_configuration);

        ButterKnife.inject(this);

        this.dataService = new DataService(this);


        AlfioConfiguration alfioConfToUpdate = (AlfioConfiguration) getIntent().getSerializableExtra("alfioConf");


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

    }

    @OnClick(R.id.event_configuration_cancel)
    public void onClickCancel(View v) {
        finish();
    }

    @OnClick(R.id.event_configuration_scan_username_pwd)
    public void onClickScan(View v) {
        IntentIntegrator integrator = new IntentIntegrator(EventConfigurationActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setResultDisplayDuration(0);
        integrator.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        integrator.setPrompt(null);
        integrator.initiateScan();
    }

    @OnClick(R.id.event_configuration_create_or_update)
    public void onClickCreateOrUpdate(View v) {

        AlfioConfiguration alfioConfToUpdate = (AlfioConfiguration) getIntent().getSerializableExtra("alfioConf");

        boolean hasError = false;

        AppConfiguration conf = dataService.getAppConfiguration();

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

            dataService.saveAppConfiguration(conf);

            finish();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null && scanResult.getContents() != null) {
            Map<String, String> scannedMap = Common.GSON.fromJson(scanResult.getContents(), Map.class);
            username.setText(scannedMap.get("username"));
            pwd.setText(scannedMap.get("password"));
            baseUrl.setText(scannedMap.get("baseUrl"));
        }
    }
}
