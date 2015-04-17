package checkin.alf.io.alfiocheckin.model;

public class CsrfAndEventId {

    public final String csrf;
    public final long eventId;

    public CsrfAndEventId(String csrf, long eventId) {
        this.csrf = csrf;
        this.eventId = eventId;
    }
}
