/**
 * 根据鼠标点击位置，返回callWay弹窗打开位置
 * @param e
 * @returns dialog offset
 */
export function callWayDialogPosition(e) {
  const { innerHeight, innerWidth } = window;

  let x = e.clientX;
  let y = e.clientY;

  if (x > innerWidth - 200) {
    x = innerWidth - 200;
  }
  if (y > innerHeight - 160) {
    y = innerHeight - 160;
  }
  return [`${x}px`, `${y}px`];
}
