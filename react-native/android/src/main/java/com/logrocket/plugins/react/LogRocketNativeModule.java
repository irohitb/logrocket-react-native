package com.logrocket.plugins.react;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.logrocket.core.CustomEventBuilder;
import com.logrocket.core.EventType;
import com.logrocket.core.IdentifyHelper;
import com.logrocket.core.LogRocketCore;
import com.logrocket.core.SDK;
import com.logrocket.core.TelemetryReporter;
import com.logrocket.core.network.IRequestBuilder;
import com.logrocket.core.network.IResponseBuilder;
import com.logrocket.core.util.logging.Logger;
import com.logrocket.core.util.logging.TaggedLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lr.core.Core.LogEvent;
import lr.redux.Redux.InitialState;
import lr.redux.Redux.ReduxAction;
import lr.utils.Utils;
import lr.utils.Utils.Value;

public class LogRocketNativeModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private final Map<String, IResponseBuilder> capturedRequests = new HashMap<>();
  private final Logger logger = new TaggedLogger("react-native");

  public LogRocketNativeModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    LogRocketReceiver receiver = new LogRocketReceiver(this.reactContext);

    IntentFilter filter = new IntentFilter();
    filter.addAction("LogRocketSDK.Error");

    this.reactContext.registerReceiver(receiver, filter);
  }

  @NonNull
  @Override
  public String getName() {
    return "LogRocketNativeModule";
  }

  @ReactMethod
  public void addListener(String eventName) {}

  @ReactMethod
  public void removeListeners(Integer count) {}

  @Keep
  @ReactMethod
  public void initWithConfig(String appID, ReadableMap config, Promise promise) {
    try {
      promise.resolve(
          SDK.init(
              this.reactContext,
              options -> {
                options.setAppID(appID);

                if (config == null) {
                  return;
                }

                if (config.hasKey("serverURL")) {
                  options.setServerURL(config.getString("serverURL"));
                }

                if (config.hasKey("enableIPCapture")) {
                  options.setEnableIPCapture(config.getBoolean("enableIPCapture"));
                }

                if (config.hasKey("uploadIntervalMs")) {
                  options.setUploadIntervalMs(config.getInt("uploadIntervalMs"));
                }

                if (config.hasKey("viewScanIntervalSeconds")) {
                  options.setViewScanIntervalSeconds(config.getInt("viewScanIntervalSeconds"));
                }

                if (config.hasKey("logLevel") && !config.isNull("logLevel")) {
                  //noinspection ConstantConditions
                  options.setLogLevel(SDK.LogLevel.fromString(config.getString("logLevel")));
                }

                if (config.hasKey("connectionType") && !config.isNull("connectionType")) {
                  //noinspection ConstantConditions
                  options.setConnectionType(
                      SDK.ConnectionType.fromString(config.getString("connectionType")));
                }

                if (config.hasKey("redactionTags")) {
                  ArrayList<Object> tags = config.getArray("redactionTags").toArrayList();
                  for (Object tag : tags) {
                    options.addRedactionTag(tag);
                  }
                }

                if (config.hasKey("enablePersistence")) {
                  options.setEnablePersistence(config.getBoolean("enablePersistence"));
                }
              }));
    } catch (Throwable th) {
      this.logger.error("Error during init", th);
      TelemetryReporter.sendErrorTelemetry(th);
      promise.resolve(false);
    }
  }

  @Keep
  @ReactMethod
  public void installShutdownHandler(Callback callback) {
    try {
      SDK.onShutdown(callback::invoke);
    } catch (Throwable th) {
      this.logger.error("Error during installShutdownHandler call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void captureRequest(String reqId, ReadableMap request) {
    try {
      IRequestBuilder requestBuilder = SDK.newRequestBuilder().setUrl(request.getString("url"));

      if (request.hasKey("body")) {
        ReadableMap arson = request.getMap("body");
        if (arson != null && arson.hasKey("arson")) {
          requestBuilder.setArsonBody(arson.getString("arson"));
        }
      }

      if (request.hasKey("method")) {
        requestBuilder.setMethod(request.getString("method"));
      }

      if (request.hasKey("headers") && !request.isNull("headers")) {
        //noinspection ConstantConditions
        requestBuilder.setHeaders(formatReadableMap(request.getMap("headers")));
      }

      IResponseBuilder responseBuilder = requestBuilder.capture();

      this.capturedRequests.put(reqId, responseBuilder);
    } catch (Throwable th) {
      this.logger.error("Error while capturing network request", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void captureResponse(String reqId, ReadableMap response) {
    try {
      IResponseBuilder responseBuilder = this.capturedRequests.get(reqId);
      if (responseBuilder != null) {

        if (response.hasKey("statusCode")) {
          responseBuilder.setStatusCode(response.getInt("statusCode"));
        }

        if (response.hasKey("duration")) {
          responseBuilder.setDuration(response.getDouble("duration"));
        }

        if (response.hasKey("body")) {
          ReadableMap arson = response.getMap("body");
          if (arson != null && arson.hasKey("arson")) {
            responseBuilder.setArsonBody(arson.getString("arson"));
          }
        }

        if (response.hasKey("headers") && !response.isNull("headers")) {
          //noinspection ConstantConditions
          responseBuilder.setHeaders(formatReadableMap(response.getMap("headers")));
        }

        responseBuilder.capture();
      }

    } catch (Throwable th) {
      this.logger.error("Error while capturing network response", th);
      TelemetryReporter.sendErrorTelemetry(th);
    } finally {
      this.capturedRequests.remove(reqId);
    }
  }

  @Keep
  @ReactMethod
  public void getSessionURL(Callback callback) {
    try {
      SDK.getSessionURL(callback::invoke);
    } catch (Throwable th) {
      this.logger.error("Error during getSessionURL call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void addLog(String level, ReadableArray args) {
    try {
      List<Object> argsList = args.toArrayList();
      LogRocketCore core = LogRocketCore.maybeGetInstance();

      if (core != null) {
        LogEvent.Builder logEvent = LogEvent.newBuilder();

        switch (level) {
          case "DEBUG":
            logEvent.setLogLevel(LogEvent.LogLevel.DEBUG);
            break;
          case "INFO":
            logEvent.setLogLevel(LogEvent.LogLevel.INFO);
            break;
          case "LOG":
            logEvent.setLogLevel(LogEvent.LogLevel.LOG);
            break;
          case "WARN":
            logEvent.setLogLevel(LogEvent.LogLevel.WARN);
            break;
          case "ERROR":
            logEvent.setLogLevel(LogEvent.LogLevel.ERROR);
            break;
        }

        for (Object arg : argsList) {
          logEvent.addArgs(Value.newBuilder().setArson((String) arg).build());
        }

        core.addEvent(EventType.LogEvent, logEvent);
      }
    } catch (Throwable th) {
      this.logger.error("Error while adding log", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void identifyWithTraits(String uid, ReadableMap traits, Boolean isAnonymous) {
    try {
      IdentifyHelper.identify(uid, formatReadableMap(traits), isAnonymous);
    } catch (Throwable th) {
      this.logger.error("Error during identify call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void shutdown() {
    try {
      SDK.shutdown();
    } catch (Throwable th) {
      this.logger.error("Error while shutting down", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void track(String customEventName, ReadableMap eventProperties) {
    try {
      CustomEventBuilder builder = new CustomEventBuilder(customEventName);

      Iterator<Map.Entry<String, Object>> iterator = eventProperties.getEntryIterator();
      while (iterator.hasNext()) {
        Entry<String, Object> entry = iterator.next();
        String keyString = entry.getKey().toString();
        ReadableMap protobufVariant = (ReadableMap) entry.getValue();

        if (protobufVariant.hasKey("doubleVal")) {
          ReadableArray readableArray = protobufVariant.getArray("doubleVal");
          if (readableArray != null) {
            Iterator<Object> arrayIterator = readableArray.toArrayList().iterator();
            ArrayList<Double> list = new ArrayList<>();
            while (arrayIterator.hasNext()) {
              list.add((Double) arrayIterator.next());
            }
            builder.putDoubleArray(keyString, list);
          }
        } else if (protobufVariant.hasKey("boolVal")) {
          ReadableArray readableArray = protobufVariant.getArray("boolVal");
          if (readableArray != null) {
            Iterator<Object> arrayIterator = readableArray.toArrayList().iterator();
            ArrayList<Boolean> list = new ArrayList<>();
            while (arrayIterator.hasNext()) {
              list.add((Boolean) arrayIterator.next());
            }
            builder.putBooleanArray(keyString, list);
          }
        } else if (protobufVariant.hasKey("stringVal")) {
          ReadableArray readableArray = protobufVariant.getArray("stringVal");
          if (readableArray != null) {
            Iterator<Object> arrayIterator = readableArray.toArrayList().iterator();
            ArrayList<String> list = new ArrayList<>();
            while (arrayIterator.hasNext()) {
              list.add((String) arrayIterator.next());
            }
            builder.putStringArray(keyString, list);
          }
        }
      }
      SDK.track(builder);
    } catch (Throwable th) {
      this.logger.error("Error during track call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void captureException(
      String errorMessage, String errorType, String exceptionType, String stackTrace) {
    try {
      SDK.captureException(errorMessage, errorType, exceptionType, stackTrace);
    } catch (Throwable th) {
      this.logger.error("Error during captureException call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void captureReduxInitialState(ReadableMap data) {
    try {
      int storeId = data.getInt("storeId");
      String state = data.getString("state");

      LogRocketCore core = LogRocketCore.maybeGetInstance();

      if (core != null) {
        InitialState.Builder builder =
            InitialState.newBuilder()
                .setState(Utils.Value.newBuilder().setArson(state))
                .setStoreId(storeId);

        core.addEvent(EventType.ReduxInitialState, builder);
      }
    } catch (Throwable th) {
      this.logger.error("Error during captureReduxInitialState call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  @Keep
  @ReactMethod
  public void captureReduxAction(ReadableMap data) {
    try {
      int storeId = data.getInt("storeId");
      int count = data.getInt("count");
      float duration = data.getInt("duration");
      String stateDelta = data.getString("stateDelta");
      String action = data.getString("action");

      LogRocketCore core = LogRocketCore.maybeGetInstance();

      if (core != null) {
        ReduxAction.Builder builder =
            ReduxAction.newBuilder()
                .setStoreId(storeId)
                .setAction(Utils.Value.newBuilder().setArson(action))
                .setDuration(duration)
                .setStateDelta(Utils.Value.newBuilder().setArson(stateDelta))
                .setCount(count);

        core.addEvent(EventType.ReduxAction, builder);
      }
    } catch (Throwable th) {
      this.logger.error("Error during captureReduxAction call", th);
      TelemetryReporter.sendErrorTelemetry(th);
    }
  }

  private static Map<String, String> formatReadableMap(ReadableMap map) {
    Map<String, String> formattedMap = new HashMap<>();

    if (map == null) {
      return formattedMap;
    }

    Iterator<Map.Entry<String, Object>> iterator = map.getEntryIterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Object> entry = iterator.next();
      formattedMap.put(entry.getKey(), entry.getValue().toString());
    }

    return formattedMap;
  }

  class LogRocketReceiver extends BroadcastReceiver {
    private final ReactApplicationContext reactContext;
    private final Logger logger = new TaggedLogger("logrocket-receiver");

    public LogRocketReceiver(ReactApplicationContext reactContext) {
      this.reactContext = reactContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      try {
        String errorMessage = intent.getStringExtra("errorMessage");
        boolean shouldDisableAndFreeMemory =
            intent.getBooleanExtra("shouldDisableAndFreeMemory", false);
        boolean shouldWarnAboutReduxSize =
            intent.getBooleanExtra("shouldWarnAboutReduxSize", false);
        WritableMap params = Arguments.createMap();
        if (errorMessage.length() > 0) {
          params.putString("errorMessage", errorMessage);
        }

        params.putBoolean("shouldDisableAndFreeMemory", shouldDisableAndFreeMemory);
        params.putBoolean("shouldWarnAboutReduxSize", shouldWarnAboutReduxSize);

        this.reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("LogRocketSDKOnError", params);
      } catch (Throwable th) {
        this.logger.error("Error while sending event from Native Module", th);
        TelemetryReporter.sendErrorTelemetry(th);
      }
    }
  }
}
