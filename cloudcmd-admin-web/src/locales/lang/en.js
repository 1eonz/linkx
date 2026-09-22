import { importAll } from '../helper'
import enLocale from 'element-ui/lib/locale/lang/en'

const en = importAll(require.context('./en', true, /\.js$/))

export default {
  message: {
    ...en,
    ...enLocale
  }
}
