import { ConnectionStatus, Socket } from "./socket.ts";

class SocketHolder {

  socket?: Socket;
  timeout?: any;

  constructor() {}

  getOrCreateSocket(): Socket | undefined {
    if (this.socket !== undefined) {
      return this.socket;
    } else {
      this.socket = new Socket();
      return this.socket;
    }
  }

  connect(...registrationPaths: string[]) {
    this.assertInstantiated();
    try {
      this!.socket!.Connect(() => {
        this!.socket!.Register(registrationPaths);
      });
    } catch(e) {
      console.error(e);
    }
  }

  onConnectionStatusChange(callback: (status: ConnectionStatus) => void) {
    this.assertInstantiated();
    this!.socket!.onConnectionStatusChange = callback;
  }

  onStateUpdate(callback: (state: any, changed: any) => void) {
    this.assertInstantiated();
    this!.socket!.onStateUpdate = callback;
  }

  isInstantiated(): boolean {
    return this.socket !== undefined;
  }

  assertInstantiated() {
    if (!this.isInstantiated()) {
      throw new Error(`SocketHolder has not been instantiated. Call 'SocketHolder.connect(...)' first.`);
    }
  }

  close() {
    this.assertInstantiated();
    this.socket!.close();
  }
}

export const SocketHolderInstance = new SocketHolder();