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

  async completion(options: { id: number; params: CompletionParams; }): Promise<NativeCompletionResult> {
    const { id, params } = options
    // eslint-disable-next-line @typescript-eslint/consistent-type-imports
    const nlc: typeof import("node-llama-cpp") = await Function('return import("node-llama-cpp")')();
    const { LlamaChatSession } = nlc;
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
    const result = await session.prompt(prompt)
    session.sequence.dispose()
    session.dispose()
    return {
      content: result,
      text: result,
      reasoning_content: '',
    }
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
      }
    }
  }
}