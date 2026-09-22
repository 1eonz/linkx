const modules = import.meta.glob('@/static/emoji/*.png', { eager: true });
const bigModules = import.meta.glob('@/static/bigEmoji/*.png', { eager: true });

export const emojiList = Object.keys(modules).map((i) => {
  return i.replace('.png', '').split('/').pop();
});
export const bigEmojiList = Object.keys(bigModules).map((i) => {
  return i.replace('.png', '').split('/').pop();
});

// 一维数组变二维数组
export const chunkArray = (arr, chunkSize) => {
  return Array.from({ length: Math.ceil(arr.length / chunkSize) }, (_, index) =>
    arr.slice(index * chunkSize, index * chunkSize + chunkSize),
  );
};

export const emojiUrl: {
  [k: string]: string;
} = {};

export const loadEmoji = async () => {
  for (const i of emojiList) {
    const r = await import(`@/static/emoji/${i}.png`);
    emojiUrl[i as string] = r.default;
  }
};
loadEmoji();
