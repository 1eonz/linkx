/**
 * 批量导入文件
 *
 * @export modules
 * @param {*} req
 * @return {*} modules
 */
export function importAll(req) {
  const modules = {}
  req.keys().forEach((key) => {
    const moduleName = key.replace(/(\.\/|\.js)/g, '')
    modules[moduleName] = {
      ...modules,
      ...req(key).default
    }
  })
  return modules
}
