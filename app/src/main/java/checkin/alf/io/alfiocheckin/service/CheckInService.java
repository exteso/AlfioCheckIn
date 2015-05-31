package checkin.alf.io.alfiocheckin.service;

import android.util.Base64;
import android.util.Log;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import checkin.alf.io.alfiocheckin.Common;
import checkin.alf.io.alfiocheckin.event.CheckInFailure;
import checkin.alf.io.alfiocheckin.event.CheckInSuccess;
import checkin.alf.io.alfiocheckin.event.FetchEventIdFailure;
import checkin.alf.io.alfiocheckin.event.FetchEventIdSuccess;
import checkin.alf.io.alfiocheckin.event.FetchTicketFailure;
import checkin.alf.io.alfiocheckin.event.FetchTicketSuccess;
import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.EventId;
import checkin.alf.io.alfiocheckin.model.EventContainer;
import checkin.alf.io.alfiocheckin.model.TicketCode;
import checkin.alf.io.alfiocheckin.model.TicketContainer;

public class CheckInService {


    private final Bus bus;
    private final OkHttpClient client;

    public CheckInService(Bus bus) {
        this.bus = bus;
        this.client = new OkHttpClient();

        bus.register(this);
    }

    private static Builder applyBasicAuth(AlfioConfiguration conf, Builder builder) {
        String basicAuth = conf.username+":"+conf.password;
        byte[] encoded;
        try {
            encoded = basicAuth.getBytes("UTF-8");
        } catch(UnsupportedEncodingException uee) {
            encoded = basicAuth.getBytes();
        }
        return builder.addHeader("Authorization", "Basic " + Base64.encodeToString(encoded, Base64.NO_WRAP));
    }



    public void checkIn(AlfioConfiguration conf, EventId eventId, final String code) {
        checkIn(conf, eventId, code, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                bus.post(new CheckInFailure());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    TicketContainer tc = Common.GSON.fromJson(response.body().string(), TicketContainer.class);
                    bus.post(new CheckInSuccess(tc, code));
                } catch (Throwable t) {
                    bus.post(new CheckInFailure());
                }
            }
        });
    }

    private void checkIn(AlfioConfiguration conf, EventId eventId, String code, Callback callback) {

        if (code == null) {
            callback.onFailure(null, null);
            return;
        }

        String ticketId = code.split("/")[0];

        String url = conf.url + "/admin/api/check-in/" + eventId.eventId + "/ticket/" + ticketId;

        TicketCode tc = new TicketCode();
        tc.code = code;
        String json = Common.GSON.toJson(tc);
        Request req = applyBasicAuth(conf, new Request.Builder().url(url).post(RequestBody.create(MediaType.parse("application/json"), json))).build();
        Call call = client.newCall(req);
        call.enqueue(callback);
    }

    private void fetchEventId(final AlfioConfiguration conf, final FetchEventId callback) {

        String url = conf.url + "/admin/api/events/" + conf.eventName;

        Call eventInfo = client.newCall(applyBasicAuth(conf, new Request.Builder().url(url)).build());

        eventInfo.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    EventContainer ev = Common.GSON.fromJson(response.body().string(), EventContainer.class);
                    callback.success(new EventId(ev.event.id));
                } catch (Throwable e) {
                    callback.onFailure(null, null);
                }
            }
        });
    }


    public void fetchEventId(AlfioConfiguration conf) {
        fetchEventId(conf, new FetchEventId() {
            @Override
            public void onFailure(Request request, IOException e) {
                bus.post(new FetchEventIdFailure());
            }

            @Override
            public void success(EventId eventId) {
                bus.post(new FetchEventIdSuccess(eventId));
            }
        });
    }

    private void getTicket(AlfioConfiguration conf, EventId eventId, String code, Callback callback) {
        try {
            String ticketId = code.split("/")[0];
            String url = conf.url + "/admin/api/check-in/" + eventId.eventId + "/ticket/" + ticketId + "?qrCode=" + URLEncoder.encode(code, "UTF-8");
            Request req = applyBasicAuth(conf, new Request.Builder().url(url)).build();
            Call call = client.newCall(req);
            call.enqueue(callback);
        } catch (Throwable e) {
            Log.i("getTicket ", "error while composing req", e);
            callback.onFailure(null, null);
        }
    }

    public void getTicket(AlfioConfiguration conf, EventId eventId, final String code) {
        getTicket(conf, eventId, code, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                bus.post(new FetchTicketFailure());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    TicketContainer tc = Common.GSON.fromJson(response.body().string(), TicketContainer.class);
                    bus.post(new FetchTicketSuccess(tc, code));
                } catch (Throwable t) {
                    Log.i("getTicket ", "error while parsing json", t);
                    bus.post(new FetchTicketFailure());
                }
            }
        });
    }

    private interface FetchEventId {
        void onFailure(Request request, IOException e);

        void success(EventId eventId);
    }
}
