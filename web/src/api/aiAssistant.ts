import http from '@/utils/http';

export const aiQuery = (data: any) =>
  http.post<any>(
    '/mpp-gateway/rtx-ydjw-chatAyena/api/independence/chatSyncAPI/chat/completions',
    data,
  );
