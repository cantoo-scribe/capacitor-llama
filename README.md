# capacitor-llama

Capacitor implementation for llama models

## Install

```bash
npm install capacitor-llama
npx cap sync
```

## API

<docgen-index>

* [`initContext(...)`](#initcontext)
* [`completion(...)`](#completion)
* [`stopCompletion(...)`](#stopcompletion)
* [`releaseContext(...)`](#releasecontext)
* [`releaseAllContexts()`](#releaseallcontexts)
* [`tokenize(...)`](#tokenize)
* [`detokenize(...)`](#detokenize)
* [`getVocab(...)`](#getvocab)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### initContext(...)

```typescript
initContext(options: ContextParams & { id: number; }) => Promise<NativeLlamaContext>
```

| Param         | Type                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#omit">Omit</a>&lt;<a href="#nativecontextparams">NativeContextParams</a>, 'cache_type_k' \| 'cache_type_v' \| 'pooling_type'&gt; & { cache_type_k?: 'f16' \| 'f32' \| 'q8_0' \| 'q4_0' \| 'q4_1' \| 'iq4_nl' \| 'q5_0' \| 'q5_1'; cache_type_v?: 'f16' \| 'f32' \| 'q8_0' \| 'q4_0' \| 'q4_1' \| 'iq4_nl' \| 'q5_0' \| 'q5_1'; pooling_type?: 'none' \| 'mean' \| 'cls' \| 'last' \| 'rank'; } & { id: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#nativellamacontext">NativeLlamaContext</a>&gt;</code>

--------------------


### completion(...)

```typescript
completion(options: { id: number; params: CompletionParams; }) => Promise<NativeCompletionResult>
```

| Param         | Type                                                                                   |
| ------------- | -------------------------------------------------------------------------------------- |
| **`options`** | <code>{ id: number; params: <a href="#completionparams">CompletionParams</a>; }</code> |

**Returns:** <code>Promise&lt;<a href="#nativecompletionresult">NativeCompletionResult</a>&gt;</code>

--------------------


### stopCompletion(...)

```typescript
stopCompletion(options: { id: number; }) => Promise<void>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: number; }</code> |

--------------------


### releaseContext(...)

```typescript
releaseContext(options: { id: number; }) => Promise<void>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: number; }</code> |

--------------------


### releaseAllContexts()

```typescript
releaseAllContexts() => Promise<void>
```

--------------------


### tokenize(...)

```typescript
tokenize(options: { id: number; text: string; specialTokens?: boolean; }) => Promise<{ tokens: number[]; }>
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code>{ id: number; text: string; specialTokens?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ tokens: number[]; }&gt;</code>

--------------------


### detokenize(...)

```typescript
detokenize(options: { id: number; tokens: number[]; }) => Promise<{ text: string; }>
```

| Param         | Type                                           |
| ------------- | ---------------------------------------------- |
| **`options`** | <code>{ id: number; tokens: number[]; }</code> |

**Returns:** <code>Promise&lt;{ text: string; }&gt;</code>

--------------------


### getVocab(...)

```typescript
getVocab(options: { id: number; }) => Promise<{ vocab: string[]; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: number; }</code> |

**Returns:** <code>Promise&lt;{ vocab: string[]; }&gt;</code>

--------------------


### Type Aliases


#### NativeLlamaContext

<code>{ model: { desc: string; size: number; nEmbd: number; nParams: number; nVocab: number; chatTemplates: { llamaChat: boolean; // Chat template in llama-chat.cpp minja: { // Chat template supported by minja.hpp default: boolean; defaultCaps: { tools: boolean; toolCalls: boolean; toolResponses: boolean; systemRole: boolean; parallelToolCalls: boolean; toolCallId: boolean; }; toolUse: boolean; toolUseCaps: { tools: boolean; toolCalls: boolean; toolResponses: boolean; systemRole: boolean; parallelToolCalls: boolean; toolCallId: boolean; }; }; }; metadata: <a href="#record">Record</a>&lt;string, unknown&gt;; isChatTemplateSupported: boolean; // Deprecated }; /** * Loaded library name for Android */ androidLib?: string; gpu: boolean; reasonNoGPU: string; }</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>


#### ContextParams

<code><a href="#omit">Omit</a>&lt;<a href="#nativecontextparams">NativeContextParams</a>, 'cache_type_k' | 'cache_type_v' | 'pooling_type'&gt; & { cache_type_k?: 'f16' | 'f32' | 'q8_0' | 'q4_0' | 'q4_1' | 'iq4_nl' | 'q5_0' | 'q5_1'; cache_type_v?: 'f16' | 'f32' | 'q8_0' | 'q4_0' | 'q4_1' | 'iq4_nl' | 'q5_0' | 'q5_1'; pooling_type?: 'none' | 'mean' | 'cls' | 'last' | 'rank'; }</code>


#### Omit

Construct a type with the properties of T except for those in type K.

<code><a href="#pick">Pick</a>&lt;T, <a href="#exclude">Exclude</a>&lt;keyof T, K&gt;&gt;</code>


#### Pick

From T, pick a set of properties whose keys are in the union K

<code>{ [P in K]: T[P]; }</code>


#### Exclude

<a href="#exclude">Exclude</a> from T those types that are assignable to U

<code>T extends U ? never : T</code>


#### NativeContextParams

<code>{ /** * For **android**, **iOS** and **electron** the `model` is an absolute path. Example: `/path/to/my-model.gguf` * * For **web** the `model` is an url. Example: https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q5_K_S.gguf */ model: string; /** * **Note: This option is available only on web.** * * Controls whether to load the model from the browser cache. * * - If `true` (default), the model manager first tries to load the model from cache. * If it's not cached, it will download the model and cache it for future use. * * - If `false`, the model will always be downloaded, ignoring any cached versions. */ readFromCache?: boolean; /** * Chat template to override the default one from the model. */ chat_template?: string; reasoning_format?: string; is_model_asset?: boolean; use_progress_callback?: boolean; n_ctx?: number; n_batch?: number; n_ubatch?: number; n_threads?: number; /** * Number of layers to store in VRAM (Currently only for iOS) */ n_gpu_layers?: number; /** * Skip GPU devices (iOS only) */ no_gpu_devices?: boolean; /** * Enable flash attention, only recommended in GPU device (Experimental in llama.cpp) */ flash_attn?: boolean; /** * KV cache data type for the K (Experimental in llama.cpp) */ cache_type_k?: string; /** * KV cache data type for the V (Experimental in llama.cpp) */ cache_type_v?: string; use_mlock?: boolean; use_mmap?: boolean; vocab_only?: boolean; /** * Single LoRA adapter path */ lora?: string; /** * Single LoRA adapter scale */ lora_scaled?: number; /** * LoRA adapter list */ lora_list?: { path: string; scaled?: number }[]; rope_freq_base?: number; rope_freq_scale?: number; pooling_type?: number; // Embedding params embedding?: boolean; embd_normalize?: number; }</code>


#### NativeCompletionResult

<code>{ /** * Original text (Ignored reasoning_content / tool_calls) */ text: string; /** * Reasoning content (parsed for reasoning model) */ reasoning_content: string; /** * Content text (Filtered text by reasoning_content / tool_calls) */ content: string; completion_probabilities?: NativeCompletionTokenProb[]; }</code>


#### NativeCompletionTokenProb

<code>{ content: string; probs: NativeCompletionTokenProbItem[]; }</code>


#### NativeCompletionTokenProbItem

<code>{ tok_str: string; prob: number; }</code>


#### CompletionParams

<code><a href="#omit">Omit</a>&lt;<a href="#nativecompletionparams">NativeCompletionParams</a>, 'emit_partial_completion' | 'prompt'&gt; & <a href="#completionbaseparams">CompletionBaseParams</a></code>


#### NativeCompletionParams

<code>{ prompt: string; n_threads?: number; /** * JSON schema for convert to grammar for structured JSON output. * It will be override by grammar if both are set. */ json_schema?: string; /** * Set grammar for grammar-based sampling. Default: no grammar */ grammar?: string; /** * Lazy grammar sampling, trigger by grammar_triggers. Default: false */ grammar_lazy?: boolean; /** * Lazy grammar triggers. Default: [] */ grammar_triggers?: { type: number; value: string; token: number; }[]; preserved_tokens?: string[]; chat_format?: number; /** * Specify a JSON array of stopping strings. * These words will not be included in the completion, so make sure to add them to the prompt for the next iteration. Default: `[]` */ stop?: string[]; /** * Set the maximum number of tokens to predict when generating text. * **Note:** May exceed the set limit slightly if the last token is a partial multibyte character. * When 0,no tokens will be generated but the prompt is evaluated into the cache. Default: `-1`, where `-1` is infinity. */ n_predict?: number; /** * If greater than 0, the response also contains the probabilities of top N tokens for each generated token given the sampling settings. * Note that for temperature &lt; 0 the tokens are sampled greedily but token probabilities are still being calculated via a simple softmax of the logits without considering any other sampler settings. * Default: `0` */ n_probs?: number; /** * Limit the next token selection to the K most probable tokens. Default: `40` */ top_k?: number; /** * Limit the next token selection to a subset of tokens with a cumulative probability above a threshold P. Default: `0.95` */ top_p?: number; /** * The minimum probability for a token to be considered, relative to the probability of the most likely token. Default: `0.05` */ min_p?: number; /** * Set the chance for token removal via XTC sampler. Default: `0.0`, which is disabled. */ xtc_probability?: number; /** * Set a minimum probability threshold for tokens to be removed via XTC sampler. Default: `0.1` (&gt; `0.5` disables XTC) */ xtc_threshold?: number; /** * Enable locally typical sampling with parameter p. Default: `1.0`, which is disabled. */ typical_p?: number; /** * Adjust the randomness of the generated text. Default: `0.8` */ temperature?: number; /** * Last n tokens to consider for penalizing repetition. Default: `64`, where `0` is disabled and `-1` is ctx-size. */ penalty_last_n?: number; /** * Control the repetition of token sequences in the generated text. Default: `1.0` */ penalty_repeat?: number; /** * Repeat alpha frequency penalty. Default: `0.0`, which is disabled. */ penalty_freq?: number; /** * Repeat alpha presence penalty. Default: `0.0`, which is disabled. */ penalty_present?: number; /** * Enable Mirostat sampling, controlling perplexity during text generation. Default: `0`, where `0` is disabled, `1` is Mirostat, and `2` is Mirostat 2.0. */ mirostat?: number; /** * Set the Mirostat target entropy, parameter tau. Default: `5.0` */ mirostat_tau?: number; /** * Set the Mirostat learning rate, parameter eta. Default: `0.1` */ mirostat_eta?: number; /** * Set the DRY (Don't Repeat Yourself) repetition penalty multiplier. Default: `0.0`, which is disabled. */ dry_multiplier?: number; /** * Set the DRY repetition penalty base value. Default: `1.75` */ dry_base?: number; /** * Tokens that extend repetition beyond this receive exponentially increasing penalty: multiplier * base ^ (length of repeating sequence before token - allowed length). Default: `2` */ dry_allowed_length?: number; /** * How many tokens to scan for repetitions. Default: `-1`, where `0` is disabled and `-1` is context size. */ dry_penalty_last_n?: number; /** * Specify an array of sequence breakers for DRY sampling. Only a JSON array of strings is accepted. Default: `['\n', ':', '"', '*']` */ dry_sequence_breakers?: string[]; /** * Top n sigma sampling as described in academic paper "Top-nσ: Not All Logits Are You Need" https://arxiv.org/pdf/2411.07641. Default: `-1.0` (Disabled) */ top_n_sigma?: number; /** * Ignore end of stream token and continue generating. Default: `false` */ ignore_eos?: boolean; /** * Modify the likelihood of a token appearing in the generated text completion. * For example, use `"logit_bias": [[15043,1.0]]` to increase the likelihood of the token 'Hello', or `"logit_bias": [[15043,-1.0]]` to decrease its likelihood. * Setting the value to false, `"logit_bias": [[15043,false]]` ensures that the token `Hello` is never produced. The tokens can also be represented as strings, * e.g.`[["Hello, World!",-0.5]]` will reduce the likelihood of all the individual tokens that represent the string `Hello, World!`, just like the `presence_penalty` does. * Default: `[]` */ logit_bias?: [number, number | false][]; /** * Set the random number generator (RNG) seed. Default: `-1`, which is a random seed. */ seed?: number; emit_partial_completion: boolean; }</code>


#### CompletionBaseParams

<code>{ prompt?: string; messages?: LlamaOAICompatibleMessage[]; chat_template?: string; jinja?: boolean; tools?: <a href="#record">Record</a>&lt;string, unknown&gt;; parallel_tool_calls?: <a href="#record">Record</a>&lt;string, unknown&gt;; tool_choice?: string; response_format?: <a href="#completionresponseformat">CompletionResponseFormat</a>; }</code>


#### LlamaOAICompatibleMessage

<code>{ // TODO: which values are valid? role: string; // 'user' | 'prompt' | 'model'; content?: string; }</code>


#### CompletionResponseFormat

<code>{ type: 'text' | 'json_object' | 'json_schema'; json_schema?: { strict?: boolean; schema: object; }; schema?: object; // for json_object type }</code>

</docgen-api>
