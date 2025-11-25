import { randomBytes } from 'crypto';
import { ipcRenderer, contextBridge } from 'electron';
import { EventEmitter } from 'events';

////////////////////////////////////////////////////////
// eslint-disable-next-line @typescript-eslint/no-var-requires
const plugins = require('./electron-plugins');

const randomId = (length = 5) => randomBytes(length).toString('hex');

const contextApi: {
  [plugin: string]: { [functionName: string]: () => Promise<any> };
} = {};

Object.keys(plugins).forEach((pluginKey) => {
  Object.keys(plugins[pluginKey])
    .filter((className) => className !== 'default')
    .forEach((classKey) => {
      const functionList = Object.getOwnPropertyNames(plugins[pluginKey][classKey].prototype).filter(
        (v) => v !== 'constructor'
      );

      if (!contextApi[classKey]) {
        contextApi[classKey] = {};
      }

      functionList.forEach((functionName) => {
        if (!contextApi[classKey][functionName] && !['addListener', 'removeAllListeners'].includes(functionName)) {
          contextApi[classKey][functionName] = (...args) => ipcRenderer.invoke(`${classKey}-${functionName}`, ...args);
        }
      });

      // Events
      
      const listeners: { [type: string]: ((...args: any[]) => void)[] } = {};
      const listenersOfTypeExist = (type: string) => type in listeners

      Object.assign(contextApi[classKey], {
        addListener(type: string, callback: (...args) => void) {

          // Deduplicate events
          if (!listenersOfTypeExist(type)) {
            listeners[type] = []
            ipcRenderer.send(`event-add-${classKey}`, type);
            const eventHandler = (_, ...args) => {
              listeners[type].forEach(listener => listener(...args))
            }
            ipcRenderer.addListener(`event-${classKey}-${type}`, eventHandler);
          }
          listeners[type].push(callback)

          return {
            remove: () => {
              const i = listeners[type].indexOf(callback);
              if (i >= 0) listeners[type].splice(i, 1);
            }
          }
        },
        removeAllListeners() {
          Object.entries(listeners).forEach(([type, currListeners]) => {
            currListeners.splice(0, currListeners.length)
          });
        },
      });
      
    });
});

contextBridge.exposeInMainWorld('CapacitorCustomPlatform', {
  name: 'electron',
  plugins: contextApi,
});
////////////////////////////////////////////////////////
