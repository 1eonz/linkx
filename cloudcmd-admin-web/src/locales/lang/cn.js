import { importAll } from '../helper'
import zhLocale from 'element-ui/lib/locale/lang/zh-CN'

const cn = importAll(require.context('./cn', true, /\.js$/))

export default {
  message: {
    ...cn,
    ...zhLocale
  }
}
