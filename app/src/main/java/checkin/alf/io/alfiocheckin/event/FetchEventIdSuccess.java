package checkin.alf.io.alfiocheckin.event;

import checkin.alf.io.alfiocheckin.model.EventId;

public class FetchEventIdSuccess {


    public final EventId eventId;

    public FetchEventIdSuccess(EventId eventId) {
        this.eventId = eventId;
    }
}
