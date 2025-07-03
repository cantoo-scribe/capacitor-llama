import { CapacitorLlama } from './CapacitorLlama';
import type {
  CompletionParams,
  ContextParams,
  NativeCompletionResult,
  NativeLlamaContext,
} from './definitions';

let contextCounter = 0;

export class LlamaContext {
  id: number;

  gpu = false;

  reasonNoGPU = '';

  model: NativeLlamaContext['model'];

  constructor({ gpu, reasonNoGPU, model }: NativeLlamaContext) {
    this.id = contextCounter++;
    this.gpu = gpu;
    this.reasonNoGPU = reasonNoGPU;
    this.model = model;
  }

  static async from(params: ContextParams): Promise<LlamaContext> {
    return CapacitorLlama.initContext({
      id: contextCounter,
      ...params,
    }).then((llama) => new LlamaContext(llama));
  }

  isLlamaChatSupported(): boolean {
    return !!this.model.chatTemplates.llamaChat;
  }

  async completion(params: CompletionParams): Promise<NativeCompletionResult> {
    const nativeParams = {
      ...params,
      // TODO: implement callbacks to enable partial completions
      emit_partial_completion: false,
    };
    
    // TODO: implement jinja on the native code
    // TODO: add the params to the same level of id
    return CapacitorLlama.completion({ id: this.id, params: nativeParams });
  }

  async release(): Promise<void> {
    await CapacitorLlama.releaseContext({ id: this.id })
  }

  async stopCompletion(): Promise<void> {
    await CapacitorLlama.stopCompletion({ id: this.id })
  }

  async tokenize(text: string, specialTokens = false): Promise<{ tokens: number[]; }> {
    return CapacitorLlama.tokenize({ id: this.id, text, specialTokens })
  }

  async detokenize(tokens: number[]): Promise<{ text: string }> {
    return CapacitorLlama.detokenize({ id: this.id, tokens })
  }
}
