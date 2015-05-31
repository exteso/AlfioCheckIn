package checkin.alf.io.alfiocheckin.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import checkin.alf.io.alfiocheckin.Common;
import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.AppConfiguration;
import checkin.alf.io.alfiocheckin.model.EventId;

/**
 * Created by sylvain on 4/21/15.
 */
public class DataService {

    private static final String KEY_ALFIO = "alfio";
    private static final String EVENT_ID = "eventId";

    private final SharedPreferences sharedPreferences;

    public DataService(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public AppConfiguration getAppConfiguration() {
        String s = sharedPreferences.getString(KEY_ALFIO, null);
        return Common.GSON.fromJson(s, AppConfiguration.class);
    }

    public List<AlfioConfiguration> getAlfioConfigurations() {
        AppConfiguration conf = getAppConfiguration();
        List<AlfioConfiguration> confs = new ArrayList<>();
        if (conf != null) {
            confs.addAll(conf.configurations.values());
        }
        return confs;
    }

    public void saveAppConfiguration(AppConfiguration appConfiguration) {
        sharedPreferences.edit().putString(KEY_ALFIO, Common.GSON.toJson(appConfiguration)).apply();
    }

    public void saveCsrfAndEventId(EventId eventId) {
        sharedPreferences.edit().putString(EVENT_ID, Common.GSON.toJson(eventId)).apply();
    }

    public EventId getEventId() {
        String s = sharedPreferences.getString(EVENT_ID, null);
        return Common.GSON.fromJson(s, EventId.class);
    }
}
