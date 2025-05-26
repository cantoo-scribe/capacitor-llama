// @ts-check
import { LlamaContext } from 'capacitor-llama';
import { Filesystem, Directory } from '@capacitor/filesystem';

// @ts-ignore
window.downloadModel = () => {
  Filesystem.downloadFile({
    url: 'https://huggingface.co/medmekk/Qwen2.5-0.5B-Instruct.GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
    directory: Directory.Documents,
    path: 'Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
  }).then(result => console.log('download model', result.path)).catch(e => console.error('error during download', e))
}

/** @type {LlamaContext} */
let context
// @ts-ignore
window.loadModel = async () => {
  const result = await Filesystem.getUri({
    path: 'Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
    directory: Directory.Documents,
  })
  const modelPath = result.uri.split('file://').pop()
  const loadBtn = document.getElementById('loadModel')
  if (!(loadBtn instanceof HTMLButtonElement)) {
    console.error('load btn not found')
    return
  }
  if (!modelPath) {
    console.error('Invalid model uri: ', result.uri)
    return
  }
  console.log('model path: ', modelPath)
  context = await LlamaContext.from({
    model: modelPath,
    use_mlock: true,
    n_ctx: 2048,
    n_gpu_layers: 0,
    
  })
  loadBtn.disabled = true
  loadBtn.style.display = 'none'
  console.log('loaded: ', JSON.stringify(context.model.metadata))
  console.log('result', context.id, context.model.desc, context.model.size)
}

const stopWords = [
  "</s>",
  "<|end|>",
  "user:",
  "assistant:",
  "<|im_end|>",
  "<|eot_id|>",
  "<|end▁of▁sentence|>",
  "<|end_of_text|>",
  "<｜end▁of▁sentence｜>",
];

const chat = [
  {
    role: "system",
    content:
      "This is a conversation between user and assistant, a friendly chatbot.",
  },
]

function updateMessagesLayout() {
  const messagesContainer = document.getElementById('messagesContainer')
  if (!messagesContainer) return
  messagesContainer.innerHTML = chat.map(message => {
    const { role, content } = message
    if (role === 'system') return ''
    return `<div class="message ${role === 'user' ? 'sent' : 'received'}">${content}</div>`
  }).join('\n')
}

function pushMessage(message) {
  chat.push(message)
  updateMessagesLayout()
}

// @ts-ignore
window.completion = async () => {
  const messageInput = document.getElementById('messageInput')
  if (!(messageInput instanceof HTMLInputElement)) return
  pushMessage({
    role: 'user',
    content: messageInput.value
  })
  messageInput.value = ''
  const { content } = await context.completion({
    messages: chat,
    stop: stopWords,
    n_predict: 1000,
  })
  pushMessage({
    role: 'assistant',
    content
  })
}
