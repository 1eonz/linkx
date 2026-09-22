export interface ConfMember {
  [propName: string]: any;
  account: string;
  category?: number | string;
  code?: string;
  id?: string;
  isChairman?: number;
  isMute?: string;
  isVideo?: number;
  name: string;
  participantStatus?: string;
}

export interface AddConfMembersParam {
  h265: string;
  isCamera: string;
  isWatchOnly: string;
  name: string;
  number: string;
}

export interface AddConferItemParams {
  account: string;
  accountType: number | string;
  confId: string;
  isChairman: number;
  name: string;
}
