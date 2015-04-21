package checkin.alf.io.alfiocheckin;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.service.DataService;

//https://github.com/makovkastar/FloatingActionButton/blob/master/README.md
public class SettingsActivity extends Activity {

    @InjectView(R.id.settings_list)
    ListView listView;

    @InjectView(R.id.settings_fab)
    FloatingActionButton fab;

    private DataService dataService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_list);

        ButterKnife.inject(this);

        this.dataService = new DataService(this);


        fab.attachToListView(listView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EventConfigurationActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        List<AlfioConfiguration> confs = dataService.getAlfioConfigurations();

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            CustomAdapter ca = (CustomAdapter) listView.getAdapter();
            ca.clear();
            ca.addAll(dataService.getAlfioConfigurations());
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
