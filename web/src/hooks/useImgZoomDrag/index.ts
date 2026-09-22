// 图片放大移动的指令
export const useImgZoomDrag = (el, callback) => {
  const oDiv = el;
  let startX = 0;
  let startY = 0;
  let left = 0;
  let top = 0;
  let widthDiff = 0;
  let heightDiff = 0;
  let δ = 0;

  // 计算出盒子的大小和图片的大小
  oDiv.addEventListener('mousedown', (e) => {
    e.preventDefault();
    const imageWidth = el.clientWidth;
    const imageHeight = el.clientHeight;
    const stageWidth = el.parentNode.clientWidth;
    const stageHeight = el.parentNode.clientHeight;
    startX = e.type === 'mousedown' ? e.pageX : e.clientX;
    startY = e.type === 'mousedown' ? e.pageY : e.clientY;
    // δ是图像宽度和高度之间的差异
    δ = 0;
    widthDiff = imageWidth - stageWidth;
    heightDiff = imageHeight - stageHeight;
    left = oDiv.offsetLeft - δ;
    top = oDiv.offsetTop + δ;

    document.addEventListener('mousemove', (e) => {
      const endX = e.type === 'mousemove' ? e.pageX : e.clientX;
      const endY = e.type === 'mousemove' ? e.pageY : e.clientY;
      const relativeX = endX - startX;
      const relativeY = endY - startY;
      let newLeft = relativeX + left;
      let newTop = relativeY + top;
      // 垂直限制
      if (heightDiff > 0) {
        if (relativeY + top > δ) {
          newTop = δ;
        } else if (relativeY + top < -heightDiff + δ) {
          newTop = -heightDiff + δ;
        }
      } else {
        newTop = top;
      }
      // 水平限制
      if (widthDiff > 0) {
        if (relativeX + left > -δ) {
          newLeft = -δ;
        } else if (relativeX + left < -widthDiff - δ) {
          newLeft = -widthDiff - δ;
        }
      } else {
        newLeft = left;
      }
      oDiv.style.left = `${newLeft}px`;
      oDiv.style.top = `${newTop}px`;
      callback({ left: newLeft, top: newTop });
    });

    document.addEventListener('mouseup', () => {
      document.onmousemove = null;
      document.onmouseup = null;
    });
  });
};
