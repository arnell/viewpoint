import { useRef, useEffect, useState } from 'react';
import axios from "axios";

import './App.css';
import Segment from './Segment';

function App() {
  let ref = useRef(null);
  const [width, setWidth] = useState(0);
  const [sectionInfo, setSectionInfo] = useState([]);

  useEffect(() => {
    axios.get("/api/image/segment/query").then((response) => {
      setSectionInfo(response.data);
    })
  }, [])

  useEffect(() => {
    console.log('width', ref.current ? ref.current.offsetWidth : 0);
    setWidth(ref.current.offsetWidth);
    // to handle page resize
    const getwidth = () => {
      setWidth(ref.current.offsetWidth);
    }
    window.addEventListener("resize", getwidth);
  }, []);

  return (
    <div className="App">
      <div className="sidebar">
      </div>
      <div className="main" ref={ref} style={{  }}>
        {sectionInfo.map((s) => (
          <Segment key={s.id} startDate={s.startDate} endDate={s.endDate} width={width} count={s.count} />
        ))}
      </div>
    </div>
  );
}

export default App;
