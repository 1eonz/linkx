export interface Tree {
  children?: Tree[];
  disabled?: boolean;
  id: number | string;
  isLeaf?: boolean;
  label: string;
  more?: boolean;
}

export interface LeafFn {
  leafs: any[];
  total: number;
}
