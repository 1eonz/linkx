declare module '*.png' {
  const value: string;
  export default value;
}

declare module "*.svg" {
  const value: string;
  export default value;
}
 
interface Window {
  WeSpaceSDK?: {
    onFloorRequestResult?: (callback: (res: { data: boolean }) => void) => void;
    onTaken?: (callback: (res: { groupId: string | number; speaker: string }) => void) => void;
    onIdle?: (callback: (res: { groupId: string | number; speaker: string }) => void) => void;
    [key: string]: any;
  };
}
