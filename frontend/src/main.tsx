import React from 'react';
import ReactDOM from 'react-dom/client';
import { GoogleOAuthProvider } from '@react-oauth/google';
import App from './App';
import './styles.css';

const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;
const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);

root.render(
  <React.StrictMode>
    {clientId ? (
      <GoogleOAuthProvider clientId={clientId}>
        <App googleOAuthEnabled />
      </GoogleOAuthProvider>
    ) : (
      <App googleOAuthEnabled={false} />
    )}
  </React.StrictMode>
);
