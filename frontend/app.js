// app.js
const { clearImageCache } = require('./utils/image.js')

App({
  globalData: {
    userInfo: null,
    token: ''
  },

  onLaunch: function () {
    // 启动时读取本地缓存的登录信息
    const token = wx.getStorageSync('token')
    const userId = wx.getStorageSync('userId')
    if (token) {
      this.globalData.token = token
    }
    if (userId) {
      this.globalData.userId = userId
    }
  },

  onShow: function () {
    // 预留：重新进入小程序时可刷新 token 有效性等
  },

  /** 切后台 / 关闭小程序：统一释放图片 LRU 缓存，避免长会话内存增长 */
  onHide: function () {
    try { clearImageCache() } catch (e) { /* ignore */ }
  },

  /** 全局 JS 错误捕获（语法错误、运行时异常），避免白屏无提示 */
  onError: function (msg) {
    console.error('[小程序全局错误]:', msg)
    // 生产环境可在此上报监控（毕设用 console 即可）
  },

  /** 全局未处理的 Promise rejection 捕获（request.js / 图片下载等异步错误） */
  onUnhandledRejection: function (res) {
    console.error('[小程序未捕获 Promise rejection]:', res && res.reason ? res.reason : res)
  }
})
