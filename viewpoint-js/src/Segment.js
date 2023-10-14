import { useRef, useState, useMemo, useEffect } from "react";
import axios from "axios";
import justifiedLayout from "justified-layout";

import useOnScreen from "./useOnScreen";

const buildInitialImageInfo = (count) => {
  const info = [];
  for (let i = 0; i < count; i++) {
    info.push({id: i, ratio: 1, thumbPath: 'placeholder.jpg'})
  }
  return info;
}

const Segment = ({width, startDate, endDate, count}) => {
    const ref = useRef(null);
    const isVisible = useOnScreen(ref);
    const [height, setHeight] = useState(500);
    const [imageInfo, setImageInfo] = useState(buildInitialImageInfo(count));

    useEffect(() => {
      axios.get(`/api/image/query?startDate=${startDate}&endDate=${endDate}`).then((response) => {
        setImageInfo(response.data);
      })
    }, [])

    let layoutGeometry;
    
    if (isVisible && imageInfo.length) {
      layoutGeometry = justifiedLayout(imageInfo.map((i)=>i.ratio), { containerWidth: width });
      let last = layoutGeometry.boxes[layoutGeometry.boxes.length - 1];
      if (height !== last.top + last.height + 10) {
        setHeight(last.top + last.height + 10);
      }
    } else {
      layoutGeometry = {boxes: []};
    }
    //console.log("layout", name, isVisible);
    if (isVisible) {
      //console.log(layoutGeometry);
    }
    
    const images = layoutGeometry.boxes.map((b, i) => {
      
      return (
        <div key={imageInfo[i].id} style={{
          position: 'absolute',
          transform: `translate(${b.left}px, ${b.top}px)`,
          height: b.height, width: b.width, backgroundColor: 'red'
        }}>
          {/* <img src={`https://source.unsplash.com/random/?_r=${imageInfo[i].thumbPath}`} width="100%" height="100%" /> */}
          <img src={`/api${imageInfo[i].thumbPath}`} width="100%" height="100%" />
          </div>
      )});

    return (
        <div ref={ref}>
            <div className='header'>{startDate + ' ' + endDate}</div>
            <div className='images' style={{ height: height + 'px' }}>
                {isVisible && images}
            </div>
        </div>
    );
};

export default Segment;