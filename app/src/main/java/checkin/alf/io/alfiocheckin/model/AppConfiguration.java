package checkin.alf.io.alfiocheckin.model;

import java.util.Map;

public class AppConfiguration {

    public String selectedInstance;
    public Map<String, AlfioConfiguration> configurations;


    public AlfioConfiguration getCurrentConfiguration() {
        return configurations != null && selectedInstance != null ? configurations.get(selectedInstance) : null;
    }

}
