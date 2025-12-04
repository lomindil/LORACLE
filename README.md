# Loracle  
### A Privacy-First, On-Device AI Assistant for Android  
### Powered by Ollama running inside Termux  

Loracle is a **privacy-first AI assistant** designed for Android.  
Instead of sending your data to the cloud, Loracle runs fully-local LLMs using **Ollama inside Termux**, ensuring:

- 💬 Private chat conversations  
- 🔊 Voice input via Android Speech Recognizer  
- 🔈 Natural voice responses via Text-to-Speech  
- 🔌 Local + Remote LLM support  
- 🧩 Modular architecture for future on-device LLMs (llama.cpp)  

Loracle supports **local Ollama instances** running entirely on-device via Termux, and it can also connect to a **remote Ollama server** if configured.

---

## ✨ Features

### 🔒 **100% Privacy-Preserving (Local-First)**
All AI processing happens **locally on your phone** via Termux + Ollama.  
Zero cloud APIs. Zero data leaves your device.

### 🤖 **Chat with Local LLMs**
Supports any model available in Ollama, including:

- `gemma:2b`
- `qwen2:1.5b`
- `llama3.2`
- `phi3:mini`
- and any GGUF/GGML-compatible Ollama model

### 🎙️ **Voice Assistant Mode**
- Uses Android’s built-in **SpeechRecognizer** for voice input  
- Replies using Android **Text-to-Speech (TTS)**  
- Hands-free "voice chat" experience entirely offline

### 🔗 **Local & Remote LLM**
- Auto-connect to **Termux local Ollama server (`http://127.0.0.1:11434`)**
- Optional support for custom remote endpoints

### 🛠️ **Designed for Extensibility**
- Modular architecture
- Future-ready for **integrating llama.cpp models** natively (WIP)
