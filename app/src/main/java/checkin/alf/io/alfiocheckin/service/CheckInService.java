package checkin.alf.io.alfiocheckin.service;

import android.util.Log;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;

import checkin.alf.io.alfiocheckin.Common;
import checkin.alf.io.alfiocheckin.event.CheckInFailure;
import checkin.alf.io.alfiocheckin.event.CheckInSuccess;
import checkin.alf.io.alfiocheckin.event.FetchCSRFTokenFailure;
import checkin.alf.io.alfiocheckin.event.FetchCSRFTokenSuccess;
import checkin.alf.io.alfiocheckin.event.FetchTicketFailure;
import checkin.alf.io.alfiocheckin.event.FetchTicketSuccess;
import checkin.alf.io.alfiocheckin.model.AlfioConfiguration;
import checkin.alf.io.alfiocheckin.model.CsrfAndEventId;
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

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.client.setCookieHandler(cookieManager);
    }

    private static String extractCSRF(String body) {
        int idx = body.indexOf("\"_csrf\"");
        String valueAttr = "value=\"";
        int valueIdx = body.indexOf(valueAttr, idx);
        int endValue = body.indexOf("\"", valueIdx + valueAttr.length());
        return body.substring(valueIdx + valueAttr.length(), endValue);
    }

    private static String extractCSRFFromAdmin(String body) {
        int idx = body.indexOf("\"_csrf\"");
        String contentAttr = "content=\"";
        int valueIdx = body.indexOf(contentAttr, idx);
        int endValue = body.indexOf("\"", valueIdx + contentAttr.length());
        return body.substring(valueIdx + contentAttr.length(), endValue);
    }

    public void checkIn(AlfioConfiguration conf, CsrfAndEventId crsfAndEventId, final String code) {
        checkIn(conf, crsfAndEventId, code, new Callback() {
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

    private void checkIn(AlfioConfiguration conf, CsrfAndEventId crsfAndEventId, String code, Callback callback) {

        if (code == null) {
            callback.onFailure(null, null);
            return;
        }

        String ticketId = code.split("/")[0];

        String url = conf.url + "/admin/api/check-in/" + crsfAndEventId.eventId + "/ticket/" + ticketId;

        TicketCode tc = new TicketCode();
        tc.code = code;
        String json = Common.GSON.toJson(tc);
        Request req = new Request.Builder().url(url).post(RequestBody.create(MediaType.parse("application/json"), json)).addHeader("X-CSRF-TOKEN", crsfAndEventId.csrf).build();
        Call call = client.newCall(req);
        call.enqueue(callback);
    }

    //GRUUUUUUUUUUUUIK GRUIIIIIIK
    private void fetchCSRFTokenAndEventId(final AlfioConfiguration conf, final FetchCSRFAndEventId callback) {
        String adminUrl = conf.url + "/admin/";
        Request reqAdminUrl = new Request.Builder().url(adminUrl).build();

        final Call callAdminUrl = client.newCall(reqAdminUrl);

        callAdminUrl.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String url = conf.url + "/authentication";
                Request req = new Request.Builder().url(url).build();

                Call call = client.newCall(req);
                call.enqueue(new LoginPageCallback(callback, conf, url));
            }
        });
    }


    public void fetchCSRFTokenAndEventId(AlfioConfiguration conf) {
        fetchCSRFTokenAndEventId(conf, new FetchCSRFAndEventId() {
            @Override
            public void onFailure(Request request, IOException e) {
                bus.post(new FetchCSRFTokenFailure());
            }

            @Override
            public void success(CsrfAndEventId csrfAndEventId) {
                bus.post(new FetchCSRFTokenSuccess(csrfAndEventId));
            }
        });
    }

    private void getTicket(AlfioConfiguration conf, CsrfAndEventId crsfAndEventId, String code, Callback callback) {
        try {
            String ticketId = code.split("/")[0];
            String url = conf.url + "/admin/api/check-in/" + crsfAndEventId.eventId + "/ticket/" + ticketId + "?qrCode=" + URLEncoder.encode(code, "UTF-8");
            Request req = new Request.Builder().url(url).build();
            Call call = client.newCall(req);
            call.enqueue(callback);
        } catch (Throwable e) {
            Log.i("getTicket ", "error while composing req", e);
            callback.onFailure(null, null);
        }
    }

    public void getTicket(AlfioConfiguration conf, CsrfAndEventId csrfAndEventId, final String code) {
        getTicket(conf, csrfAndEventId, code, new Callback() {
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

    private interface FetchCSRFAndEventId {
        void onFailure(Request request, IOException e);

        void success(CsrfAndEventId csrfAndEventId);
    }

    private class LoginPageCallback implements Callback {
        private final FetchCSRFAndEventId callback;
        private final AlfioConfiguration conf;
        private final String url;

        public LoginPageCallback(FetchCSRFAndEventId callback, AlfioConfiguration conf, String url) {
            this.callback = callback;
            this.conf = conf;
            this.url = url;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            callback.onFailure(request, e);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            String body = response.body().string();

            String csrf = extractCSRF(body);
            RequestBody reqBody = new FormEncodingBuilder()
                    .add("username", conf.username)
                    .add("password", conf.password)
                    .add("_csrf", csrf)
                    .build();

            Call login = client.newCall(new Request.Builder().url(url).post(reqBody).build());
            login.enqueue(new LoginCallback(callback, conf));
        }
    }

    private class LoginCallback implements Callback {
        private final FetchCSRFAndEventId callback;
        private final AlfioConfiguration conf;

        public LoginCallback(FetchCSRFAndEventId callback, AlfioConfiguration conf) {
            this.callback = callback;
            this.conf = conf;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            callback.onFailure(request, e);
        }

        @Override
        public void onResponse(Response response) throws IOException {

            final String csrf = extractCSRFFromAdmin(response.body().string());
            String url = conf.url + "/admin/api/events/" + conf.eventName;

            Call eventInfo = client.newCall(new Request.Builder().url(url).build());

            eventInfo.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    callback.onFailure(request, e);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        EventContainer ev = Common.GSON.fromJson(response.body().string(), EventContainer.class);
                        callback.success(new CsrfAndEventId(csrf, ev.event.id));
                    } catch (Throwable e) {
                        callback.onFailure(null, null);
                    }
                }
            });
        }
    }
}
