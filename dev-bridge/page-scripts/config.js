export function bridgeConfig(target) {
  const query = new URLSearchParams(location.search);
  const session = query.get('bridgeSession') || 'local';
  if (!/^[\w.-]{1,100}$/.test(session)) throw new Error('Invalid bridgeSession');
  const url = new URL(query.get('bridgeUrl') || `/__dev_bridge/${target}/socket`, location.href);
  if (url.protocol === 'http:') url.protocol = 'ws:';
  if (url.protocol === 'https:') url.protocol = 'wss:';
  if (!['ws:', 'wss:'].includes(url.protocol)) throw new Error('bridgeUrl must use ws:// or wss://');
  if (location.protocol === 'https:' && url.protocol !== 'wss:') throw new Error('HTTPS pages require WSS; use the same-origin Vite proxy');
  return { target, session, url: url.href, token: query.get('bridgeToken') || '' };
}

export function assertJsonValue(value, seen = new Set(), path = 'value') {
  if (value === undefined || value === null || typeof value === 'string' || typeof value === 'boolean') return;
  if (typeof value === 'number' && Number.isFinite(value)) return;
  if (typeof value !== 'object') throw new Error(`Unsupported bridge ${path}: ${typeof value}`);
  if (seen.has(value)) throw new Error(`Unsupported circular bridge ${path}`);
  if (!Array.isArray(value) && Object.getPrototypeOf(value) !== Object.prototype && Object.getPrototypeOf(value) !== null) throw new Error(`Unsupported bridge ${path}: ${value.constructor?.name}`);
  seen.add(value);
  for (const [key, item] of Object.entries(value)) assertJsonValue(item, seen, `${path}.${key}`);
  seen.delete(value);
}
