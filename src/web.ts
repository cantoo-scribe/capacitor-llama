import { WebPlugin } from '@capacitor/core';
import { Wllama } from '@wllama/wllama/esm/index.js';
import WasmFromCDN from '@wllama/wllama/esm/wasm-from-cdn.js';
// let wllamaInstance = new Wllama(WLLAMA_CONFIG_PATHS, ...);

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
  async initContext(options: ContextParams & { id: number }): Promise<NativeLlamaContext> {
    const wllamaInstance = new Wllama(WasmFromCDN)
    // TODO: download from params
    await wllamaInstance.loadModelFromHF('bartowski/Qwen2.5-0.5B-Instruct-GGUF', 'Qwen2.5-0.5B-Instruct-Q5_K_S.gguf', { 
      n_ctx: options.n_ctx,
      n_batch: options.n_batch,
      cache_type_k: options.cache_type_k !== 'iq4_nl' ? options.cache_type_k : undefined,
      cache_type_v: options.cache_type_v !== 'iq4_nl' ? options.cache_type_v: undefined,
      embeddings: options.embedding,
      n_threads: options.n_threads,
    })
    
    // wllamaInstance.loadModelFromUrl()
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
        nEmbd: 0,
        nParams: 0,
        size: 0,
      }
    }
  }

  async completion(options: { id: number; params: CompletionParams; }): Promise<NativeCompletionResult> {
    const wllama = this.wllamas[options.id]
    let result = ''
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
      result = await wllama.createChatCompletion(history, {})
      
    } else if (options.params.prompt) {
      result = await wllama.createCompletion(options.params.prompt, {})
    }
    return {
      text: result,
      content: result,
      reasoning_content: ''
    }
  }

  async releaseContext(options: { id: number; }): Promise<void> {
    const wllama = this.wllamas[options.id]
    if (!wllama) {
      console.error('context not found:', options.id)
      return
    }
    wllama.exit()
    delete this.wllamas[options.id]
  }

  async releaseAllContexts(): Promise<void> {
    await Promise.allSettled(Object.keys(this.wllamas).map(key => this.releaseContext({ id: Number(key) })))
  }


}
