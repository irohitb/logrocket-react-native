#import "LogRocketNativeModule.h"
#import <React/RCTLog.h>
#import <LogRocket/LogRocket-Swift.h>

@implementation LogRocketNativeModule

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(addLog:(NSString *)level args:(NSArray *)args)
{
  [LROSDK addLogWithLevel:level args:args];
}

RCT_EXPORT_METHOD(captureException:(NSString *)message
                  errorType:(NSString *)errorType
                  exceptionType:(NSString *)exceptionType
                  stackTrace:(NSString *)stackTrace)
{
  [LROSDK captureExceptionWithErrorMessage:message errorType:errorType exceptionType:exceptionType stackTrace:stackTrace];
}

RCT_EXPORT_METHOD(getSessionURL:(RCTResponseSenderBlock)callback)
{
  void (^completion)(NSString*) = ^(NSString* sessionURL) {
    callback(@[sessionURL]);
  };

  [LROSDK getSessionURL:completion];
}

NSMutableDictionary<NSString *, LROResponseBuilder *> *capturedRequests;

- (id)init {
  self = [super init];

  if (self) {
    capturedRequests = [[NSMutableDictionary alloc] init];
  }

  return self;
}

+ (BOOL)requiresMainQueueSetup {
  return YES;
}

RCT_EXPORT_METHOD(initWithConfig:(NSString *)appID
                  config:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSString *serverURL = config[@"serverURL"];
    NSSet<NSString *> *tags = [NSSet setWithArray:config[@"redactionTags"]];

    LROConfiguration *configuration = [[LROConfiguration alloc] initWithAppID:appID];

    configuration.serverURL = serverURL;
    configuration.redactionTags = tags;
    configuration.viewScanningEnabled = true;
    configuration.networkCaptureEnabled = false;
    configuration.logCaptureEnabled = false;
    configuration.requestSanitizer = nil;
    configuration.responseSanitizer = nil;
    configuration.registerTouchHandlers = true;

    BOOL result = [LROSDK initializeWithConfiguration:configuration];
    resolve(@(result));
  }

  @catch (NSException *e) {
    NSLog(@"Failed to start LogRocket SDK");
    resolve(@(false));
  }
}

RCT_EXPORT_METHOD(identifyWithTraits:(NSString *)userID
                  userInfo:(NSDictionary *)userInfo
                  isAnonymous:(BOOL *)isAnonymous)
{
    @try {
        if (isAnonymous) {
          [LROSDK identifyAsAnonymousWithUserID:userID userInfo:userInfo];
        } else {
          [LROSDK identifyWithUserID:userID userInfo:userInfo];
        }
    }
    @catch (NSException *e) {
        NSLog(@"LogRocket: Failed to identify user.");
    }
}

RCT_EXPORT_METHOD(shutdown:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    [LROSDK shutdown];
    resolve(@(true));
  }

  @catch (NSException *e) {
    NSLog(@"Failed to shutdown LogRocket SDK");
    resolve(@(false));
  }
}

RCT_EXPORT_METHOD(track:(NSString *)customEventName eventProperties:(NSDictionary *)eventProperties)
{
  LROCustomEventBuilder *builder = [[LROCustomEventBuilder alloc] initWithName:customEventName];

  for (NSString *key in eventProperties) {
    NSDictionary *value = [eventProperties objectForKey:key];

    if ([value objectForKey:@"doubleVal"]) {
      NSArray *doubleArray = [value objectForKey:@"doubleVal"];
      [builder putDoubleArrayWithKey:key value:doubleArray];
    } else if ([value objectForKey:@"boolVal"]) {
      NSArray *boolArray = [value objectForKey:@"boolVal"];
      [builder putBoolArrayWithKey:key value:boolArray];
    } else if ([value objectForKey:@"stringVal"]) {
      NSArray *stringArray = [value objectForKey:@"stringVal"];
      [builder putStringArrayWithKey:key value:stringArray];
    }
  }

  [LROSDK track:builder];
}


RCT_EXPORT_METHOD(captureRequest:(NSString *)reqID request:(NSDictionary *)request)
{
  LRORequestBuilder *builder = [LROSDK newRequestBuilder];
  builder.url = request[@"url"];

  if ([request objectForKey:@"body"]) {
    NSDictionary *arson = [request objectForKey:@"body"];
    if (arson && [arson objectForKey:@"arson"]) {
      builder.arsonBody = [arson objectForKey:@"arson"];
    }
  }

  if ([request objectForKey:@"method"]) {
    builder.method = [request objectForKey:@"method"];
  }

  if ([request objectForKey:@"headers"]) {
    builder.headers = [request objectForKey:@"headers"];
  }

  capturedRequests[reqID] = [builder capture];
}

RCT_EXPORT_METHOD(captureResponse:(NSString *)reqID response:(NSDictionary *)response)
{
  LROResponseBuilder *builder = capturedRequests[reqID];

  if (builder) {
    if ([response objectForKey:@"body"]) {
      NSDictionary *arson = [response objectForKey:@"body"];
      if (arson && [arson objectForKey:@"arson"]) {
        builder.arsonBody = [arson objectForKey:@"arson"];
      }
    }

    if ([response objectForKey:@"statusCode"]) {
      NSNumber *statusCode = (NSNumber *) [response objectForKey:@"statusCode"];
      builder.status = [statusCode longValue];
    }

    if ([response objectForKey:@"headers"]) {
      builder.headers = [response objectForKey:@"headers"];
    }

    [builder capture];
  }

  [capturedRequests removeObjectForKey:reqID];
}

@end
