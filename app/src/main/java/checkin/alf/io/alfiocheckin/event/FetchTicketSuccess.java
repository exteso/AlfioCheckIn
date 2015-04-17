package checkin.alf.io.alfiocheckin.event;


import checkin.alf.io.alfiocheckin.model.TicketContainer;

public class FetchTicketSuccess {

    public final TicketContainer tc;
    public final String code;

    public FetchTicketSuccess(TicketContainer tc, String code) {
        this.tc = tc;
        this.code = code;
    }
}
