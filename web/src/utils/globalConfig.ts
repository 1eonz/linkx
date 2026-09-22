// 全局配置模型
class GlobalConfig {
  _resolves: ((data: any) => void)[] = []
  
  // 异步属性：优先从 localStorage 读取，无则排队等待 load 完成
  get data(): Promise<any> {
    return new Promise((resolve) => {
      const cached = localStorage.getItem('globalConfig')
      if (cached) return resolve(JSON.parse(cached))
      this._resolves.push(resolve)
    })
  }

  // 设置数据：触发所有等待的 resolve，并写入 localStorage
  set data(data: any) {
    this._resolves.forEach((resolve) => resolve(data))
    this._resolves = []
    if (data) {
      localStorage.setItem('globalConfig', JSON.stringify(data))
    }
  }

  // 主动加载：请求接口并触发 setter（幂等，建议入口调用）
  async load() {
    const { getGlobalsList } = await import('@/api/dictionary')
    const res = await getGlobalsList()
    this.data = res?.code === 0 ? res.data : null
  }
}

export const globalConfig = new GlobalConfig()
