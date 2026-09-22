
import Vue from 'vue'
import { Dialog } from 'element-ui'

// 维护所有打开的弹窗实例
const dialogInstances = []

/**
 * 关闭所有打开的弹窗
 * @desc 用于退出登录时清理所有弹窗
 */
export function closeAllDialogs() {
  dialogInstances.forEach(instance => {
    try {
      instance.visible = false
      setTimeout(() => {
        instance.$el?.parentElement?.removeChild(instance.$el)
        instance.$destroy()
      }, 300)
    } catch (e) {
      console.error('closeAllDialogs error:', e)
    }
  })
  dialogInstances.length = 0
}

/**
 * createDialog
 * @desc - 传入组件comp,生成一个弹窗函数dialogFn
 * @param {VueComponent} comp - 需要在弹窗中显示的Vue组件
 * @param {Object} config - 可选，ElDidalog组件的配置参数
 * @returns {Function} dialogFn 返回一个函数，调用该函数会创建并显示弹窗
 *
 * dialogFn
 * @desc 弹窗函数dialogFn, 可异步调用
 * @param {Object} options - 可选，comp组件的配置参数
 * @param {Object} options.props - 组件属性
 * @param {Object} options.on - 自定义事件
 * @returns {Promise} 返回一个Promise
 *
 * Promise
 * @desc 通过Promise控制Dialog的关闭异步状态
 * @param {Function} resolve - 弹窗关闭并返回数据时调用
 * @param {Function} reject - cancel：点击取消按钮cancal触发时调用  close：点击关闭按钮或蒙层close触发时调用
 *
 * 子组件触发弹窗:
 * this.$emit('ok', data)
 * this.$emit('cancel')
 *
 * 父组件调用弹窗：
 * import AddFrom from '@/components/AddFrom.vue';
 * const diaLogFn = createDialog(AddFrom, { title: '用户新增' });
 * async handleAdd() {
 *   try {
 *     const formData = await dialogFn();
 *     formData && fthis.fetchData(formData)
 *   } catch(error) {
 *     if(error.type === 'cancel') {
 *     } else if(error.type === 'close') {
 *     }
 *   }
 * }
 */
export function createDialog(comp, config = { title: '弹窗标题' }) {
  return (options = {}) => {
    return new Promise((resolve, reject) => {
      // 挂载节点
      const container = document.createElement('div')
      document.body.appendChild(container)

      // 组件属性, 自定义事件
      const props = options.props || {}
      const customEvents = options.on || {}

      // 创建实例, 挂载到container
      const app = new Vue({
        data: {
          visible: false, // 初始设置为false
        },
        mounted() {
          // 组件挂载后显示弹窗
          this.showDialog()
        },
        methods: {
          destroy() {
            this.visible = false
            // 从实例列表中移除
            const index = dialogInstances.indexOf(app)
            if (index > -1) {
              dialogInstances.splice(index, 1)
            }
            setTimeout(() => {
              app.$el?.parentElement?.removeChild(app.$el)
              app.$destroy()
            }, 300)
          },
          showDialog() {
            // 延迟显示弹窗，确保动画正常触发
            this.$nextTick(() => {
              this.visible = true
            })
          }
        },
        render(h) {
          /** 创建Dialog子组件
           * - ok: 触发弹窗关闭并返回数据
           * - cancel: 触发弹窗关闭
           * - compInstance: comp扩展ok, cancel生成新的组件
          */
          const compInstance = h(comp, {
            props,
            on: {
              cancel: () => {
                reject({ type: 'cancel-dialog' })
                this.destroy()
              },
              ok: (data) => {
                resolve(data)
                this.destroy()
              },
              ...customEvents
            }
          })

          /** 创建Dialog组件
           * - close: 原生事件中关闭销毁弹窗
           * - config: 弹窗配置参数
          */
          return h(Dialog, {
            props: {
              visible: this.visible,
              ...config
            },
            on: {
              close: () => {
                reject({ type: 'close-dialog' })
                this.destroy()
              },
            }
          }, [compInstance])
        }
      }).$mount(container)
      
      // 将弹窗实例添加到列表中
      dialogInstances.push(app)
    })
  }
}
