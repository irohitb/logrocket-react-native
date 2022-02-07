declare let LogRocket: ILogRocket;

export = LogRocket;

interface IRequest {
  reqId: string;
  url: string;
  headers: { [key: string]: string | undefined };
  body?: string;
  method: string;
  referrer?: string;
  mode?: string;
  credentials?: string;
}

interface IResponse {
  reqId: string;
  status?: number;
  headers: { [key: string]: string | undefined };
  body?: string;
  method: string;
}

interface IOptions {
  serverURL?: string,
  enableIPCapture?: boolean,
  uploadIntervalMs?: number,
  viewScanIntervalSeconds?: number,
  logLevel?: string,
  network?: {
    isEnabled?: boolean,
    requestSanitizer?(request: IRequest): null | IRequest,
    responseSanitizer?(response: IResponse): null | IResponse,
  },
  console?: {
    isEnabled?: boolean | {
      log?: boolean,
      info?: boolean,
      debug?: boolean,
      warn?: boolean,
      error?: boolean
    },
    shouldAggregateConsoleErrors?: boolean,
  },
  redactionTags?: [string],
  enablePersistence?: boolean,
  connectionType?: 'MOBILE' | 'WIFI',
  dangerouslySkipExpoGoCheck?: boolean,
}

interface IUserTraits {
  [propName: string]: string | number | boolean,
}

type TrackEventProperties = {
  revenue?: number,
  [key: string]: string | number | boolean | string[] | number[] | boolean[] | undefined
};

interface IExceptionOptions {
  tags?: {
    [key: string]: string | number | boolean;
  },
  extra?: {
    [key: string]: string | number | boolean;
  },
}

type State = { [key: string]: any };
type Action = { [key: string]: any };

interface IReduxMiddlewareOptions {
  /** Sanitizer function to scrub redux state */
  stateSanitizer?(state: State): State,
  /** Sanitizer function to scrub or ignore specific redux actions */
  actionSanitizer?(action: Action): null | Action,
}

interface ILogRocket {
  init(appID: string, config?: IOptions): void;
  getSessionURL(callback: (sessionURL: string) => void): void;
  /** Identify a user with the current session, with optional user traits */
  identify(uid: string, traits?: IUserTraits): void;
  identify(traits: IUserTraits): void;
  captureException(exception: any, options?: IExceptionOptions): void;
  /** Send an event to LogRocket */
  track(eventName: string, eventProperties?: TrackEventProperties): void;
  /** Returns a redux middleware which adds redux logs to LogRocket sessions */
  reduxMiddleware(
    /** Optional sanitizer configuration */
    options?: IReduxMiddlewareOptions
  ): any;
  shutdown(): void;
}
