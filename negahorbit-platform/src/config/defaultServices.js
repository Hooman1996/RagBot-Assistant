export const DEFAULT_SERVICES_CONFIG = {
  "platform": "NegahOrbit",
  "version": "2.0.0",
  "defaultTheme": "obsidian-champagne",
  "categories": [
    {
      "id": "chatbots",
      "chapter": "01",
      "orbitName": "Orbit 01 — Conversational Intelligence",
      "sectionIndex": 1,
      "title": "CHATBOTS"
    },
    {
      "id": "asr",
      "chapter": "02",
      "orbitName": "Orbit 02 — Acoustic & Voice Engines",
      "sectionIndex": 2,
      "title": "ASR"
    },
    {
      "id": "assistant",
      "chapter": "03",
      "orbitName": "Orbit 03 — Autonomous Banking Core",
      "sectionIndex": 3,
      "title": "AGENTIC\nASSISTANT"
    },
    {
      "id": "ocr",
      "chapter": "04",
      "orbitName": "Orbit 04 — Optical Document Vision",
      "sectionIndex": 4,
      "title": "OCR"
    }
  ],
  "services": [
    {
      "id": "hibot-chatbot",
      "order": 1,
      "category": "chatbots",
      "title": "Hibot",
      "kicker": "Chatbot",
      "tagline": "A friendly assistant that answers questions about Negah services in plain language.",
      "url": "TODO: http://10.0.0.10:8001",
      "openIn": "same-tab",
      "status": "live",
      "form": "speech-bubbles"
    },
    {
      "id": "negahboom-chatbot",
      "order": 2,
      "category": "chatbots",
      "title": "NegahBoom",
      "kicker": "Chatbot",
      "tagline": "Conversational guidance tailored for NegahBoom ecosystem services and customer inquiries.",
      "url": "TODO: http://10.0.0.10:8002",
      "openIn": "same-tab",
      "status": "live",
      "form": "ecosystem-globe"
    },
    {
      "id": "crm-chatbot",
      "order": 3,
      "category": "chatbots",
      "title": "CRM",
      "kicker": "Chatbot",
      "tagline": "An intelligent copilot connected to CRM workflows to accelerate support and customer resolution.",
      "url": "TODO: http://10.0.0.10:8003",
      "openIn": "same-tab",
      "status": "live",
      "form": "crm-constellation"
    },
    {
      "id": "document-chatbot",
      "order": 4,
      "category": "chatbots",
      "title": "Document",
      "kicker": "Chatbot",
      "tagline": "Grounded Q&A over internal manuals, policies, and banking documents with instant citations.",
      "url": "TODO: http://10.0.0.10:8004",
      "openIn": "same-tab",
      "status": "live",
      "form": "rag-documents"
    },
    {
      "id": "stt-asr",
      "order": 5,
      "category": "asr",
      "title": "STT",
      "kicker": "Speech-to-Text",
      "tagline": "Real-time speech recognition that transcribes voice commands and calls with high accuracy.",
      "url": "TODO: http://10.0.0.10:8005",
      "openIn": "same-tab",
      "status": "live",
      "form": "microphone-wave"
    },
    {
      "id": "tts-asr",
      "order": 6,
      "category": "asr",
      "title": "TTS",
      "kicker": "Text-to-Speech",
      "tagline": "Natural, expressive neural voice synthesis built for banking assistants and voice channels.",
      "url": "TODO: http://10.0.0.10:8006",
      "openIn": "same-tab",
      "status": "live",
      "form": "speaker-synthesis"
    },
    {
      "id": "banking-agentic-assistant",
      "order": 7,
      "category": "assistant",
      "title": "Agentic Assistant",
      "kicker": "Banking App",
      "tagline": "An autonomous banking copilot that plans and executes multi-step financial actions inside the app.",
      "url": "/banking/",
      "openIn": "same-tab",
      "status": "live",
      "form": "agentic-card-orbit"
    },
    {
      "id": "ocr-vision",
      "order": 8,
      "category": "ocr",
      "title": "OCR",
      "kicker": "Document Vision",
      "tagline": "Instant optical character recognition and structured extraction for IDs, checks, and forms.",
      "url": "TODO: http://10.0.0.10:8008",
      "openIn": "same-tab",
      "status": "live",
      "form": "ocr-scanner"
    }
  ]
};
