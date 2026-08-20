# SpeakSmart 🎙️

**SpeakSmart** is a modern, privacy-focused Android application built with Jetpack Compose, MediaPipe LLM Inference (on-device AI), and Android SpeechRecognizer. It acts as your personal private English tutor, analyzing your spoken English, identifying grammatical mistakes, suggesting vocabulary improvements, and generating interactive micro-quizzes.

---

## ✨ Features

- 🎤 **Hold-to-Speak Interface**: Intuitive microphone button with pulsing glow animations and real-time audio RMS visualization.
- 🗣️ **Real-Time Speech-to-Text**: Built-in offline speech recognition for instant transcription.
- 🧠 **On-Device LLM Integration**: Powered by Google MediaPipe LLM Inference API for running local models (like Gemma 2B) completely offline.
- ⚡ **Smart Built-in Grammar Engine**: Includes an automatic fallback rule-based analysis engine when an LLM model file is not present.
- 🎯 **AI English Tutor System Prompt**: Automatically structures feedback into grammar corrections, vocabulary enhancements, and quick multiple-choice quizzes.
- 🌙 **Premium Dark Theme**: Sleek, glassmorphic UI with custom color palettes and smooth Compose animations.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.3
- **UI Framework**: Jetpack Compose (Material3)
- **Speech Recognition**: Android `SpeechRecognizer` API
- **AI / ML**: MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai:0.10.27`)
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Minimum SDK**: API Level 24 (Android 7.0)

---

## 🚀 Quick Setup & Build

### 1. Build the APK
```bash
./gradlew assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Run on Device
```bash
android run
# Or via adb:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Load a Local LLM Model (Optional)
To enable full on-device Gemma inference:
```bash
adb shell mkdir -p /data/local/tmp/llm/
adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/llm/model.bin
```

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for details.
