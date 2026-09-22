import defaultSettings from '@/settings'

const { showSettings, fixedHeader, sidebarLogo } = defaultSettings

const state = {
  showSettings: showSettings,
  fixedHeader: fixedHeader,
  sidebarLogo: sidebarLogo,
  systemName: localStorage.getItem('SYSTEM_NAME') || '警务协同',
}

const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    if (state.hasOwnProperty(key)) {
      state[key] = value
    }
  },
  SET_SYSTEM_NAME: (state, name) => {
    state.systemName = name
    localStorage.setItem('SYSTEM_NAME', name)
  }
}

const actions = {
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  },
  setSystemName({ commit }, name) {
    commit('SET_SYSTEM_NAME', name)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

