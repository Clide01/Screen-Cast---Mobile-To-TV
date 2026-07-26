import React, { useState, useEffect, useRef } from 'react';
import VideoPlayer from './components/VideoPlayer';

export default function App() {
  const [stream, setStream] = useState(null);
  const [status, setStatus] = useState('Ready for connection');
  const [deviceName] = useState('Smart TV - Living Room'); // Customizable name
  
  const pcRef = useRef(null);
  const wsRef = useRef(null);

  useEffect(() => {
    // Start WebSocket server listener / signaling connection on load
    const ws = new WebSocket(`ws://${window.location.hostname}:8080`);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus('Waiting for Phone to select this TV...');
      initWebRTC(ws);
    };

    ws.onmessage = async (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'offer') {
        await pcRef.current.setRemoteDescription(new RTCSessionDescription(data.sdp));
        const answer = await pcRef.current.createAnswer();
        await pcRef.current.setLocalDescription(answer);

        ws.send(JSON.stringify({ type: 'answer', sdp: answer }));
        setStatus('Streaming active!');
      } else if (data.type === 'candidate' && data.candidate) {
        await pcRef.current.addIceCandidate(new RTCIceCandidate(data.candidate));
      }
    };

    ws.onclose = () => setStatus('Disconnected. Retrying...');
    ws.onerror = () => setStatus('Waiting for connection...');

    return () => ws.close();
  }, []);

  const initWebRTC = (ws) => {
    const pc = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
    });
    pcRef.current = pc;

    pc.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        setStream(event.streams[0]);
      }
    };

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        ws.send(JSON.stringify({ type: 'candidate', candidate: event.candidate }));
      }
    };
  };

  return (
    <div style={{
      fontFamily: 'system-ui, sans-serif',
      color: '#fff',
      backgroundColor: '#0f172a',
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center'
    }}>
      {stream ? (
        <VideoPlayer stream={stream} />
      ) : (
        <div style={{ textAlign: 'center', padding: '40px', borderRadius: '16px', background: '#1e293b' }}>
          <div style={{ fontSize: '3rem', marginBottom: '10px' }}>📺</div>
          <h1 style={{ fontSize: '2.2rem', margin: '0 0 10px 0' }}>{deviceName}</h1>
          <p style={{ color: '#94a3b8', fontSize: '1.1rem' }}>Status: <strong style={{ color: '#38bdf8' }}>{status}</strong></p>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '20px' }}>
            Open the AppCaster app on your phone to start casting.
          </p>
        </div>
      )}
    </div>
  );
}
