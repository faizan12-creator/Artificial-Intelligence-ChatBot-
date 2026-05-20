# *\*JavaAI — Premium JavaFX AI Chatbot*

JavaAI is a modern, responsive, and asynchronous desktop AI assistant built using **JavaFX** and powered by the **Llama 3.1 Model** via the Groq Cloud API. It provides an enterprise-grade UI experience with real-time analytics, smooth custom-rendered layouts, and reliable local state persistence.

---

# ## 🚀 Key Features

* **Modern UI Engine:** Built fully in JavaFX with absolute positioning animations, fluid transition interpolations, and dynamic window scaling support.
* **Asynchronous Architecture:** Heavy network operations and Groq API calls are isolated into background worker threads, preventing any UI freezing or thread blocking.
* **Smart Markdown Parsing:** Automatically parses and renders structural elements such as headers, bulleted lists, and block quotes inside message windows.
* **Session Management & Persistence:** Utilizes structured local JSON storage to handle automatic tracking, serialization, and restoration of past chat histories.
* **Real-time Metrics Dashboard:** Displays live token consumption trackers ($RPM$/$TPM$) along with granular parameter tuning (temperature, system constraints).

---

## 🛠️ Software Architecture

The project adheres strictly to the **Model-View-Controller (MVC)** design pattern and classic Object-Oriented Programming (OOP) principles to enforce clear separation of concerns.
### Core Components Breakdown
1. **`Main.java`**: Configures the underlying operating system environment windows, initializes foundational stage assets, and anchors the primary view.
2. **`AIChatBot.java`**: Acts as the central controller orchestrating interactive UI events, text field event listeners, and overall UI state shifts.
3. **`APIService.java`**: Houses the network communication client logic, generating payload mappings and parsing response tokens over secure channels.
4. **`BubbleBuilder.java`**: A reusable UI rendering engine dedicated to generating custom-styled chat bubble bubbles with precise internal text padding.
5. **`ChatSession.java`**: Handles local disk I/O routines, reading and updating local JSON files to serialize historical string arrays across runtime boots.

---

## ⚙️ Setup & Installation Instructions

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher recommended.
* **JavaFX SDK:** Version 21+ configured within your runtime environment.
* **IDE:** IntelliJ IDEA (Ultimate or Community Edition).

### Step-by-Step Configuration

1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/faizan12-creator/Artificial-Intelligence-ChatBot.git](https://github.com/faizan12-creator/Artificial-Intelligence-ChatBot.git)
