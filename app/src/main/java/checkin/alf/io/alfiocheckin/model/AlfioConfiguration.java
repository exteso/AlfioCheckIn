package checkin.alf.io.alfiocheckin.model;

import java.io.Serializable;

public class AlfioConfiguration implements Serializable {

    public String name;
    public String eventName;
    public String url;
    public String username;
    public String password;

    public AlfioConfiguration() {

    }

    public AlfioConfiguration(String name, String eventName, String url, String username, String password) {
        this.name = name;
        this.eventName = eventName;
        this.url = url;
        this.username = username;
        this.password = password;
    }
}
