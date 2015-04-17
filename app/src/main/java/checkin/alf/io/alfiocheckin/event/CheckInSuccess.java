package checkin.alf.io.alfiocheckin.event;

import checkin.alf.io.alfiocheckin.model.TicketContainer;

/**
 * Created by sylvain on 4/17/15.
 */
public class CheckInSuccess {

    public final TicketContainer tc;
    public final String code;

    public CheckInSuccess(TicketContainer tc, String code) {
        this.tc = tc;
        this.code = code;
    }
}
