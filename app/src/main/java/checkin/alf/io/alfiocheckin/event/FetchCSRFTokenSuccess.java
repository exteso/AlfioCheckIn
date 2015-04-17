package checkin.alf.io.alfiocheckin.event;

import checkin.alf.io.alfiocheckin.model.CsrfAndEventId;

public class FetchCSRFTokenSuccess {


    public final CsrfAndEventId csrfAndEventId;

    public FetchCSRFTokenSuccess(CsrfAndEventId csrfAndEventId) {
        this.csrfAndEventId = csrfAndEventId;
    }
}
