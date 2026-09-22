import { writeFileSync } from 'fs';

// eslint-disable-next-line n/no-extraneous-import
import fg from 'fast-glob';

/**
 * 获取src下所有文件
 */
async function getAllFiles(pattern) {
  const entries = await fg.glob([pattern], { dot: true });
  return entries;
}

/**
 * 找到未引用的文件并输出在unused-files.json文件内
 */
export const unusedFilesPlugin = () => {
  let config;
  const loadFiles: Set<string> = new Set();
  const output = './unused-files.json';

  return {
    async buildEnd() {
      const pattern = `${config.root}/src/**/*`;
      const allFiles = await getAllFiles(pattern);
      // TODO:还需要对图片和样式文件等判断是否引用...
      const unusedFiles: string[] = [];
      allFiles.forEach((i) => {
        if (!loadFiles.has(i) && /\.vue|\.ts/.test(i)) {
          unusedFiles.push(i.replace(config.root, ''));
        }
      });
      writeFileSync(output, JSON.stringify(unusedFiles, null, 4));
      console.log('buildEnd');
    },
    buildStart() {
      // start
      writeFileSync(output, JSON.stringify([], null, 4));
    },
    configResolved(resolvedConfig) {
      config = resolvedConfig;
    },
    load(id) {
      if (id.includes(`${config.root}/src`)) {
        loadFiles.add(id);
      }
    },
    name: 'read-config',
  };
};
