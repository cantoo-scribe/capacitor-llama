import { WebPlugin } from '@capacitor/core';
import { Wllama } from '@wllama/wllama/esm/index.js';
import WasmFromCDN from '@wllama/wllama/esm/wasm-from-cdn.js';

import type {
  CapacitorLlamaPlugin,
  CompletionParams,
  ContextParams,
  NativeCompletionResult,
  NativeLlamaContext,
} from './definitions';

// TODO: pass the correct url
// const CONFIG_PATHS = {
//   'single-thread/wllama.wasm': './esm/single-thread/wllama.wasm',
//   'multi-thread/wllama.wasm' : './esm/multi-thread/wllama.wasm',
// };
export class CapacitorLlamaWeb extends WebPlugin implements CapacitorLlamaPlugin {
  wllamas: Record<number, Wllama> = {}
  completionAbortControllers: Record<number, () => void> = {}
  async initContext(options: ContextParams & { id: number }): Promise<NativeLlamaContext> {
    const wllamaInstance = new Wllama(WasmFromCDN)
    await wllamaInstance.loadModelFromUrl(options.model, { 
      n_ctx: options.n_ctx,
      n_batch: options.n_batch,
      cache_type_k: options.cache_type_k !== 'iq4_nl' ? options.cache_type_k : undefined,
      cache_type_v: options.cache_type_v !== 'iq4_nl' ? options.cache_type_v: undefined,
      embeddings: options.embedding,
      n_threads: options.n_threads,
      useCache: options.readFromCache,
    })

    this.wllamas[options.id] = wllamaInstance
    return {
      gpu: false,
      reasonNoGPU: '',
      model: {
        desc: '',
        isChatTemplateSupported: true,
        chatTemplates: {
          llamaChat: true,
          minja: { 
            default: false,
            toolUseCaps: { parallelToolCalls:false, systemRole: false, toolCallId: false, toolCalls: false, toolResponses: false, tools: false },
            toolUse: false,
            defaultCaps: { parallelToolCalls:false, systemRole: false, toolCallId: false, toolCalls: false, toolResponses: false, tools: false }
          },
        },
        metadata: wllamaInstance.getModelMetadata().meta, // model.fileInfo.metadata,
        nEmbd: wllamaInstance.getLoadedContextInfo().n_embd,
        // TODO: how to get the number of parameters?
        nParams: 0,
        size: (await wllamaInstance.modelManager.getModels()).find(model => model.url === options.model)?.size || 0,
        nVocab: wllamaInstance.getLoadedContextInfo().n_vocab
      }
    }
  }

  async completion(options: { id: number; params: CompletionParams; }): Promise<NativeCompletionResult> {
    const wllama = this.wllamas[options.id]
    let result = ''
    let abort = false
    this.completionAbortControllers[options.id] = () => {
      abort = true
    }
    if (options.params.messages?.length) {
      const history = options.params.messages.map(message => {
        const messageContent = message.content || ''
        if (message.role ===  'prompt' || message.role ===  'system') {
          return {
            role: 'system' as const,
            content: messageContent,
          }
        } else if (message.role === 'model') {
          return {
            role: 'assistant' as const,
            content: messageContent,
          }
        }
        return {
          role: 'user' as const,
          content: messageContent,
        }
      }).filter(m => !!m)
      // 'system' | 'user' | 'assistant'
      result = await wllama.createChatCompletion(history, {
        nPredict: options.params.n_predict,
        onNewToken(_token, _piece, _currentText, { abortSignal }) {
          if (abort) abortSignal()
        },
      }).finally(() => {
        delete this.completionAbortControllers[options.id]
      })
      
    } else if (options.params.prompt) {
      result = await wllama.createCompletion(options.params.prompt, {
        nPredict: options.params.n_predict,
        onNewToken(_token, _piece, _currentText, { abortSignal }) {
          if (abort) abortSignal()
        },
      }).finally(() => {
        delete this.completionAbortControllers[options.id]
      })
    }
    return {
      text: result,
      content: result,
      reasoning_content: ''
    }
  }

  async stopCompletion(options: { id: number; }): Promise<void> {
    const abortController = this.completionAbortControllers[options.id]
    if (abortController) abortController()
  }

  async releaseContext(options: { id: number; }): Promise<void> {
    const wllama = this.wllamas[options.id]
    if (!wllama) {
      console.error('context not found:', options.id)
      return
    }
    wllama.exit()
    delete this.wllamas[options.id]
    delete this.completionAbortControllers[options.id]
  }

  async releaseAllContexts(): Promise<void> {
    await Promise.allSettled(Object.keys(this.wllamas).map(key => this.releaseContext({ id: Number(key) })))
  }

  async tokenize(options: { id: number; text: string; specialTokens?: boolean }): Promise<{ tokens: number[]; }> {
    const wllama = this.wllamas[options.id]
    const tokens = await wllama.tokenize(options.text, options.specialTokens)
    return {
      tokens
    }
  }

  async detokenize(options: { id: number; tokens: number[]; }): Promise<{ text: string }> {
    const wllama = this.wllamas[options.id]
    const uint8array = await wllama.detokenize(options.tokens)
    return { text: new TextDecoder().decode(uint8array) }
  }

  async getVocab(options: { id: number; }): Promise<{ vocab: string[]; }> {
    const wllama = this.wllamas[options.id]
    const vocab = await wllama.getVocab().then(tokens => 
      tokens.map(token => {
        const decoder = new TextDecoder('utf-8'); // Specify the encoding (default is UTF-8)
        return decoder.decode(token);
      })
    )
    return { vocab }
  }
}
