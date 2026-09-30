# S.A.M. — Smart Autonomous Machine

> A premium Android AI voice assistant with real device automation, multi-AI provider support, and a futuristic animated interface.

![Platform](https://img.shields.io/badge/Platform-Android-blue)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-green)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-orange)

---

## Features

- **Voice-Driven Assistant** — Wake word ("Hey Sam"), speech-to-text, text-to-speech, conversation mode
- **Real Phone Automation** — Open apps, send WhatsApp messages, set alarms, Google search, Maps navigation via Android Intents + AccessibilityService
- **Multi-Step Task Engine** — AI-planned tasks with sequential execution and verification before completion
- **SAM Overlay** — Dynamic Island-style floating capsule that appears over other apps during voice interaction
- **Background Engine** — Foreground service keeps SAM listening for wake word even when app is closed
- **Multi-AI Providers** — OpenAI, Google Gemini, Anthropic Claude, with pluggable architecture
- **Premium Animated Orb** — 7 states, 6 types, customizable aura/particles/rings/glow
- **Wikipedia Knowledge** — Free API integration for instant answers
- **Memory System** — Persistent local storage (Room) for preferences, notes, and context
- **Coding Studio** — Built-in editor with language detection and preview
- **Developer Mode** — Protected debug/diagnostics panel

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.49 |
| Database | Room 2.6 |
| Network | OkHttp 4.12 + Retrofit 2.11 |
| Preferences | DataStore Preferences |
| Navigation | Navigation Compose |
| Voice | SpeechRecognizer + TextToSpeech |
| Automation | AccessibilityService + Intents |
| Build | Gradle Kotlin DSL + Version Catalog |

---

## Architecture

```
com.samai.assistant/
├── ai/                 → Model router, providers, brain, prompts
├── androidcontrol/     → AccessibilityService, control engine, app registry
├── coding/             → Coding studio engine
├── di/                 → Hilt modules
├── engine/             → Background service, task execution engine
├── knowledge/          → Wikipedia integration
├── memory/             → Room database, memory manager
├── navigation/         → Compose navigation, screen routes
├── overlay/            → Floating overlay (Dynamic Island style)
├── profile/            → User profile, developer identity (immutable)
├── settings/           → Settings repository & viewmodel
├── task/               → Task models, AI-based planner
├── ui/
│   ├── components/     → SAMOrb, VoiceOrb, shared components
│   ├── screens/        → Home, Chat, SAM Core, Discover, Settings, etc.
│   └── theme/          → Dark futuristic colors, typography
├── utils/              → Safety, permissions, network monitor
└── voice/              → Voice manager, wake word service
```

---

## Setup

### Prerequisites
- Android Studio Ladybug+ (2024)
- JDK 17
- Android SDK 35

### Build

```bash
# Clone
git clone https://github.com/theshlok18/v-app2.git
cd v-app2

# Open in Android Studio, or build from CLI:
./gradlew assembleDebug
```

### Configuration
1. Open **Settings → AI Models** inside the app
2. Add your API key (OpenAI / Gemini / Anthropic)
3. Enable **Accessibility Service** when prompted
4. Grant **Microphone** and **Overlay** permissions
5. Toggle **SAM Engine ON** in settings for background wake word

---

## Screenshots (UI States)

| Home | Chat | Orb Customization |
|------|------|-------------------|
| Animated core + quick actions | Futuristic bubbles + voice input | Live preview + types/auras |

---

## Security & Privacy

- No root required
- No hidden APIs or exploits
- Official Android Intents + AccessibilityService only
- Sensitive actions (messages, calls) require explicit user confirmation
- API keys stored locally via DataStore
- No data leaves the device except AI API calls you initiate

---

## Developer

| Field | Value |
|-------|-------|
| Name | Shlok |
| Role | Data Science Student |
| GitHub | [@theshlok18](https://github.com/theshlok18) |
| Instagram | [@iishlok23](https://instagram.com/iishlok23) |

---

## License

This project is for educational and personal use. All original code.

---

*Built with a vision of what a personal AI should feel like — not a chatbot, but an agent that acts.*
