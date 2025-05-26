import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorLlamaPlugin)
public class CapacitorLlamaPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CapacitorLlamaPlugin"
    public let jsName = "CapacitorLlama"
    // TODO: implement the bridge pattern
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initContext", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "releaseContext", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "releaseAllContexts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getFormattedChat", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "completion", returnType: CAPPluginReturnPromise)
    ]

    @objc func initContext(_ call: CAPPluginCall) {
        do {
            guard let contextId = call.getDouble("id") else {
                call.reject("Missing required parameter 'contextId'.")
                return
            }
            let result = CAPLlama.initContext(contextId, withContextParams: call.options)
            
            if let resultDict = result as? [String: Any] {
                call.resolve(resultDict)
            } else {
                call.reject("Invalid result format from RNLlama.initContext")
            }

        } catch let error as NSException {
            call.reject(error.reason ?? "Unknown error")
        }
    }

    @objc func releaseContext(_ call: CAPPluginCall) {
        do {
            guard let contextId = call.getDouble("id") else {
                call.reject("Missing required parameter 'contextId'.")
                return
            }
            CAPLlama.releaseContext(contextId)
            call.resolve()
        } catch let error as NSException {
            call.reject(error.reason ?? "Unknown error")
        }
    }

    @objc func releaseAllContexts(_ call: CAPPluginCall) {
        do {
            CAPLlama.releaseAllContexts()
            call.resolve()
        } catch let error as NSException {
            call.reject(error.reason ?? "Unknown error")
        }
    }

    @objc func getFormattedChat(_ call: CAPPluginCall) {
        let messages = call.getString("messages")
        let chatTemplate = call.getString("chatTemplate")
        guard let contextId = call.getDouble("id") else {
            call.reject("Missing required parameter 'id'.")
            return
        }
        do {
            let result = CAPLlama.getFormattedChat(contextId, withMessages: messages, withTemplate: chatTemplate, withParams: call.options)
            if let resultDict = result as? [String: Any] {
                call.resolve(resultDict)
            } else {
                call.reject("Invalid result format from RNLlama.initContext")
            }
        } catch let error as NSException {
            call.reject(error.reason ?? "Unknown error")
        }
    }

    @objc func completion(_ call: CAPPluginCall) {
        guard let contextId = call.getDouble("id") else {
            call.reject("Missing required parameter 'id'.")
            return
        }
        do {
            var params = call.getObject("params");
            // Safely get the "messages" array
            let messages = params?["messages"] as? [[String: Any]]
            if (messages != nil && params != nil) {
                // Convert the messages array into JSON data
                let jsonData = try JSONSerialization.data(withJSONObject: messages!, options: [])
                // Convert JSON data to a String (if you need a JSON string)
                let jsonString = String(data: jsonData, encoding: .utf8)

                print("Messages JSON String: \(jsonString ?? "nil")")
                    
                // Now you have `jsonData` or `jsonString` to work with
                
                let chatTemplate = params?["chatTemplate"] as? String
                let formatResult = CAPLlama.getFormattedChat(contextId, withMessages: jsonString, withTemplate: chatTemplate, withParams: params)
                params!["prompt"] = formatResult?["prompt"] as! String as String
            }
            let result = CAPLlama.completion(contextId, withCompletionParams: params)
            if let resultDict = result as? [String: Any] {
                call.resolve(resultDict)
            } else {
                call.reject("Invalid result format from RNLlama.initContext")
            }
        } catch let error as NSException {
            call.reject(error.reason ?? "Unknown error")
        } catch {
            call.reject(error.localizedDescription)
        }
    }
}
