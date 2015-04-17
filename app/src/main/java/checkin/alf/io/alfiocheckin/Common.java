package checkin.alf.io.alfiocheckin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class Common {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    // https://github.com/square/otto/tree/master/otto-sample/src/main/java/com/squareup/otto/sample
    public static final Bus BUS = new Bus(ThreadEnforcer.ANY);
}
