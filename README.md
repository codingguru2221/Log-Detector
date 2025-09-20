

# 🔒 Portable Offline Log Analysis Tool

## 📌 Overview

The **Portable Offline Log Analysis Tool** is a cross-platform Java application designed for **air-gapped / isolated networks** where data leakage must be nearly impossible.
It provides a lightweight, self-contained Security Operations Center (SOC)-like capability that can:

* Collect system and network logs,
* Parse and normalize them into a structured format,
* Detect suspicious activity using rule-based, signature, heuristic, and anomaly detection,
* Generate real-time alerts via popup notifications,
* Store all logs securely using encryption so that only authenticated users can access them.

This tool is completely **offline** and requires **no cloud dependency**, ensuring data privacy and full functionality in restricted environments.

---

## ✨ Key Features

* **Cross-Platform Portability** → Runs on Windows, Linux, and macOS with minimal setup.
* **Multi-source Log Collection** → Collect logs from system files, syslog (UDP/TCP), FTP, or USB imports.
* **Parsing & Normalization** → Converts heterogeneous log formats into a unified schema.
* **Threat Detection** → Identifies malicious behavior using:

  * Rule/Signature-based matching (TTPs)
  * Heuristic checks
  * Simple anomaly detection (event frequency spikes, unusual access patterns)
* **Real-Time Alerts** → Popup notifications for suspicious activity.
* **Secure Log Storage** → AES-encrypted database, access restricted to authenticated users.
* **User Interface** → JavaFX-based dashboard for viewing logs, alerts, and reports.
* **Offline Updates** → Rules and threat intelligence feeds can be imported via USB or local files.
* **Reporting** → Generate and export security reports in CSV/PDF.

---

## 🏗️ System Architecture

```
Collectors  →  Parsers  →  Normalizer  →  Encrypted Storage  
                       ↘→ Detection Engine → Alerts (Popup/UI)
```

**Core Modules:**

* **Collectors:** SyslogCollector, FileCollector, USBCollector, FTPCollector
* **Parsers:** SyslogParser, WindowsEventParser, ApacheLogParser
* **Detection:** RuleEngine, AnomalyDetector
* **Security:** Authentication, Encryption (AES-GCM), Key Management
* **UI:** JavaFX dashboard (login, live events, alerts, reports)

---

## 🛡️ Security Highlights

* AES-256-GCM encrypted log storage
* Password hashing with Argon2/Bcrypt
* Role-based access control (Admin, Analyst, Viewer)
* No external network connections → **air-gapped safe**

---

## 📂 Project Structure

```
log-analysis-tool/
├── src/main/java/com/sih/logtool/
│   ├── collectors/       # Log collectors
│   ├── parsers/          # Log parsers & normalizers
│   ├── detection/        # Rule engine & anomaly detection
│   ├── storage/          # Encrypted DB and repositories
│   ├── security/         # Auth & crypto services
│   ├── ui/               # JavaFX UI components
│   └── util/             # Config & utilities
├── resources/
│   ├── rules/            # Detection rules (JSON/YAML)
│   └── config/           # Config files
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

* Java 17+
* Maven or Gradle
* (Optional) Git installed

### Installation

```bash
# Clone repository
git clone https://github.com/your-username/log-analysis-tool.git
cd log-analysis-tool

# Build project
mvn clean install
```

### Running the Application

```bash
java -jar target/log-analysis-tool.jar
```



## 📊 Example Use Case

1. Import system and firewall logs via USB into the tool.
2. The parser normalizes logs into a structured format.
3. The detection engine identifies multiple failed login attempts from a suspicious IP.
4. A **popup alert** is triggered on the analyst’s system.
5. Logs remain **AES-encrypted** in the database, accessible only to authenticated users.
6. Analyst exports a **security report (PDF/CSV)** for further action.



## 🤝 Contributing

Contributions are welcome! Please fork this repository and create a pull request for review.

⚡ With this tool, organizations operating in isolated networks can achieve **real-time situational awareness, secure log monitoring, and proactive cyber threat detection** — without depending on cloud or external services.



Do you want me to also add **badges (like build status, license, Java version)** and a **logo/banner** section at the top so your GitHub repo looks more professional?
