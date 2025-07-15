// @ts-check
import { LlamaContext, Llama } from 'capacitor-llama';
import { Filesystem, Directory } from '@capacitor/filesystem';

function showBtn(id) {
  const btn = document.getElementById(id)
  if (!(btn instanceof HTMLButtonElement)) {
    console.error(id, 'btn not found')
    return
  }
  btn.disabled = false
  btn.style.display = 'block'
}

function hideBtn(id) {
  const btn = document.getElementById(id)
  if (!(btn instanceof HTMLButtonElement)) {
    console.error(id, 'btn not found')
    return
  }
  btn.disabled = true
  btn.style.display = 'none'
}

// @ts-ignore
window.downloadModel = async () => {
  await Filesystem.downloadFile({
    url: 'https://huggingface.co/medmekk/Qwen2.5-0.5B-Instruct.GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
    directory: Directory.Documents,
    path: 'Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
  }).then(result => console.log('download model', result.path)).catch(e => console.error('error during download', e))
  hideBtn('downloadModel')
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

  const releaseBtn = document.getElementById('loadModel')
  if (!(releaseBtn instanceof HTMLButtonElement)) {
    console.error('release btn not found')
    return
  }
  if (!modelPath) {
    console.error('Invalid model uri: ', result.uri)
    return
  }
  console.log('model path: ', modelPath)
  context = await LlamaContext.from({
    // for ios and android
    // model: modelPath,
    // for electron
    // model: /path/to/model.gguf,
    // for browser
    model: 'https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
    use_mlock: true,
    n_ctx: 2048,
    n_gpu_layers: 0,
    
  })

  hideBtn('loadModel')
  showBtn('releaseModel')
  console.log('loaded model: ', context.model)
  console.log('result', context.id, context.model.desc, context.model.size, context.model.nVocab)
}

// @ts-ignore
window.getVocab = (p) => Llama.getVocab(p) 

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

let chat = [
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

function clearChat() {
  chat = []
  updateMessagesLayout()
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

window.releaseModel = async () => {
  await context.release()
  clearChat()
  showBtn('loadModel')
  hideBtn('releaseModel')
}


hideBtn('releaseModel')
hideBtn('downloadModel')