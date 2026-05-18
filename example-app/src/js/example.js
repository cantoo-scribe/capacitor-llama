// @ts-check
import { LlamaContext, Llama } from '@cantoo/capacitor-llama';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Capacitor } from '@capacitor/core';

const platform = Capacitor.getPlatform(); 
const directory = Directory.Data

const models = {
  'qwen2.5-0.5b': {
    path: 'Qwen2.5-0.5B-Instruct-Q5_K_S.gguf',
    url: 'https://huggingface.co/medmekk/Qwen2.5-0.5B-Instruct.GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q5_K_S.gguf'
  },
  'qwen3.5-0.8b': {
    path: 'Qwen3.5-0.8B-Q5_K_M.gguf',
    url: 'https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q5_K_M.gguf?download=true'
  }
}

/** @type { keyof models } */
const modelName = 'qwen3.5-0.8b'

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
  console.log('downloading model from url:', models[modelName].url)
  await Filesystem.downloadFile({
    url: models[modelName].url,
    directory,
    path: models[modelName].path,
  }).then(result => {
    console.log('download model', result.path)
    hideBtn('downloadModel')
  }).catch(e => console.error('error during download', e))
}

/** @type {LlamaContext} */
let context
// @ts-ignore
window.loadModel = async () => {
  const result = await Filesystem.getUri({
    path: models[modelName].path,
    directory,
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
  try {
  context = await LlamaContext.from({
    // for ios and android
    model: modelPath,
    // for electron
    // model: /path/to/model.gguf,
    // for browser
    // model: models[modelName].url,
    use_mlock: true,
    n_ctx: 2048,
    n_gpu_layers: 0,
    
  })

  hideBtn('loadModel')
  showBtn('releaseModel')
  showBtn('sendMessage')
  console.log('loaded model: ', context.model)
  console.log('result', context.id, context.model.desc, context.model.size, context.model.nVocab)
  } catch (err) {
    console.log('load failed', err)
  }
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

function updateLastMessage(token) {
  const last = chat[chat.length - 1]
  if (!last) return
  last.content += token
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
  pushMessage({
    role: 'assistant',
    content: ''
  })
  const { content } = await context.completion({
    messages: chat,
    stop: stopWords,
    n_predict: 300,
    onToken: (event) => {
      console.log('onToken', event)
      updateLastMessage(event.tokenResult.token)
    }
  })
  // pushMessage({
  //   role: 'assistant',
  //   content
  // })
}

// @ts-ignore
window.releaseModel = async () => {
  await context.release()
  clearChat()
  showBtn('loadModel')
  hideBtn('releaseModel')
  hideBtn('sendMessage')
}

;(async () => {
  console.log('platform: ', platform)
  if (platform === 'android') {
    // await Filesystem.deleteFile({
    //   path: models[modelName].path,
    //   directory,
    // }).then(() => console.log('model file deleted successfully'))
    //   .catch(() => console.log('error occurred while deleting the model file'))
    
    Filesystem.stat({
      path: models[modelName].path,
      directory,
    }).then(() => {
      hideBtn('downloadModel')
      console.log('the model is ready')
    }).catch(() => console.log('the model is missing'))
  } else if (platform === 'electron') {
    hideBtn('downloadModel')
  }
})()

hideBtn('releaseModel')
hideBtn('sendMessage')
// hideBtn('downloadModel')