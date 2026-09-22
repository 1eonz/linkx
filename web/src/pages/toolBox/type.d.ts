export interface LayerManageOptions {
  checked: boolean;
  childIcon?: boolean;
  children?: LayerManageOptions[];
  icon: string;
  id: string;
  name: string;
  show?: boolean;
}
