package checkin.alf.io.alfiocheckin;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;

//https://github.com/makovkastar/FloatingActionButton/blob/master/README.md
public class SettingsActivity extends Activity {


    private AppConfiguration getConf() {
        SharedPreferences pref = getSharedPreferences("alfio", Context.MODE_PRIVATE);
        return Common.GSON.fromJson(pref.getString("alfio", null), AppConfiguration.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_list);

        ListView listView = (ListView) findViewById(R.id.settings_list);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.settings_fab);
        fab.attachToListView(listView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EventConfigurationActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        List<AlfioConfiguration> confs = getAlfioConfigurations();

        final CustomAdapter adapter = new CustomAdapter(this, R.layout.event_list_item, confs);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(SettingsActivity.this, EventConfigurationActivity.class);
                intent.putExtra("alfioConf", adapter.getItem(position));
                startActivityForResult(intent, 1);
            }
        });
    }

    private List<AlfioConfiguration> getAlfioConfigurations() {
        AppConfiguration conf = getConf();
        List<AlfioConfiguration> confs = new ArrayList<>();
        if (conf != null) {
            confs.addAll(conf.configurations.values());
        }
        return confs;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            ListView listView = (ListView) findViewById(R.id.settings_list);
            CustomAdapter ca = (CustomAdapter) listView.getAdapter();
            ca.clear();
            ca.addAll(getAlfioConfigurations());
        }
    }

    private static class CustomAdapter extends ArrayAdapter<AlfioConfiguration> {

        public CustomAdapter(Context context, int resource, List<AlfioConfiguration> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.event_list_item, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.event_list_item_text);
            textView.setText(getItem(position).name);
            return rowView;
        }
    }
}
