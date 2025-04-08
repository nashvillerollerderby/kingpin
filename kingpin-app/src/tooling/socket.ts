export enum ConnectionStatus {
  UNINITIALIZED = 'UNINITIALIZED',
  LOADING = 'LOADING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED'
}

export class Socket {
  private _connectCallback?: () => void = undefined;
  private _connectTimeout?: number = undefined;
  private _registerOnConnect: Array<string> = [];
  private _Connected: boolean = false;
  private _connectionStatus: ConnectionStatus = ConnectionStatus.UNINITIALIZED;
  private _started: boolean = false;
  private _preRegisterDone: boolean = false;
  private _enrichPropCache: { [key: string]: any} = {}
  private _heartbeat: any = null;
  private _callbackTrie: {} = {}
  private _stateTrie: {} = {}
  private _stateKeyRegex = /([A-Z][a-zA-Z]+)(?:\(([\-a-zA-Z0-9_.?]+)\))?/g;
  private _isNumberOnlyRegex = /^[0-9]+$/;

  state: any = {};
  splattedState: any = {};
  socket?: WebSocket;
  onStateUpdate?: (state: any, changed: any) => void;
  onConnectionStatusChange?: (status: ConnectionStatus) => void;

  get connectionStatus(): ConnectionStatus {
    return this._connectionStatus;
  }

  private set connectionStatus(status: ConnectionStatus) {
    this._connectionStatus = status;
    if (this.onConnectionStatusChange) {
      this.onConnectionStatusChange(this.connectionStatus);
    }
  }

  constructor() {

  }

  Connect(callback?: () => void) {
    if (!this._started) {
      this.connectionStatus = ConnectionStatus.LOADING;
      this._connectCallback = callback;
      this._connect();
      this._started = true;
    }
  }

  Command(command: any, data: any = null) {
    const req = {
      action: command,
      data: data,
    };
    this._send(JSON.stringify(req));
  }

  Set(key: any, value: any, flag: any) {
    const req = {
      action: 'Set',
      key: key,
      value: value,
      flag: typeof flag !== 'undefined' ? flag : '',
    };
    this._send(JSON.stringify(req));
  }

  Register(paths: string[]) {
    if (paths.length) {
      this._registerOnConnect.push(...paths);
    }
    if (this.connectionStatus === ConnectionStatus.CONNECTED) {
      const req = {
        action: 'Register',
        paths: this._registerOnConnect.length > 0 ? this._registerOnConnect : paths,
      };
      this._send(JSON.stringify(req));
    }
  }

  close() {
    this?.socket?.close();
  }

  private _connect() {
    this._connectTimeout = undefined;
    let url = (document.location.protocol === 'http:' ? 'ws' : 'wss') + '://';
    url += document.location.host + '/WS/';
    // This is not required, but helps figure out which device is which.
    url += '?source=' + encodeURIComponent(document.location.pathname + document.location.search);
    let platform = '';
    if (navigator.userAgent) {
      const match = navigator.userAgent.match(/\((.*)\).*\(.*\)/);
      if (match) {
        platform += match[1];
      }
    }
    if (!platform) {
      platform += window.screen.width + 'x' + window.screen.height + ' ';
      if (navigator.maxTouchPoints !== undefined) {
        platform += navigator.maxTouchPoints > 0 ? 'Touchscreen ' : 'NotTouchscreen; ';
      }
    }
    url += '&platform=' + encodeURIComponent(platform);

    if (!this._Connected || !this.socket) {
      console.debug('WS', 'Connecting the websocket at ' + url);

      this.socket = new WebSocket(url);
      this.socket.onopen = (_e: any) => {
        this.connectionStatus = ConnectionStatus.CONNECTED;
        this._Connected = true;
        console.debug('WS', 'Websocket: Open');

        Object.keys(this.state ?? {}).forEach((key) => {
          this._nullCallbacks(key);
        });
        this.state = {};
        if (this._preRegisterDone) {
          let req = {
            action: 'Register',
            paths: this._registerOnConnect,
          };
          this._send(JSON.stringify(req));
        }
        if (this._connectCallback != null) {
          this._connectCallback();
        }
        // Heartbeat every 30s so the connection is kept alive.
        this._heartbeat = setInterval(() => {
          if (!this.Command) {
            console.log('what', this);
          } else {
            this.Command('Ping');
          }
        }, 15000);
      };

      this.socket.onmessage = (e: any) => {
        const json = JSON.parse(e.data);
        console.debug('WS', json);
        if (json.authorization != null) {
          alert(json.authorization);
        }
        if (json.state != null) {
          if (!this._processUpdate) {
            console.log('processUpdate what?', this);
          } else {
            this._processUpdate(json.state);
          }
        }
      };

      this.socket.onclose = (e: any) => {
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this._Connected = false;
        console.debug('WS', 'Websocket: Close', e);
        if (this._connectTimeout == null) {
          this._connectTimeout = setTimeout(this._connect, 1000);
        }
        clearInterval(this._heartbeat);
      };
      this.socket.onerror = (e: any) => {
        console.error('WS', 'Websocket: Error', e);
        if (this._connectTimeout == null) {
          this._connectTimeout = setTimeout(this._connect, 1000);
        }
        clearInterval(this._heartbeat);
      };
    } else {
      // run the post connect callback if we didn't need to connect
      if (this._connectCallback != null) {
        this._connectCallback();
      }
    }
  }

  private _send(data: any) {
    if (this.socket != null && this.socket.readyState === 1) {
      this.socket.send(data);
    }
  }

  private _nullCallbacks(k: any) {
    k = this._enrichProp(k);
    this._getMatchesFromTrie(this._callbackTrie, k, 'partialKey').forEach((v) => {
      if (v.type === 'plain') {
        try {
          v.func(k, null, v.elem);
        } catch (err: any) {
          console.error(err.message);
          console.log(err.stack);
        }
      }
    });
    this._removeFromTrie(this._stateTrie, k, this._cleanPropCache);
  }

  private _processUpdate(state: any) {
    Object.entries(state ?? {}).forEach(([k, v]) => {
      k = this._enrichProp(k);

      if (v == null) {
        delete this.state[k];
        this._removeFromTrie(this._stateTrie, k, this._cleanPropCache);
      } else {
        this.state[k] = v;
        this._addToTrie(this._stateTrie, k, k);
      }
    });

    this._splatState();
    this.onStateUpdate?.call(this, this.state, state);
  }

  private _splatState() {
    Object.entries(this.state).forEach(([k, v]) => {
      const matches = k.matchAll(this._stateKeyRegex);
      let match = matches.next();
      const path: Array<string | number> = new Array<string | number>();
      while (!match.done) {
        const [_, key, subKey] = match.value;
        path.push(key);
        if (subKey) {
          if (this._isNumberOnlyRegex.test(subKey)) {
            const parsed = Number.parseInt(subKey);
            if (!isNaN(parsed)) {
              path.push(parsed);
              match = matches.next();
              continue;
            }
          }
          path.push(subKey);
        }
        match = matches.next();
      }
      let ref: any = null;
      for (let i = 0; i < path.length - 1; i++) {
        if ((ref ?? this.splattedState)[path[i]] != undefined) {
          ref = (ref ?? this.splattedState)[path[i]];
        } else {
          (ref ?? this.splattedState)[path[i]] = {};
          ref = (ref ?? this.splattedState)[path[i]];
        }
      }
      try {
        (ref ?? this.splattedState)[path.at(-1)!] = v;
      } catch(e) {
        console.warn(e);
      }
    });
  }

  private _enrichProp(prop: any): any {
    if (this._enrichPropCache[prop] != undefined) {
      return this._enrichPropCache[prop];
    }
    // noinspection JSPrimitiveTypeWrapperUsage
    prop = new String(prop);
    let i = prop.length - 1;
    let parts = [];
    while (i >= 0) {
      let dot;
      let key;
      let val = '';
      if (prop[i] === ')') {
        const open = prop.lastIndexOf('(', i);
        dot = prop.lastIndexOf('.', open);
        key = prop.substring(dot + 1, open);
        val = prop.substring(open + 1, i);
      } else {
        dot = prop.lastIndexOf('.', i);
        key = prop.substring(dot + 1, i + 1);
      }
      prop[key] = val;
      parts.push(key);
      i = dot - 1;
    }
    prop.field = parts[0];
    parts.reverse();
    prop.parts = parts;
    prop.upTo = (key: any) => {
      return key === prop.field
        ? prop
        : prop.substring(0, prop.indexOf(key + (prop[key] ? '(' : ''))) + key + (prop[key] ? '(' + prop[key] + ')' : '');
    };
    this._enrichPropCache[prop] = prop;
    return prop;
  }

  private _cleanPropCache(p: any, i: number) {
    if (!this?._enrichPropCache) {
      console.log('_enrichPropCache what', this);
    } else {
      delete this._enrichPropCache[p.slice(0, i + 1).join('')];
    }
  }

  private _addToTrie(t: any, key: any, value: any) {
    const p = key.split(/(?=[.(])/);
    for (let i = 0; i < p.length; i++) {
      const c = p[i];
      t[c] = t[c] || {};
      t = t[c];
    }
    t._values = t._values || [];
    t._values.push(value);
  }

  private _removeFromTrie(t: any, key: any, onDeletedNode: any) {
     let remove = (t: any, p: any, i: number) => {
      if (i === p.length) {
        delete t._values;
        return;
      }
      const c = p[i];
      if (t[c] == null) {
        return; // key not in trie
      }
      remove(t[c], p, i + 1);
      if (Object.entries(t[c] ?? {}).length > 0) {
        delete t[c];
        onDeletedNode(p, i);
      } else {
        return; // no further object will be empty
      }
    }

    remove(t, key.split(/(?=[.(])/), 0);
  }

  private _getMatchesFromTrie(trie: any, key: any, mode: string = 'partialKey') {
    let result: any[] = [];

    let findAllMatches = (t: any) => {
      if (t._values) {
        result.push(...t._values);
      }
      Object.keys(t).forEach(function (k) {
        if (k !== '_values') {
          findAllMatches(t[k]);
        }
      });
    }

    let findMatches = (t: any, p: any, i: number): any[] | undefined => {
      if (mode === 'partialKey' && t._values) {
        result.push(...t._values);
      }
      for (; i < p.length; i++) {
        if (t['.*)'] || t['(*)']) {
          // Allow Blah(*) as a wildcard.
          let j = i;
          // id captured by * might contain . and thus be split - find the end
          while (j < p.length && !p[j].endsWith(')')) {
            j++;
          }
          if (t['.*)']) {
            findMatches(t['.*)'], p, j + 1);
          }
          if (t['(*)']) {
            findMatches(t['(*)'], p, j + 1);
          }
        }
        if (p[i] === '.*)' || p[i] === '(*)') {
          Object.keys(t).forEach((k) => {
            if (k === '_values' || k.endsWith('*)')) {
              return;
            } else if (k.endsWith(')')) {
              findMatches(t[k], p, i + 1);
            } else {
              findMatches(t[k], p, i);
            }
          });
        } else {
          t = t[p[i]];
          if (t == null) {
            return;
          }
          if (mode === 'partialKey' && t._values) {
            result.push(...t._values);
          }
        }
      }
      if (mode === 'partialTrie') {
        findAllMatches(t);
      } else if (mode === 'exact' && t._values) {
        result.push(...t._values);
      }
    }

    findMatches(trie, key.split(/(?=[.(])/), 0);
    return result;
  }
}