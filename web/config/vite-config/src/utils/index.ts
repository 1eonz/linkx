// Read all environment variable configuration files to process.env
export function wrapperEnv(envConf: Recordable): ViteEnv {
  const ret: any = {};

  for (const envName of Object.keys(envConf)) {
    let realName = envConf[envName].replaceAll(String.raw`\n`, '\n');
    if (realName === 'true') {
      realName = true;
    } else if (realName === 'false') {
      realName = false;
    }

    if (envName === 'VITE_PORT') {
      realName = Number(realName);
    }

    ret[envName] = realName;

    if (typeof realName === 'string') {
      // eslint-disable-next-line n/prefer-global/process
      process.env[envName] = realName;
    } else if (typeof realName === 'object') {
      // eslint-disable-next-line n/prefer-global/process
      process.env[envName] = JSON.stringify(realName);
    }
  }
  return ret;
}
