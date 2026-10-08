export type HostMode = 'h5' | 'pc-web' | 'pc-app';

export type HostRequest = {
  id?: string;
  method: string;
  data?: unknown;
};

export type HostResponse = {
  errorCode?: number;
  errorMsg?: string;
  data?: unknown;
};

export type HostCall = HostRequest & {
  receivedAt: number;
};

export interface HostController {
  install(): Promise<void>;
  calls(): HostCall[];
  waitForCall(method: string, timeoutMs?: number): Promise<HostCall>;
  respond(call: HostCall, response: HostResponse): Promise<void>;
  emit(method: string, data?: unknown): Promise<void>;
}
