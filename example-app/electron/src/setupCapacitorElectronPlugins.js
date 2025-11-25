// the code of this file was written based on the capacitor-community/electron implementation
// https:// github.com/capacitor-community/electron/blob/main/src/electron-platform/util.ts#L89

// @ts-check
/* eslint-disable @typescript-eslint/no-var-requires */

const { ipcMain } = require('electron')
const EventEmitter = require('events')
// const path = require('path')

const plugins = require('./rt/electron-plugins')

function deepClone(object) {
  if (globalThis?.structuredClone) {
    return globalThis.structuredClone(object)
  }

  return JSON.parse(JSON.stringify(object))
}

const pluginInstanceRegistry = {}
const config = {}
function setupCapacitorElectronPlugins() {
  //   const rtPluginsPath = path.join(__dirname, 'scripts', 'electron', 'electron-plugins.js')
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  //   const plugins = require(rtPluginsPath)

  for (const pluginKey of Object.keys(plugins)) {
    for (const classKey of Object.keys(plugins[pluginKey]).filter(
      className => className !== 'default'
    )) {
      if (!pluginInstanceRegistry[classKey]) {
        pluginInstanceRegistry[classKey] = new plugins[pluginKey][classKey](
          deepClone(config)
        )
      }

      const functionList = Object.getOwnPropertyNames(
        plugins[pluginKey][classKey].prototype
      ).filter(v => v !== 'constructor')

      for (const functionName of functionList) {
        if (functionName === 'addListener') {
          ipcMain.on(`event-add-${classKey}`, (event, type) => {
            const eventHandler = (...data) =>
              event.sender.send(`event-${classKey}-${type}`, ...data)

            pluginInstanceRegistry[classKey].addListener(type, eventHandler)

            ipcMain.once(`event-remove-${classKey}-${type}`, () => {
              pluginInstanceRegistry[classKey].removeListener(type, eventHandler)
            })
          })
        } else if ('removeAllListeners' === functionName) {
          ipcMain.on(`event-remove-all-listeners`, () => {
            pluginInstanceRegistry[classKey].removeAllListeners()
          })
        } else {
          console.log(`--> ${functionName}`);
          ipcMain.handle(`${classKey}-${functionName}`, (_event, ...args) => {
            console.log(`called ipcMain.handle: ${classKey}-${functionName}`)
            const pluginRef = pluginInstanceRegistry[classKey]
  
            return pluginRef[functionName](...args)
          })
        }
      }

    }
  }
}

module.exports = {
  setupCapacitorElectronPlugins,
}
