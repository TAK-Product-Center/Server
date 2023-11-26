import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import FileManager from './FileManager';
import MenuBar from './MenuBar';
import MissionManager from './MissionManager';

const rootElement = document.getElementById('root');
if (rootElement) {
  const root = ReactDOM.createRoot(rootElement);
  root.render(
    <React.StrictMode>
      <MenuBar />
      <FileManager />
    </React.StrictMode>
  );
}

const fileManagerElement = document.getElementById('fileManager');
if (fileManagerElement) {
  const root = ReactDOM.createRoot(fileManagerElement);
  root.render(
    <React.StrictMode>
      <MenuBar />
      <FileManager />
    </React.StrictMode>
  );
}

const misisonManagerElement = document.getElementById('missionManager');
if (misisonManagerElement) {
  const root = ReactDOM.createRoot(misisonManagerElement);
  root.render(
    <React.StrictMode>
      <MenuBar />
      <MissionManager />
    </React.StrictMode>
  );
}


