import type { Token, TokenBias } from 'node-llama-cpp'

import type {
  CapacitorLlamaPlugin,
  CompletionParams,
  ContextParams,
  NativeCompletionResult,
  NativeLlamaContext
} from '../../src/definitions';


export class ElectronLlama implements CapacitorLlamaPlugin {
  // eslint-disable-next-line @typescript-eslint/consistent-type-imports
  contexts: Record<number, import('node-llama-cpp').LlamaContext> = {}
  completionAbortControllers: Record<number, AbortController> = {}

  async completion(options: { id: number; params: CompletionParams; }): Promise<NativeCompletionResult> {
    const { id, params } = options
    // eslint-disable-next-line @typescript-eslint/consistent-type-imports
    const nlc: typeof import("node-llama-cpp") = await Function('return import("node-llama-cpp")')();
    const { LlamaChatSession, TokenBias } = nlc;
    const context = this.contexts[id]
    const session = new LlamaChatSession({
      contextSequence: context.getSequence(),
    })
    let prompt = params.prompt
    if (params.messages?.length) {
      const lastMessage = params.messages.pop()
      const history = params.messages.map(message => {
        const messageContent = message.content || ''
        if (message.role === 'user') {
          return {
            type: 'user' as const,
            text: messageContent,
          }
        } else if (message.role ===  'prompt' || message.role ===  'system') {
          return {
            type: 'system' as const,
            text: messageContent,
          }
        } else if (message.role === 'model') {
          return {
            type: 'model' as const,
            response: [messageContent],
          }
        }
      }).filter(m => !!m)
      await session.setChatHistory(history)
      prompt = lastMessage.content
    }
    if (!prompt) throw new Error('prompt missing')
    const abortController = new AbortController()
    this.completionAbortControllers[id] = abortController
    let customBias: TokenBias | undefined
    if(params.logit_bias?.length) {
      customBias = new TokenBias(context.model.tokenizer)
      params.logit_bias.forEach(([tk, prob]) => {
        customBias.set(tk as Token, prob === false ? 'never' : prob)
      })
    }

    const result = await session.prompt(prompt, { maxTokens: options.params.n_predict, signal: abortController.signal, tokenBias: customBias }).finally(() => {
      delete this.completionAbortControllers[id]
    })
    
    session.sequence.dispose()
    session.dispose()
  
    return {
      content: result,
      text: result,
      reasoning_content: '',
    }
  }

  async stopCompletion(options: { id: number; }): Promise<void> {
    const controller = this.completionAbortControllers[options.id]
    if (controller) controller.abort()
  }

  async initContext(options: ContextParams & { id: number; }): Promise<NativeLlamaContext> {
    // eslint-disable-next-line @typescript-eslint/consistent-type-imports
    const nlc: typeof import("node-llama-cpp") = await Function('return import("node-llama-cpp")')();
    const { getLlama } = nlc;
    const llama = await getLlama()
    const model = await llama.loadModel({
      modelPath: options.model,
      gpuLayers: options.n_gpu_layers,
      useMlock: options.use_mlock,
    })
    const context = await model.createContext({
      batchSize: options.n_batch,
      contextSize: options.n_ctx,
      lora: options.lora,
    });
    
    this.contexts[options.id] = context

    return {
      gpu: false,
      reasonNoGPU: '',
      model: {
        desc: model.typeDescription,
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
        metadata: model.fileInfo.metadata,
        nEmbd: model.embeddingVectorSize,
        // TODO: is this the right value?
        nParams: model.trainContextSize,
        size: model.size,
        nVocab: model.tokenizer.length
      }
    }
  }

  async releaseContext(options: { id: number; }): Promise<void> {
    const context = this.contexts[options.id]
    if (!context) {
      console.error('context not found:', options.id)
      return
    }
    !context.disposed && await context.dispose()
    delete this.contexts[options.id]
    delete this.completionAbortControllers[options.id]
  }

  async releaseAllContexts(): Promise<void> {
    await Promise.allSettled(Object.keys(this.contexts).map(key => this.releaseContext({ id: Number(key) })))
  }

  async detokenize(options: { id: number; tokens: number[]; }): Promise<{ text: string }> {
    const context = this.contexts[options.id]
    // TODO: is this typecasting safe?
    return {
      text: context.model.detokenize(options.tokens as Token[], true)
    }
  }

  async tokenize(options: { id: number; text: string; specialTokens?: boolean; }): Promise<{ tokens: number[]; }> {
    const context = this.contexts[options.id]
    return {
      tokens: context.model.tokenize(options.text, options.specialTokens)
    }
  }

  async getVocab(options: { id: number; }): Promise<{ vocab: string[]; }> {
    const context = this.contexts[options.id]
    const vocab: string[] = []
    for(const token of context.model.iterateAllTokens()) {
      const rawText = context.model.detokenize([token]);
      vocab.push(rawText)
    }

    return {
      vocab
    }
  }
}