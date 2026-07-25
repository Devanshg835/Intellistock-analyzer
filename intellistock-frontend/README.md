# IntelliStock Frontend (React + Vite)

This is the frontend dashboard for IntelliStock, delivering a premium glassmorphic UI using modern CSS variables and modular components.

## Tech Stack
- **Framework**: React (Vite template)
- **Styling**: Custom Vanilla CSS (with responsive tokens, CSS variables, dark layout, and glassmorphism cards)
- **HTTP Client**: Axios (configured in `src/api.js`)

## Directory Structure
- `src/api.js`: Axios configuration mapping the backend base URL (`http://localhost:8080/api`) and methods.
- `src/App.jsx`: Main App orchestrator, holding search states, error indicators, loading spinners, and result mapping.
- `src/index.css`: Custom CSS variables and layout styles.
- `index.html`: Page shell loading Google Fonts (Outfit & Inter).

## How to Run Locally

### Prerequisites
- Node.js (v18 or higher) and npm installed.

### Commands

1. **Install Dependencies**:
   Navigate to the `intellistock-frontend` directory and run:
   ```bash
   npm install
   ```

2. **Start Dev Server**:
   ```bash
   npm run dev
   ```

The application will launch on `http://localhost:5173/`. Ensure the backend server is running on `http://localhost:8080` to successfully fetch stock details.
